# 🗂️ Feature Plan — Android Chess (4 hướng nâng cấp)

> Nguồn: 4 idea người dùng pick (memory leak fix · UI reskin · AI upgrade · hoàn thiện gameplay).
> **Phát hiện quan trọng khi audit code (2026-06-10):** 3/4 idea **đã được triển khai phần lớn** ở các commit trước.
> File này là single-source-of-truth, cập nhật sau mỗi wave.

---

## ✅ Đã hoàn thành (trước phiên này — xác nhận qua đọc source)

### Memory leak / ổn định (doc/memory_leak.md)
| Mã | Nội dung | Trạng thái thực tế |
|----|----------|--------------------|
| ML-04 | Cancel `aiHandler` trong `onDestroy` | ✅ `Chess.cancelAiHandler()` + gọi từ `ChessBoardActivity.onDestroy()` |
| BUG-01 | Lifecycle guard cho AI thread | ✅ `isDestroyed` flag, check trước & sau `aiHandler.post` |
| BUG-02 | Cancel mọi infinite animator + banner destroy | ✅ `onDestroy()` cancel blink/pulse/corner/glow + `bannerDestroy` |
| BUG-04 | Undo Pawn Promotion | ✅ Phát hiện promotion qua `instanceof Pawn`, khôi phục Pawn |
| WARN-01 | Knight check trùng `(-2,-1)` | ✅ Đã sửa thành `(+2,-1)` — 8 hướng knight đúng |
| WARN-02 | NPE `getParent()` trong `resetValidMoveButtons` | ✅ Có null-guard |
| ML-02, ML-03, BUG-03/05/06, WARN-05, OPT-01 | Liên quan `sdkadbmob/AdMobManager.kt` | ⚪ **Lỗi thời** — package đã bị xóa khi migrate sang `AdmobWrapper` SDK |

### UI Reskin (doc/reskin.md)
- ✅ Fonts `Cinzel` + `Exo2` đã thêm (`res/font/cinzel.xml`, `exo2.xml`).
- ✅ 20 game color tokens (`game_gold_primary`, `game_neon_cyan`, …) trong `colors.xml`.
- ✅ Board activity dark + gold/cyan glow, turn indicator, ambient animations, board themes (20 theme + picker dialog).
- ✅ `dialog_game_end.xml`, `dialog_difficulty.xml`, `dialog_glass_generic.xml`, pawn promotion đã reskin.

### AI Engine
- ✅ Minimax + Alpha-Beta, piece-square tables, opening book cơ bản, 4 độ khó (EASY random → UNBEATABLE depth 4).

---

## 🟢 Hoàn thành phiên này (compile-verified với SDK 36, đã revert build.gradle về 37)

### Wave 1 — Correctness & AI polish
- [x] **Point.hashCode()** — thêm `hashCode()` `(x<<3)|y` đúng contract với `equals()`. (`Point.java`)
- [x] **AI move-ordering (MVV-LVA)** — thêm `mvvLvaScore()` + `orderMoves()`, áp dụng ở root minimax/alpha-beta và node alpha-beta đệ quy ⇒ cắt tỉa tốt hơn, AI mạnh & nhanh hơn ở HARD/UNBEATABLE. Stable sort giữ shuffle cho nước cùng điểm. (`AIEngine.java`)
- [~] **Cache GameStatsManager** — bỏ: `Chess` đã cache sẵn `statsManager` ở constructor; `MainActivity` chỉ tạo khi mở dialog stats (hiếm) → không đáng tối ưu.

### Wave 2 — Hoàn thiện luật cờ
- [x] **Nhập thành (Castling)** — `King.addCastlingMovePoints()`: vua & xe chưa đi (`hasMoved`), ô giữa trống, vua không bị chiếu / không đi qua/đến ô bị tấn công. Thực thi (di chuyển cả xe) + undo trong `Chess.doMove`/`undoLastMove`. (`King.java`, `Rook.java`, `Chess.java`, `MoveRecord.java`)
- [x] **Bắt tốt qua đường (En passant)** — `Chess.enPassantTarget/Victim` cập nhật khi tốt đi 2 ô; `Pawn.generateMoves` sinh nước bắt chéo vào ô bị bỏ qua; thực thi (gỡ tốt ở ô bên) + undo khôi phục đúng ô.
- [x] **An toàn AI:** `Chess.inAiSimulation` bật trong `AIEngine.getBestMove` (try/finally) ⇒ tắt castling & en passant trong search, **không phải sửa** `simulateMove/undoSimulateMove`. AI vẫn đánh mọi nước thường; người chơi dùng đủ luật.

### Wave 3 — Vá 2 edge case luật cờ (sau audit)
- [x] **Castling — mô phỏng vua rời ô gốc** khi kiểm tra ô đi qua/đích: tạm `men[4][row]=null` (try/finally khôi phục) ⇒ xử lý đúng edge case "xe sau lưng vua", không còn offer nước nhập thành rồi bị từ chối. (`King.java`)
- [x] **En passant trong phát hiện chiếu hết** — `isMoveLegal` & `wouldMoveSaveKing` (qua helper `isEnPassantPseudoMove`) nay gỡ con tốt bị bắt qua đường khi mô phỏng ⇒ checkmate/stalemate đúng kể cả khi bắt qua đường là nước giải chiếu duy nhất. (`Chess.java`)

### Wave 4 — Test + Decoupling + Device verify
- [x] **Decouple engine khỏi UI**: thêm interface `ChessBoardView`; `Chess` không còn cast `(ChessBoardActivity) ctx` (12 chỗ → `boardView`); `ChessBoardActivity implements ChessBoardView`. Thêm chế độ model-only (guard `button == null` / `ctx == null`) để chạy headless.
- [x] **23 unit test JVM PASS** (`./gradlew testDebugUnitTest`, `testOptions.unitTests.returnDefaultValues=true`):
  - `PointTest` (5) — equals/hashCode contract.
  - `AIEngineOrderingTest` (3) — MVV-LVA.
  - `CastlingRulesTest` (8) — sinh nước nhập thành + edge case "xe sau lưng vua".
  - `EnPassantRulesTest` (4) — sinh nước + guard AI.
  - `CastlingExecutionTest` (2) — **thực thi + undo** nhập thành 2 bên (model).
  - `EnPassantExecutionTest` (1) — **thực thi + undo** bắt tốt qua đường (model).
- [x] **Device verify (Pixel 7 Pro, cheetah)**: build APK (SDK 36), install, launch → bàn cờ render đúng, engine init OK; tap e2→e4 chạy `doMove`/`validateKing`/animation 3 phase, đổi lượt sang Black, nút Undo hiện. **Không FATAL/crash.** Nhánh set `enPassantTarget` (đẩy tốt 2 ô) đã chạy thật.

### Wave 5 — Polish + Device playtest nhập thành (Pixel 7 Pro)
- [x] **Log gating**: `ChessLog` gate `BuildConfig.DEBUG`, thay 45 call site `Log.d/e` → release không log.
- [x] **Tách god-class**: `ChessAudio` (SoundPool) + `ChessHaptics` (rung) tách khỏi `Chess` (~1300→1244 dòng); logic move/undo giữ nguyên. 23 test vẫn pass.
- [x] **Playtest O-O trên thiết bị** (drive bằng adb input tap): dựng chuỗi e4/Bc4/Nf3 → nhập thành gần. Log + screenshot xác nhận **Vua e1→g1, Xe h1→f1** tự động, lượt sang Black. **Undo**: Vua→e1, Xe→h1 chuẩn. Không crash.

> ✅ Castling đã verify **end-to-end trên UI thật**. En passant: execution+undo đã có unit test + nhánh `enPassantTarget` đã chạy live (đẩy tốt 2 ô); chuỗi nước để *nhìn* en passant trên UI có thể drive tương tự nếu cần.

> 📌 **Lưu ý môi trường**: SDK platform `android-37` trong máy bị hỏng → build production cần sửa SDK hoặc hạ về 36. APK debug đã cài trên Pixel là bản build SDK 36 (chỉ để test). `build.gradle` trong repo giữ nguyên SDK 37.

---

## 📋 Picked — chờ triển khai (wave sau)

- AI side-agnostic: opening book hiện chỉ cho Black (app luôn để AI = Black nên ưu tiên thấp).
- AI quiescence search (giảm horizon effect) cho UNBEATABLE — cần playtest đo tốc độ.
- Reskin polish: `a_main.xml` còn nền ImageView `ic_bkg_1` (có thể thay bằng `bg_game_gradient`).

## ⏸️ Deferred (cần thiết bị / rủi ro cao)

- **ML-01** `Chess.ctx` → `WeakReference<Context>`: đụng ~20 call-site cast `(ChessBoardActivity) ctx`. Lợi ích biên thấp (đã có `cancelAiHandler` + `WeakReference<Chess>` ở Storage). Hoãn.
- Lưu/khôi phục ván dở qua các phiên (persistent game state).
- Đồng hồ cờ (chess clock), gợi ý nước đi (hint), draw 50 nước / lặp 3 lần.

## ❌ Skipped

- Reward Ad (không có touchpoint — theo doc/AD.MD).

---

## ⚠️ Ghi chú verify
- Mọi wave **build `./gradlew assembleDebug`** để chắc chắn compile.
- **Castling + En passant cần playtest trên thiết bị/emulator** (logic luật cờ + undo không thể verify chỉ bằng compile). Claude không tự chạy được emulator trong phiên này → đánh dấu cần người dùng test.
