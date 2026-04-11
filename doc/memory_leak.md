# Memory Leak & Bug Analysis — Android Chess

> **Phân tích:** 2026-04-11 | **Fix hoàn tất:** 2026-04-12 | **Build:** ✅ SUCCESSFUL
> **Phạm vi phân tích:** Toàn bộ source code `app/src/main/java/`

---

## Tóm tắt

| Mức độ | Số lượng |
|--------|----------|
| 🔴 Nghiêm trọng (Memory Leak thực sự) | 5 |
| 🟠 Cao (Bug tiềm ẩn / crash risk) | 6 |
| 🟡 Trung bình (Code smell / logic bug) | 6 |
| 🟢 Thấp (Cải thiện) | 4 |

---

## 🔴 NGHIÊM TRỌNG — Memory Leak Thực Sự

### [ML-01] `Chess.java` — `ctx` giữ strong reference tới Activity

**File:** `chess/Chess.java`  
**Dòng:** 37, 150

```java
public Context ctx;  // L37 — public field, không weak reference
```

**Vấn đề:**  
`Chess` object được lưu trong `Storage.chess` (static field). Khi `ChessBoardActivity` bị destroy (rotate màn hình, back press), `ctx` vẫn giữ reference tới Activity đã chết → Memory Leak.

`changeLayout()` tại L139–146 tái sử dụng `Storage.chess` và gán `ctx` mới, nhưng nếu `changeLayout()` không được gọi kịp thời hoặc Activity bị destroy trước đó, leak xảy ra.

**Hậu quả:** Mỗi lần rotate màn hình → 1 Activity bị retain. GUI hierarchy, Drawables, Windows đều không được thu hồi.

**Giải pháp:**
```java
// Thay public Context ctx bằng WeakReference
private WeakReference<Context> ctxRef;

public Context getCtx() {
    return ctxRef != null ? ctxRef.get() : null;
}

public void setCtx(Context ctx) {
    this.ctxRef = new WeakReference<>(ctx);
}
```

---

### [ML-02] `Storage.java` — `Storage.chess` là static strong reference

**File:** `chess/Storage.java`  
**Dòng:** 27

```java
public static Chess chess = null;  // L27 — static, giữ toàn bộ game state + ctx
```

**Vấn đề:**  
`Storage.chess` là `static` và thông qua `Chess.ctx`, nó gián tiếp giữ strong reference tới `ChessBoardActivity`. Kể cả khi đã có `WeakReference<Chess> chessRef` ở L12, code vẫn song song dùng `Storage.chess` backup dạng strong reference (ghi chú "Backward compatibility").

Hai cơ chế lưu cùng tồn tại là nguy hiểm:  
- `Storage.chess = null` tại `handleBackPress()` — ổn khi back.
- Nhưng nếu app crash hoặc process kill rồi restore → `Storage.chess` có thể vẫn trỏ đến Activity cũ.

**Giải pháp:**  
Xóa `public static Chess chess = null;` và chỉ dùng `WeakReference<Chess> chessRef`.

---

### [ML-03] `AdMobManager.kt` — `CoroutineScope` không bị cancel

**File:** `sdkadbmob/AdMobManager.kt`  
**Dòng:** 165, 599, 603

```kotlin
CoroutineScope(Dispatchers.Default).launch { ... }  // L165
CoroutineScope(Dispatchers.Default).launch { ... }  // L599 — initSplashScreen
CoroutineScope(Dispatchers.Main).launch { ... }     // L603
```

**Vấn đề:**  
`CoroutineScope` được tạo mới mỗi lần mà không lưu lại để cancel. Đặc biệt nghiêm trọng tại `initSplashScreen()` (L599): scope này `collectLatest` trên `EventBus.eventFlow` và **không bao giờ bị hủy**. `EventBus` có `replay = 1` nên flow sẽ luôn emit lại value — coroutine này sống mãi cho đến khi process chết.

Coroutine tại L603 gọi `loadAppOpenAd()` với `activity` context — nếu Activity đã destroyed mà coroutine vẫn chạy → crash hoặc leak.

**Giải pháp:**
```kotlin
// Dùng GlobalScope.launch với SupervisorJob hoặc lưu Job để cancel
private var splashJob: Job? = null

fun initSplashScreen(activity: Activity, onAdLoaded: () -> Unit) {
    splashJob?.cancel()
    splashJob = CoroutineScope(Dispatchers.Default).launch {
        EventBus.eventFlow.take(1).collect { ... }  // take(1) thay collectLatest
    }
}
```

---

### [ML-04] `Chess.java` — `aiHandler` (main looper Handler) không được removed

**File:** `chess/Chess.java`  
**Dòng:** 53

```java
private android.os.Handler aiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
```

**Vấn đề:**  
`aiHandler` trong `Chess` (được giữ bởi static `Storage.chess`) có thể chứa pending `Runnable` khi Activity bị destroy. Nếu `triggerAITurn()` (L608) đang chạy và Activity bị destroy trước khi `aiHandler.post()` thực thi, Runnable trong queue sẽ giữ reference tới `Chess` → giữ `ctx` → Memory Leak.

**Hậu quả:** Crash tiềm ẩn khi `aiHandler.post()` gọi `doMove()` → `((ChessBoardActivity) ctx).animateTurnChange()` trên Activity đã destroyed.

**Giải pháp:**
```java
@Override
public void onDestroy() {
    if (chess != null) {
        chess.cancelAiHandler();
    }
}

// Trong Chess.java
public void cancelAiHandler() {
    aiHandler.removeCallbacksAndMessages(null);
    isAiThinking = false;
}
```

---

### [ML-05] `Chessman.java` — `moves` ArrayList không được clear trước khi tích lũy

**File:** `chess/Chessman.java`  
**Dòng:** 47, 360–411

```java
public ArrayList<Point> moves = new ArrayList<>();
```

**Vấn đề:**  
Các phương thức `addVerticalMovePoints()`, `addHorizontalMovePoints()`, `addObliqueNWtoSEMovePoints()`, v.v. **không** clear `moves` trước khi thêm. Chúng chỉ kiểm tra `!moves.contains(p)` để tránh trùng lặp — nhưng `contains()` là O(n) trên ArrayList và moves cũ từ turn trước có thể vẫn tồn tại nếu ai đó gọi riêng lẻ các method này mà không gọi `generateMoves()`. Mỗi `Point` check là O(n) trên list có thể chứa nhiều entries rác.

Đây không phải memory leak nghiêm trọng nhưng tích lũy garbage nếu `generateMoves()` bị quên gọi, và làm chậm AI minimax vì mỗi `generateAllLegalMoves()` loop qua 64 ô và gọi `generateMoves()`.

---

## 🟠 CAO — Bug Tiềm Ẩn / Crash Risk

### [BUG-01] `Chess.java` — AI Thread chạy sau khi Activity destroyed

**File:** `chess/Chess.java`  
**Dòng:** 612–651 (`triggerAITurn`)

```java
new Thread(() -> {
    ...
    aiHandler.post(() -> {
        ...
        doMove(from, to);  // Gọi ((ChessBoardActivity) ctx).animateTurnChange()
        isAiThinking = false;
    });
}).start();
```

**Vấn đề:**  
Thread AI không biết Activity đã bị destroy. `gameEnd` flag chỉ ngăn gọi `doMove()`, nhưng không ngăn nếu AI thread đang sleep khi user back, rồi `gameEnd = false` (reset), rồi thread wakes up → gọi `doMove()` trên Activity mới (hoặc null).

Không có check `isFinishing()` hay lifecycle-aware component.

**Crash scenario:**  
1. User nhấn Back → `Storage.chess = null`, Activity `finish()`.
2. AI thread đang sleep (delay 500–1500ms).
3. Thread wake up → `gameEnd = false` (vì game đã reset) → `doMove()` → `ctx.animateTurnChange()` → `ClassCastException` hoặc NPE.

---

### [BUG-02] `ChessBoardActivity.java` — `onDestroy()` không cancel `currentBlinkAnimator` và `adView`

**File:** `ChessBoardActivity.java`  
**Dòng:** 634–639

```java
@Override
protected void onDestroy() {
    if (adView != null) {
        adView.destroy();
    }
    super.onDestroy();
    // THIẾU: currentBlinkAnimator.cancel(), chess cleanup
}
```

**Vấn đề:**  
- `currentBlinkAnimator` (ObjectAnimator chạy INFINITE trên `blackTurnIndicator`/`whiteTurnIndicator`) không bị cancel trong `onDestroy()`. Animator giữ reference tới View → View giữ Context → Activity Leak.
- `chess.cancelAiHandler()` không được gọi → AI thread tiếp tục chạy.
- `AdMobManager.INSTANCE.setInterstitialListener(null)` không được gọi → `interstitialListener` vẫn trỏ tới Activity.

---

### [BUG-03] `AdMobManager.kt` — `currentActivity` WeakReference có thể null mà không có null check

**File:** `sdkadbmob/AdMobManager.kt`  
**Dòng:** 119, 376–428

```kotlin
private var currentActivity: WeakReference<Activity>? = null
```

**Vấn đề:**  
`currentActivity` được khai báo nhưng **không bao giờ được sử dụng** trong `showInterstitial()` hay `showAppOpenAd()` — các method này nhận `activity` parameter riêng. Tuy nhiên nếu sau này ai thêm code dùng `currentActivity?.get()` mà không check null → NPE khi WeakReference đã cleared.

Quan trọng hơn: `setCurrentActivity(this)` được gọi trong `ChessBoardActivity.onCreate()` nhưng activity cũ không clear → nếu `currentActivity` được dùng sau này sẽ leak Activity.

---

### [BUG-04] `Chess.java` — `undoLastMove()` không xử lý trường hợp Promoted Pawn

**File:** `chess/Chess.java`  
**Dòng:** 951–1020

```java
// Restore first move status for pawns
if (lastMove.wasFirstMove && movedPiece.type == Chessman.ChessmanType.Pawn) {
    ((Pawn) movedPiece).firstMove = true;
}
```

**Vấn đề:**  
Khi Pawn được promote thành Queen, `promotionResult()` tạo ra một object Queen mới và thay thế Pawn trong `chessmen[][]`. Nhưng `moveHistory` vẫn lưu reference đến Pawn object cũ (`movedPiece`). Khi undo:

1. `movedPiece = chessmen[lastMove.toX][lastMove.toY]` → đây là **Queen** mới, không phải Pawn.
2. Undo move Queen về vị trí cũ — **không** khôi phục Pawn.
3. Cast sang `(Pawn)` sẽ `ClassCastException` hoặc điều kiện `type == Pawn` false → không restore `firstMove`.

Kết quả: Sau undo pawn promotion, cờ vua không khôi phục đúng trạng thái.

---

### [BUG-05] `MyApplication.kt` — `MobileAds.initialize()` gọi trên background thread

**File:** `MyApplication.kt`  
**Dòng:** 19–24

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    MobileAds.initialize(this@MyApplication) {}  // Sai thread!
    AdMobManager.init(this@MyApplication) { ... }
}
```

**Vấn đề:**  
`MobileAds.initialize()` theo tài liệu Google phải được gọi trên **Main Thread**. Gọi trên `Dispatchers.IO` (background thread) có thể gây:
- Race condition với các lần load ad đầu tiên.
- `MobileAds` chưa init xong khi `SplashActivity.initSplashScreen()` được gọi.
- Undefined behavior trên một số thiết bị.

**Giải pháp:**
```kotlin
// Chạy trên Main thread
CoroutineScope(Dispatchers.Main).launch {
    MobileAds.initialize(this@MyApplication) {}
    // Sau đó chuyển sang IO nếu cần
    withContext(Dispatchers.IO) {
        AdMobManager.init(this@MyApplication) { ... }
    }
}
```

---

### [BUG-06] `AdMobManager.kt` — `initSplashScreen()` đếm `countInitSplashScreen` trên singleton nhưng không reset

**File:** `sdkadbmob/AdMobManager.kt`  
**Dòng:** 591–621

```kotlin
var countInitSplashScreen = 0  // public var trên object singleton

fun initSplashScreen(activity: Activity, onAdLoaded: () -> Unit) {
    countInitSplashScreen++
    if (countInitSplashScreen > 1) {
        onAdLoaded.invoke()
    } else { ... }
}
```

**Vấn đề:**  
`countInitSplashScreen` không bao giờ được reset. Nếu user kill app và mở lại trong cùng process (hiếm nhưng có thể xảy ra trên một số launcher), `countInitSplashScreen` đã là 1 → App Open Ad sẽ **không bao giờ hiển thị** lần nào nữa cho đến khi process thực sự chết.

---

## 🟡 TRUNG BÌNH — Code Smell / Logic Bug

### [WARN-01] `Chessman.java` — `isPointSafe()` kiểm tra Knight trùng lặp

**File:** `chess/Chessman.java`  
**Dòng:** 284–289

```java
x = point.x - 2;
y = point.y - 1;
if (Point.isValid(x, y) && parent.chessmen[x][y] != null
        && parent.chessmen[x][y].color != color
        && parent.chessmen[x][y].type == ChessmanType.Knight)
    return false;
```

**Vấn đề:**  
Knight move `(-2, -1)` được kiểm tra **hai lần** tại dòng 260–265 VÀ 284–289. Đây là duplicate code gây nhầm lẫn và có thể che giấu bug nếu một trong hai được sửa mà cái còn lại thì không. Tổng có 8 vị trí knight nhưng code check 9 lần (1 trùng).

---

### [WARN-02] `Chess.java` — `resetValidMoveButtons()` có thể NPE nếu View đã detached

**File:** `chess/Chess.java`  
**Dòng:** 817–825

```java
public void resetValidMoveButtons() {
    clearSelectionHighlight();
    for (View v : validMoveButtons)
        ((ViewGroup) v.getParent()).removeView(v);  // Crash nếu getParent() == null
    validMoveButtons.clear();
}
```

**Vấn đề:**  
Nếu `boardLayout` đã bị detach hoặc `v` đã bị remove bởi code khác (ví dụ: rotate màn hình → `changeLayout()` clear tất cả views), `v.getParent()` trả về `null` → NPE.

**Giải pháp:**
```java
for (View v : validMoveButtons) {
    if (v.getParent() != null) {
        ((ViewGroup) v.getParent()).removeView(v);
    }
}
```

---

### [WARN-03] `Chess.java` — `addForwardMovePoints()` không kiểm tra ô trước có trống không

**File:** `chess/Chessman.java`  
**Dòng:** 617–621

```java
private void addForwardMovePoints(int step) {
    Point p = new Point(point.x, point.y + step);
    if (p.isValid() && !moves.contains(p))
        moves.add(p);  // Không kiểm tra chessmen[p.x][p.y] == null!
}
```

**Vấn đề:**  
`addForwardMovePoints()` dùng bởi `add1StepForwardMovePoints()` và `add2StepForwardMovePoints()` không check xem ô có bị chặn không. `Pawn.generateMoves()` handle đúng (L38–48 trong `Pawn.java`), nhưng nếu `addForwardMovePoints()` được gọi trực tiếp từ code khác thì sẽ cho phép di chuyển xuyên qua quân khác.

---

### [WARN-04] `AIEngine.java` — `undoSimulateMove()` không xử lý En Passant

**File:** `chess/AIEngine.java`  
**Dòng:** 538–592

**Vấn đề:**  
Simulation trong AI không handle En Passant capture. Khi thực thi undo sau khi AI capture bằng En Passant, quân bị bắt không được restore về đúng ô vuông (vì En Passant bắt không phải ở `[toX][toY]`). Điều này khiến AI evaluation sai ở depth > 1 cho HARD/UNBEATABLE mode.

---

### [WARN-05] `AdMobManager.kt` — `getGAID()` chạy raw `Thread` thay vì Coroutine

**File:** `sdkadbmob/AdMobManager.kt`  
**Dòng:** 197–208

```kotlin
fun getGAID(context: Context, callback: (String) -> Unit) {
    Thread {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            ...
            callback(id)  // callback gọi trên background thread!
        } catch (e: Exception) {
            callback("")
        }
    }.start()
}
```

**Vấn đề:**  
`callback(id)` được gọi trên background thread. Trong `init()`, callback này update `this.application`, `this.currentDeviceGAID`, gọi `EventBus.sendEvent()`. Nếu các caller khác đọc `currentDeviceGAID` đồng thời từ main thread → Data race.

---

### [WARN-06] `SplashActivity.java` — Handler leak khi Activity finish trước delay

**File:** `SplashActivity.java`  
**Dòng:** 57–59

```java
new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
    runOnUiThread(() -> navigateToMainActivity());
}, remainingTime);
```

**Vấn đề:**  
Handler được tạo mới (anonymous) và không được giữ reference để cancel. Nếu Activity bị destroyed trong `remainingTime` window (ví dụ: user quay back ngay), Runnable vẫn fire và gọi `runOnUiThread()` → `navigateToMainActivity()` trên Activity đã finished → NavigationException hoặc blank screen.

---

## 🟢 THẤP — Cải Thiện Đề Xuất

### [OPT-01] `AdPreloadDemo.kt` — `Thread.sleep()` trong raw Thread

**File:** `sdkadbmob/AdMobManager.kt`  
**Dòng:** 1021–1026

```kotlin
Thread {
    Thread.sleep(2000)
    preloadedAds = kotlin.random.Random.nextInt(1, maxAds + 1)
    isPreloading = false
    ...
}.start()
```

`preloadedAds` và `isPreloading` là non-volatile, non-synchronized → visibility issue trên đa luồng.

---

### [OPT-02] `Chess.java` — `GameStatsManager` được tạo lại nhiều lần không cần thiết

**File:** `ChessBoardActivity.java`  
**Dòng:** 566, 193

```java
// Mỗi lần showCustomGameEndDialog():
com.saigonphantomlabs.chess.GameStatsManager sm = new com.saigonphantomlabs.chess.GameStatsManager(this);

// Trong MainActivity.showStatsDialog():
com.saigonphantomlabs.chess.GameStatsManager statsManager = new com.saigonphantomlabs.chess.GameStatsManager(this);
```

`GameStatsManager` chỉ đọc SharedPreferences — không cần tạo mới mỗi lần. Nên cache lại, đặc biệt để tránh `Context` được giữ lâu hơn cần thiết.

---

### [OPT-03] `Chessman.java` — `moves` dùng `ArrayList.contains()` là O(n)

**File:** `chess/Chessman.java`  
**Dòng:** 374, 380, 400, 407...

```java
if (!moves.contains(p)) moves.add(p);
```

`ArrayList.contains()` dựa vào `equals()` — O(n). Với AI minimax depth 4, mỗi node gọi `generateMoves()` cho tất cả pieces → nested O(n²) operations trong vòng lặp. Dùng `HashSet<Point>` cho lookup và chuyển sang List khi cần trả về sẽ cải thiện tốc độ AI đáng kể.

Cần implement `Point.equals()` và `Point.hashCode()` đúng cách.

---

### [OPT-04] `Chess.java` — `deadMen` list tăng trưởng vô hạn, không xóa button reference

**File:** `chess/Chess.java`  
**Dòng:** 33, 572–594

```java
public ArrayList<Chessman> deadMen = new ArrayList<>();
```

Khi quân bị bắt (`kill()`): `deadMen.add(m)` và `m.isDead = true`. Button được animate fade-out rồi removed khỏi layout. Nhưng `deadMen` vẫn giữ reference tới `Chessman` object, mà `Chessman` có field `button: ImageButton` đã null parent nhưng vẫn là Java object. Sau undo, `button` được tạo lại bằng `restored.createButton()` — button cũ bị bỏ.

Không phải leak nghiêm trọng nhưng với nhiều ván nhiều undo/redo, memory footprint tăng dần.

---

## Checklist Khắc Phục Ưu Tiên

- [ ] **[ML-01]** Chuyển `Chess.ctx` thành `WeakReference<Context>`
- [ ] **[ML-02]** Xóa `Storage.chess` static field, chỉ dùng `WeakReference<Chess>`  
- [ ] **[ML-03]** Cancel coroutine trong `initSplashScreen()` / dùng `take(1)` thay `collectLatest`
- [ ] **[ML-04]** Gọi `aiHandler.removeCallbacksAndMessages(null)` trong `ChessBoardActivity.onDestroy()`
- [ ] **[BUG-01]** Thêm lifecycle check trong AI thread trước khi `doMove()`
- [ ] **[BUG-02]** Cancel `currentBlinkAnimator` và clear listener trong `onDestroy()`
- [ ] **[BUG-04]** Fix undo logic cho Pawn Promotion
- [ ] **[BUG-05]** Gọi `MobileAds.initialize()` trên Main Thread
- [ ] **[WARN-02]** Guard null check cho `v.getParent()` trong `resetValidMoveButtons()`
- [ ] **[WARN-06]** Lưu Handler reference trong `SplashActivity` và cancel trong `onDestroy()`
