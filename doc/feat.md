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

### Wave 6 — Animation di chuyển + ghost trail (`Chessman.java`)
- [x] **Animation move**: trượt rõ/chậm hơn (flight 200→340ms, ease-in-out, giảm xoay) + **ghost trail** (`spawnMoveTrail`, 3 afterimage mờ dần) — user xác nhận "đẹp". Leak-safe: `newDrawable()` + `AnimatorListener` gỡ ghost cả khi cancel.
- [x] **Memory audit**: LeakCanary active báo **0 leak**; đính chính `Drawable.Callback` là WeakReference (API14+) nên không leak qua static cache.

> Phạm vi: nằm ở `Chessman.java`, đã commit ở wave trước (tách riêng khỏi đợt cleanup Wave 7 bên dưới).

### Wave 7 — Cleanup + tách sâu god-class + leak-hardening promotion
- [x] **Cleanup**: xoá 10 import thừa + field dư `enPassantVictim`/`prevEnPassantVictim` (quân bị bắt qua đường suy ra trực tiếp từ `chessmen[to.x][from.y]`) + method dead `performCheckHaptic`. Guard `ctx != null` cho mọi Toast (headless-safe).
- [x] **Tách sâu god-class**: thêm `ChessAnimator` (selection pulse + check flash) → `Chess` **1244→1171 dòng**. Tổng 5 lớp helper tách ra: `ChessBoardView`, `ChessLog`, `ChessAudio`, `ChessHaptics`, `ChessAnimator`.
- [x] **`animatePromotion` leak-safe**: chuyển `withEndAction` → `AnimatorListener` (gỡ tốt cũ cả khi cancel) + guard `isDestroyed`/`boardLayout==null`. Cờ `cancelled` phân biệt cancel-thường (snap quân mới vào ngay, model đã phong cấp nên view bắt buộc hiện) với kết thúc tự nhiên (Phase 2 entrance).
- [x] **26 unit test pass** (thêm castle-into-check, double-push-sets-target, non-double-clears-target). Verify thiết bị: selection highlight + move + O-O + undo OK, không crash.

### Wave 8 — Phủ test 3 tầng (unit + widget/Robolectric + Espresso)
- [x] **Unit/integration JVM (+33 test)**: `MoveGenerationTest` (6 quân: mở/chặn/ăn/biên), `CheckmateStalemateTest` (chiếu/chiếu hết/hết cờ/an toàn qua `RecordingChessBoardView`), `MoveExecutionTest` (từ chối nước hở vua, chặn chiếu, bắt quân, firstMove), `UndoTest` (thường/bắt/đẩy-2-ô), `AIEnginePlayTest` (getBestMove ăn quân treo, getThinkDelay, trả null khi hết nước, không mutate bàn), `FullGameIntegrationTest` (ván K+R thu nhỏ → chiếu hết + undo).
- [x] **Widget test (Robolectric, JVM — không cần device, +11 test)**: `BoardThemeManager` & `GameStatsManager` (persistence SharedPreferences), `MoveRecord.getNotation`, **promotion + undo promotion** (cần `createButton`/`addView` thật). Ghim `@Config(sdk=34)` vì compileSdk 37 vượt mức Robolectric hỗ trợ.
- [x] **Espresso (instrumented, +4 test)**: `MainActivity` (menu hiển thị + điều hướng PvP với extra `IS_VS_AI=false`, chặn intent bằng `intending`), `ChessBoardActivity` (bàn cờ hiển thị, nút Undo/Play-Again ẩn đầu ván). **Phải nâng espresso 3.6.1→3.7.0** vì 3.6.1 lỗi `InputManager.getInstance` trên Android 16.
- [x] **Kết quả**: 70 test JVM+Robolectric **PASS** + 4 Espresso **PASS trên Pixel 7 Pro (Android 16)** = **74/74 xanh**. `build.gradle` thêm `testImplementation` robolectric + androidx.test, `androidTestImplementation` espresso (không vào APK).

> 📌 Phát hiện trong lúc test (không phải bug): nút "Play Again"/Undo là `GONE` lúc đang chơi (đúng thiết kế). `undoLastMove` trên history rỗng gọi `Toast` không guard `ctx` → chỉ NPE ở test seam (app thật luôn có ctx), không sửa.

### Wave 9 — Mở rộng test + luật hoà + dọn polish
- [x] **Test bổ sung (+11, không sửa engine)**: `PinDiscoveredCheckTest` (ghim quân không rời tuyến / vẫn đi dọc tuyến / mở chiếu phát hiện qua `isPointSafe`), AI **UNBEATABLE alpha-beta** ăn Hậu treo + không mutate bàn.
- [x] **Luật hoà mới (engine + undo + 7 test)**: `DrawRulesTest` — **50-move** (`halfMoveClock`, reset khi ăn quân/đẩy tốt) + **threefold** (`positionKey()` khoá thế: bố cục + bên đi + en passant + quyền nhập thành, đếm trong HashMap). Wire vào `checkOpponentKingStatus` → hoà (ưu tiên sau chiếu hết/hết cờ). Undo phục hồi clock + giảm đếm thế chính xác. `resetGame` xoá sạch.
- [x] **Polish**: thêm `ChessAppGlideModule` (@GlideModule) → **dứt warning** `GeneratedAppGlideModule` lúc khởi động (đã xác nhận `Wrote GeneratedAppGlideModule` + log warning biến mất trên device).
- [x] **Tách god-class tiếp**: trích cụm 7 method an toàn vua (`validateKing` + `isKingMoveSafe`/`hasAnyLegalMove`/`isMoveLegal`/`canAnyPieceSaveKing`/`wouldMoveSaveKing`/`isEnPassantPseudoMove`) sang **`KingSafetyEvaluator`** → `Chess.java` **1171→1057 dòng**. Tổng 7 lớp helper: `ChessBoardView`, `ChessLog`, `ChessAudio`, `ChessHaptics`, `ChessAnimator`, `KingSafetyEvaluator` (+ GlideModule).
- [x] **Kết quả**: **81 test JVM+Robolectric PASS** (+11) + **4 Espresso PASS trên Pixel 7 Pro (Android 16)** với APK build code mới = **85/85 xanh**. Verify thiết bị: move/undo/illegal-reject/no-leak (LeakCanary 0 retained).

> 📌 **AppContexts ~11 / Native heap ~112MB**: điều tra cho thấy do **AppLovin SDK + WebView** (ad), KHÔNG phải engine cờ — LeakCanary xác nhận 0 leak nên chấp nhận; giảm thêm = gỡ/đổi ad SDK, ngoài phạm vi. Java heap (engine) chỉ ~25MB, khỏe.

---

## 🟢 Hoàn thành phiên 2026-06-12 (VIP/ads/memory — chi tiết ở doc/AD.MD)

- [x] **VIP/ads**: upgrade SDK 1.1.5 + revamp UI màn VIP + consent UMP + **rewarded watch-ad** (→ mục "❌ Skipped Reward Ad" cũ nay ĐÃ có).
- [x] **Fix loạt bug ad**: App Open không show đè ad khác / không phá gameplay; banner lifecycle thủ công (chỉ refresh ở màn cờ); `rewardedInFlight` (watch-ad grant 3 ngày đúng).
- [x] **ML-01 `Chess.ctx` → `WeakReference`** (trước ở Deferred) — `getCtx()`/`setCtx()` + 6 piece class + AIEngine.
- [x] **Memory**: fix leak `SplashActivity` (static callback + WeakReference qua `splashTimeoutRunnable`); tối ưu GIF nền 3.4MB→1.76MB + Glide downsample RGB_565; APK release **13.2→11.6MB**.
- [x] **Test: 164 unit/widget PASS** + integration (Espresso/instrumented); `doc/SDK_LEAKS.md` ghi leak SDK-internal.

## 📋 Picked — 8 tính năng mới (user chọn 2026-06-12, triển khai theo wave)

**Wave A — tái dùng engine, value cao:**
1. ✅ **Gợi ý nước đi (Hint)** — `Chess.computeBestHint()` (AIEngine HARD depth 3) → highlight ô from/to (`bg_hint_square` gold), off-thread mirror AI; gate: VIP gợi ý ngay / non-VIP xem rewarded (không có ad vẫn gợi ý); auto-clear 4s + khi tương tác; nút gold "Gợi ý" + `ic_hint`; string 15 locale. **2 unit test PASS** (`HintTest` — bắt hậu treo + nước hợp lệ). **Verify device Pixel: Nb1→c3 highlight đúng, 78ms, 0 crash.**
2. ✅ **Lịch sử nước đi + xuất/share PGN** — `PgnExporter` (thuần, testable): SAN từng nước (pawn/piece/capture/O-O/O-O-O/promotion/en passant), movetext đánh số, **PGN 7-tag** + share intent. Dialog "Nước đi" (`showBasicDialog`) hiện list SAN + nút **Chia sẻ PGN** → share sheet; nút moves icon-only. **12 unit test PASS** (`PgnExporterTest`). **Verify device:** "1. e4" hiển thị + share sheet ra PGN chuẩn.
   - 🔧 **Fix bug UI (regression Hint):** hàng action button (theme/restart/undo/hint/moves) tràn ngang → nút Hint bị clip. Fix: bọc `HorizontalScrollView` (căn giữa khi vừa, cuộn khi tràn — không clip) + restart/undo/moves **icon-only** → 5 nút vừa khít màn Pixel 7 Pro. Verify device.

**Wave B — gameplay:**
3. ✅ **Đồng hồ cờ** (Blitz 5'/Rapid 10'/Không giờ) — `ChessClock` thuần (tick/switch/increment/flag/format, **8 unit test**); dialog time-control (Material, ép theme tối `ThemeOverlay.Chess.Dialog`) ở game start (PvP+PvE) → `TIME_CONTROL_MS` extra; 2 đồng hồ pill (đen trên/trắng dưới) đồng bộ lượt; tick 200ms `SystemClock`; flag-fall → `endGameByTimeout` + end dialog; pause/resume theo lifecycle, reset khi chơi lại. **Verify device:** Blitz → trắng đếm 5:00→4:4x, đen 5:00; dialog tối. *(v1: increment=0, clock reset khi xoay màn.)*
4. **Lưu & tiếp tục ván** — serialize board state + undo stack (SharedPreferences/file). *(tiếp theo)*

**🎨 Hệ DIALOG CHUNG (glass game style — nhất quán cả app):**
- ✅ `DialogUtils.showChoiceDialog` — dialog chọn 1-trong-N **glass** chung (icon halo/ring + title gold-glow + list card accent/emoji/subtitle/arrow + stagger anim) qua `dialog_glass_choice.xml`. Migrate **time-control** (trước `MaterialAlertDialog.setItems` — không nhất quán) → glass. Verify device: dialog xinh, khớp difficulty.
- ✅ Migrate **VIP success/failed/revoke** (trước `MaterialAlertDialogBuilder` ×3) → `showBasicDialog` (glass message). Sửa test revoke (click `btn_positive` custom).
- ⚪ `showDifficultyDialog` vẫn impl riêng (visual Y HỆT glass choice) — fold vào `showChoiceDialog` sau (pure refactor, 0 đổi visual).
- Build: **186 test PASS**; `assembleDebug` xanh.

**Wave C — content / retention:**
5. **Câu đố cờ** (Puzzles mate-in-N) — bộ FEN nhúng, engine validate.
6. **Phân tích sau ván** (blunder detection) — eval mỗi nước qua engine, gắn nhãn.
7. **Thành tích / huy hiệu** (Achievements) — SharedPreferences + UI list.
8. **Bộ quân cờ** (piece sets) — thêm asset (cân nhắc APK), có thể VIP unlock.

## ⏸️ Deferred

- AI side-agnostic opening book (AI luôn Black → ưu tiên thấp).
- AI quiescence search (giảm horizon effect) cho UNBEATABLE — cần playtest đo tốc độ.

## ❌ Skipped

- (trống — "Reward Ad" cũ nay đã triển khai qua VIP watch-ad)

---

## ⚠️ Ghi chú verify
- Mọi wave **build `./gradlew assembleDebug`** để chắc chắn compile.
- **Castling + En passant cần playtest trên thiết bị/emulator** (logic luật cờ + undo không thể verify chỉ bằng compile). Claude không tự chạy được emulator trong phiên này → đánh dấu cần người dùng test.
