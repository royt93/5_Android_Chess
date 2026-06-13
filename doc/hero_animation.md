# Hero Animation (Shared Element Transitions)

Bổ sung hero animation cho toàn game: một view ở màn nguồn "bay & biến hình" thành view
tương ứng ở màn đích, thay vì cả màn fade. Dùng `ActivityOptionsCompat.makeSceneTransitionAnimation`.

## ✅ Đã triển khai

### Nền tảng
- `res/values/themes.xml`: bật `windowActivityTransitions` + `windowContentTransitions` +
  `windowAllowEnter/ReturnTransitionOverlap` + `windowSharedElementsUseOverlay` cho
  **Theme.Chess**, **Theme.Chess.Vip**, **SplashTheme**.
- `NavAnim.java` (helper): `startWithHero(from, intent, hero, name)` — gán `transitionName`
  cho view nguồn lúc click, dùng scene transition (KHÔNG `overridePendingTransition` để tránh
  xung đột); `hero == null` → fallback `fade_zoom`. Cờ Intent `EXTRA_HERO` báo đích bỏ entry
  anim trùng của hero view. Hằng tên: `HERO_LOGO/BOARD/SAVED/ACHIEVEMENTS/PUZZLES/VIP`.

### Các cặp hero
| Từ → Đến | Hero element | transitionName |
|----------|--------------|----------------|
| Splash `ivLogo` → Main `ivMainLogo` | Logo | `hero_logo` |
| SavedGames `miniBoard` → ChessBoard `boardLayout` | Mini-board → bàn cờ | `hero_board` |
| Puzzles `puzzleRow` → ChessBoard `boardLayout` | Hàng câu đố → bàn cờ | `hero_board` |
| Main `btnPlayPvP/PvE` → ChessBoard `boardLayout` | Nút Play → bàn cờ | `hero_board` |
| Main `btnSavedGames` → SavedGames header | Nút → tiêu đề | `hero_saved` |
| Main `btnAchievements` → Achievements header | Nút → tiêu đề | `hero_achievements` |
| Main `btnPuzzles` → Puzzles header | Nút → tiêu đề | `hero_puzzles` |
| Main `btnVip` → Vip header | Nút → tiêu đề | `hero_vip` |
| ChessBoard → PawnPromotion | (giữ nguyên fade modal) | — |

### Xử lý xung đột
- **Bỏ entry anim trùng** của hero view khi vào bằng hero (quyết định: "thay hẳn bằng hero"):
  - Main: bỏ logo-pop thủ công (`heroLogo`).
  - ChessBoard: bỏ board fly-in thủ công (`NavAnim.enteredViaHero`).
- **Return hero**: btnBack của SavedGames/Puzzles/Achievements/Vip đổi `finish()` →
  `finishAfterTransition()` để animate ngược (header → nút menu). System back đã tự gọi
  `finishAfterTransition()` từ API 21.
- Splash: cancel logo breathing + reset scale/rotation trước khi navigate để shared element
  bắt đúng trạng thái tĩnh.

## ⚠️ Lưu ý / rủi ro còn lại
- Splash→Main dùng `FLAG_ACTIVITY_NEW_TASK`; nếu framework bỏ qua scene transition do NEW_TASK
  → tự degrade về fade (không crash). Cần xác nhận trên thiết bị.
- Board hero map từ view nhỏ (84dp / hàng text) sang bàn cờ chỉ `changeBounds` (không phải
  ImageView nên không `changeImageTransform`) — bàn cờ "lớn dần" từ vị trí nguồn.
- ChessBoard back (quit dialog + interstitial ad) giữ `finish()` cũ → không có return hero từ
  bàn cờ (tránh đụng luồng ad callback).

## Build
`./gradlew assembleDebug` — PASS (chỉ warning Java 8 cũ).
