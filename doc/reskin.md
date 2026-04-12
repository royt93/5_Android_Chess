# 🎮 UI Reskin Plan — Android Chess: Game Style

> **Mục tiêu:** Chuyển UI từ "minimal app" sang "game premium" — dark theme, metallic gold, ánh sáng neon, typography mạnh mẽ, animation sống động.
> **Lấy cảm hứng từ:** Chess.com mobile, Lichess dark mode, các strategy game AAA.

---

## 🎨 1. Design System — Theme Mới

### Palette màu

| Tên token | Hex | Dùng cho |
|-----------|-----|----------|
| `game_bg_deep` | `#0D0D0F` | Background tổng thể |
| `game_bg_surface` | `#1A1A2E` | Card, panel nền |
| `game_bg_elevated` | `#16213E` | Board surround, dialog |
| `game_gold_primary` | `#D4AF37` | Accent chính, border, icon highlight |
| `game_gold_light` | `#F5D76E` | Text heading, glow |
| `game_gold_dark` | `#8B7536` | Shadow, disabled state |
| `game_red_danger` | `#C0392B` | Check indicator, exit button |
| `game_green_action` | `#27AE60` | Play Again, confirm button |
| `game_blue_info` | `#2980B9` | Undo, stats |
| `game_white_piece` | `#F0EAD6` | Quân trắng, text sáng |
| `game_black_piece` | `#1C1C1C` | Quân đen |
| `game_board_light` | `#F0D9B5` | Ô sáng bàn cờ |
| `game_board_dark` | `#B58863` | Ô tối bàn cờ |
| `game_neon_cyan` | `#00D4FF` | Blink animation turn indicator |
| `game_text_muted` | `#8899AA` | Subtitle, placeholder text |

### Typography

```
Font chính:  "Cinzel" (Google Font — serif cổ điển, sang trọng)
Font phụ:    "Rajdhani" hoặc "Exo 2" (numbers, labels)
Font score:  "Bebas Neue" (đậm, đọc tốt)

Cách implement:
1. Thêm vào res/font/ (download từ Google Fonts)
2. Hoặc dùng Downloadable Fonts trong XML
```

### Gradient nền chính

```xml
<!-- res/drawable/bg_game_gradient.xml -->
<shape>
    <gradient
        android:startColor="#0D0D0F"
        android:centerColor="#1A1A2E"
        android:endColor="#0D0D0F"
        android:angle="135"/>
</shape>
```

---

## 📱 2. SplashActivity — `a_splash.xml`

### Hiện tại
- GIF background fullscreen
- ProgressBar trắng ở giữa
- Không có branding

### Reskin thành

```
┌─────────────────────────────┐
│                             │
│  [Animated particles/       │
│   chess pieces floating]    │
│                             │
│         ♛                  │
│    CHESS MASTER             │
│   ─────────────────         │
│    STRATEGY GAME            │
│                             │
│   [Loading bar - gold]      │
│   ████████░░░░░░ 65%        │
│                             │
│   "Powered by AI Engine"    │
└─────────────────────────────┘
```

**Thay đổi cụ thể:**
| Element | Hiện tại | Reskin |
|---------|----------|--------|
| Background | GIF `ic_bkg_1` | Dark gradient `#0D0D0F → #1A1A2E` |
| Loading | `ProgressBar` (spinner) | Horizontal progress bar màu vàng gold |
| Logo | Không có | TextView "CHESS" với font Cinzel, size 48sp, màu `#D4AF37` |
| Subtitle | Không có | "STRATEGY GAME" — letter-spacing 0.3em, màu `#8899AA` |
| Decoration | Không có | Chess piece SVG icons float/fade-in xung quanh |

**File cần sửa:** `a_splash.xml`, `SplashActivity.java`

---

## 🏠 3. MainActivity — `a_main.xml`

### Hiện tại
- GIF background
- Text Version/Copyright nhỏ
- 4 button dọc (PvP, PvE, Stats, Rules)
- 3 icon button dưới (Rate, More, Share)

### Reskin thành

```
┌─────────────────────────────┐
│  ✦ CHESS MASTER   v2026.04 │  ← Header bar mờ
├─────────────────────────────┤
│                             │
│   [Animated chess board     │
│    miniature / particle bg] │
│                             │
│    ♛ CHESS MASTER ♛         │  ← Logo vàng gold, glow effect
│    ─────────────────        │
│                             │
│  ┌─────────────────────┐   │
│  │  ⚔  VS PLAYER        │  │  ← Neon border button, gold text
│  └─────────────────────┘   │
│                             │
│  ┌─────────────────────┐   │
│  │  🤖  VS CPU          │  │  ← Secondary button, xanh viền
│  └─────────────────────┘   │
│                             │
│  ┌────────┐  ┌────────┐   │
│  │ 📊 STATS│  │📖 RULES│   │  ← 2 button grid dưới
│  └────────┘  └────────┘   │
│                             │
│  [⭐]    [🔗]    [📤]      │  ← Icon-only bottom row
└─────────────────────────────┘
```

**Thay đổi cụ thể:**

| Element | Hiện tại | Reskin |
|---------|----------|--------|
| Background | GIF | Dark gradient + subtle particle overlay |
| Logo / Title | Không có | "CHESS MASTER" Cinzel 36sp, gold, glow shadow |
| btnPlayPvP | Rectangular `bg_button_start`, 72dp cao | Tall 80dp, metallic gold gradient border, dark fill, text vàng |
| btnPlayPvE | Tương tự PvP | Border xanh cyan `#00D4FF`, dark fill |
| btnStats + btnRules | Riêng lẻ full-width | 2 cột bên nhau, compact 56dp, muted gold border |
| Bottom 3 icons | 3 button riêng | Row icon với label nhỏ phía dưới, rounded square background |
| Version text | 12sp mờ | Hiển thị trong header bar góc phải |

**Drawable mới cần tạo:**
```
bg_btn_pvp.xml       — Gold gradient border, dark fill, rounded 12dp
bg_btn_pve.xml       — Cyan border, dark fill, rounded 12dp
bg_btn_secondary.xml — Muted border, glass effect
bg_game_header.xml   — Horizontal bar gradient dark
```

---

## ♟️ 4. ChessBoardActivity — `a_chess_board.xml`

### Hiện tại
- Background màu đổi theo lượt (đen/trắng thuần)
- Turn indicator: 16dp circle đỏ
- Captured pieces: dải ngang trên/dưới
- Nút Undo/Restart: MaterialButton màu cam/xanh

### Reskin thành

```
┌─────────────────────────────┐
│ ♙♙♙♙♙♙   [captured black]  │  ← Dải tối, border gold, icon rõ
│         ▲ BLACK TURN        │  ← Triangle indicator, neon cyan
├─────────────────────────────┤
│                             │
│  ┌─────────────────────┐   │
│  │  🔲🔳🔲🔳🔲🔳🔲🔳   │   │
│  │  🔳🔲🔳🔲🔳🔲🔳🔲   │   │  ← Board với texture gỗ/marble
│  │      [PIECES]        │   │      highlight: gold ring quanh
│  │                      │   │      quân được chọn
│  │  [valid move dots]   │   │      valid moves: cyan dot
│  └─────────────────────┘   │
│                             │
│         ▼ WHITE TURN        │  ← Triangle indicator, neon gold
│ ♖♕♙♙    [captured white]   │
├─────────────────────────────┤
│  [↩ UNDO]      [🔄 RESET]  │  ← Dark pill buttons, icon trái
├─────────────────────────────┤
│  [  AD BANNER  ]            │
└─────────────────────────────┘
```

**Thay đổi cụ thể:**

| Element | Hiện tại | Reskin |
|---------|----------|--------|
| Background | Solid black/white swap | Gradient dark xoay chiều, có texture nhẹ |
| Board background | `#D13300` (nâu đỏ) | Texture gỗ PNG hoặc `#8B4513` với vignette |
| Ô sáng | Chỉ trong drawable | `#F0D9B5` classic |
| Ô tối | Chỉ trong drawable | `#B58863` classic |
| Turn indicator | 16dp red circle, blink | Badge hình tam giác với player name, glow cyan/gold |
| btnUndo | Orange MaterialButton | Dark pill, icon trắng, border mờ |
| btnRestart | Green MaterialButton | Dark pill, icon trắng, border mờ |
| Captured pieces bar | `bg_captured_white/black` | Frosted glass panel, border 1dp gold mờ |
| Valid move dots | `ic_point` resource | Cyan semi-transparent circle + outer ring |
| Selected piece highlight | Scale pulse | Gold ring glow xung quanh piece button |

**Java cần sửa (`Chess.java`):**
- `createValidMoveButton()`: thay drawable `ic_point` bằng custom `bg_valid_move_dot`
- `startSelectionPulse()`: thêm glow color bên cạnh scale pulse

---

## 🏆 5. Dialog Game End — `dialog_game_end.xml`

### Hiện tại
- White background card, bo tròn
- Trophy icon
- Stats container nền `#F5F5F5`
- Nút xanh/grey thông thường

### Reskin thành

```
┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
  ┌───────────────────────┐
  │  ✨ [CONFETTI BURST]  │
  │                       │
  │      🏆               │  ← Trophy icon, gold glow, scale animation
  │  CHECKMATE!           │  ← Cinzel 28sp, gold text
  │  White Wins 👑         │  ← Subtitle
  │  ─────────────────    │
  │  ⏱ 12:34   🎯 47 moves│  ← Stats row icon, compact
  │  ♟ 8 cap.  ♙ 6 cap.  │
  │  ───────────────────  │
  │  [📋 REVIEW] [▶ AGAIN]│  ← Gold outlined / Gold filled
  │       [✖ EXIT]        │
  └───────────────────────┘
└ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
```

**Thay đổi cụ thể:**

| Element | Hiện tại | Reskin |
|---------|----------|--------|
| Dialog background | `dialog_background.xml` | Dark card `#1A1A2E`, border 1.5dp gold, corner 20dp |
| Title text | `#333333` | Gold `#D4AF37`, Cinzel |
| Message text | `#757575` | `#8899AA` |
| Stats container | `#F5F5F5` | `#0D0D0F` với border 1dp `#2C2C3E` |
| Stats text | `#616161` / `#333333` | Icons emoji thay label, values `#F0EAD6` |
| btn_review | Blue outline | Gold outline, dark fill |
| btn_play_again | Green fill | Solid gold `#D4AF37`, text đen |
| btn_exit | Grey text | Muted red text `#C0392B` |
| Trophy icon | Static | Scale + glow animation khi dialog show |

---

## 🎯 6. Dialog Chọn Độ Khó — `DialogUtils.java`

### Hiện tại
- `AlertDialog` với buttons tạo dynamic trong Java
- Màu cứng: green/orange/red/purple hardcode

### Reskin thành

```
╔═══════════════════════════╗
║     ⚔ SELECT OPPONENT     ║
╠═══════════════════════════╣
║  🟢  EASY     "Novice"    ║
║  ──────────────────────   ║
║  🟡  MEDIUM   "Knight"    ║
║  ──────────────────────   ║
║  🔴  HARD     "Grand..."  ║
║  ──────────────────────   ║
║  🟣  UNBEATABLE "Magnus"  ║
╠═══════════════════════════╣
║          [CANCEL]          ║
╚═══════════════════════════╝
```

**Thay đổi:**
- Tạo `dialog_difficulty.xml` riêng (hiện đang inflate dynamic)
- Mỗi difficulty item = một row với icon màu + title + subtitle mô tả
- Row highlight màu theo difficulty
- Font Cinzel/Exo 2 cho title

---

## 🎖️ 7. Pawn Promotion — `a_pawn_promotion.xml`

### Hiện tại
- Cards trắng tên pieces + icon
- Background mờ đen

### Reskin thành

```
┌─────────────────────────────┐
│    PROMOTE YOUR PAWN ♟      │
│   ─────────────────────     │
│                             │
│  ┌──────┐┌──────┐┌──────┐┌──────┐
│  │  ♛   ││  ♖   ││  ♗   ││  ♘   │
│  │QUEEN ││ ROOK ││BISHOP││KNIGHT│  ← Gold border hover, dark bg
│  └──────┘└──────┘└──────┘└──────┘
│                             │
└─────────────────────────────┘
```

**Thay đổi:**
- Card background: `#1A1A2E` thay `#FFFFFF`
- Card border: 2dp gold khi selected/hover
- Text màu: `#D4AF37` thay `#000000`
- Piece icon: thêm glow filter
- Background overlay: gradient tối hơn, thêm blur hint

---

## 🔧 8. Danh sách File Cần Thay Đổi

### `res/values/`
```
colors.xml         — Thêm toàn bộ game palette (~15 màu mới)
themes.xml         — Cập nhật colorPrimary → gold, dark background
styles.xml         — Thêm GameButton style, GameText style
dimens.xml         — Thêm button_height_game=80dp, corner_game=12dp
```

### `res/drawable/` (tạo mới)
```
bg_game_gradient.xml       — Dark gradient nền
bg_btn_pvp.xml             — Gold border button
bg_btn_pve.xml             — Cyan border button
bg_btn_secondary.xml       — Muted glass button
bg_btn_danger.xml          — Red border button
bg_turn_indicator_black.xml— Cyan badge indicator
bg_turn_indicator_white.xml— Gold badge indicator
bg_dialog_game.xml         — Dark card dialog
bg_captured_game.xml       — Frosted glass bar
bg_valid_move_game.xml     — Cyan dot + ring
bg_piece_selected.xml      — Gold ring glow
bg_promotion_card.xml      — Dark card promotion
```

### `res/layout/`
```
a_splash.xml         — Resize logo, horizontal progress bar
a_main.xml           — Toàn bộ restructure menu
a_chess_board.xml    — Turn indicator, captured bar, buttons
dialog_game_end.xml  — Dark card, gold text, compact stats
a_pawn_promotion.xml — Dark cards, gold border
dialog_glass_generic.xml — Dark glass effect
```

### `res/font/` (thêm mới)
```
cinzel_regular.ttf
cinzel_bold.ttf
exo2_regular.ttf
exo2_semibold.ttf
```
> Tải từ: https://fonts.google.com/specimen/Cinzel

### Java/Kotlin
```
Chess.java           — createValidMoveButton() → custom drawable
ChessBoardActivity.java — updateSystemBarsTint() với màu game mới
```

---

## 📐 9. Animation Specs

| Interaction | Hiện tại | Reskin |
|------------|----------|--------|
| Button press | Scale 1.1 → 1.0 | Scale + gold ripple wave |
| Turn change | Background color animat | Slide indicator + text fade + screen edge glow |
| Piece select | Scale pulse INFINITE | Gold ring expand và giữ |
| Piece move | Bounce interpolator | Ease-out với trail shadow |
| Dialog appear | `dialog_enter_anim` fade | Slide up + fade, scale 0.8→1.0 |
| Check animation | Alpha blink | Red glow pulsate trên King |
| Game end | Static dialog | Confetti burst + trophy scale in |
| MenuItem entry | Slide from bottom | Overshoot scale stagger (giữ nguyên) |

---

## 🗺️ 10. Thứ Tự Thực Hiện Đề Xuất

```
Phase 1 — Foundation (colors, themes, fonts)
├── colors.xml: thêm game palette
├── themes.xml: cập nhật app theme dark
├── dimens.xml: thêm tokens
└── res/font/: thêm Cinzel + Exo 2

Phase 2 — Drawables (backgrounds, borders)
├── bg_game_gradient, bg_btn_pvp/pve/secondary
├── bg_dialog_game, bg_captured_game
├── bg_valid_move_game, bg_piece_selected
└── bg_turn_indicator_black/white

Phase 3 — Screens (layout by layout)
├── a_splash.xml
├── a_main.xml
├── a_chess_board.xml
├── dialog_game_end.xml
├── a_pawn_promotion.xml
└── dialog_glass_generic.xml

Phase 4 — Logic adjustments
├── Chess.java: valid move button drawable
└── ChessBoardActivity.java: system bar màu

Phase 5 — Polish & Animations
├── Animation definitions
└── Final visual pass
```

---

## 💡 11. References Trực quan

| Concept | Tham khảo |
|---------|-----------|
| Board dark theme | Chess.com → Settings → Board Themes → "Brown" on dark |
| Main menu style | "Chess Rush" by Tencent, "Chessplosion" |
| Dialog style | "Shadow Fight 3" result screen |
| Button style | "Clash Royale" lobby buttons |
| Typography | "Cinzel" như game medieval/fantasy |
| Color palette | Dark navy + Antique gold = classic chess aesthetic |
