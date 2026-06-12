# Memory leaks trong AdmobApplovinWrapper (báo tác giả `royt93`)

> Phát hiện qua LeakCanary trên app Quick Chess (wrapper `com.github.royt93:AdmobApplovinWrapper:1.1.5`).
> Các leak này nằm trong SDK — app **không sửa được triệt để**, chỉ giảm thiểu phía app (xem ghi chú).

## 🔴 1. `AdManager.splashTimeoutRunnable` giữ callback `initSplashScreen` static (nặng nhất)

**Chain (LeakCanary):**
```
GC Root: Thread
 → AdManager class (static splashTimeoutRunnable)
 → AdManager$$ExternalSyntheticLambda3/2
 → <host>.SplashActivity$2 (callback Function0 truyền vào initSplashScreen)
 → SplashActivity  (~918KB × mỗi lần mở app)
```
**Nguyên nhân:** `initSplashScreen(activity, onComplete)` đăng ký một timeout `Runnable` (Handler.postDelayed) tham chiếu `onComplete`, nhưng **không remove/clear runnable** sau khi navigate hoặc khi timeout xong → callback (và Activity nó capture) bị giữ static vĩnh viễn.

**Đề xuất SDK:** `handler.removeCallbacks(splashTimeoutRunnable)` + `splashTimeoutRunnable = null` ngay sau khi onComplete fire / ad dismiss / timeout; hoặc giữ callback bằng `WeakReference`.

**Giảm thiểu phía app (đã làm):** truyền callback là **static nested class + `WeakReference<Activity>`** thay anonymous inner class → Activity GC được dù SDK giữ static. (Xem `SplashActivity.SplashNavCallback`/`ConsentCallback`.)

## 🟠 2. `AdManager.provider` giữ context (×1–2)
Provider (`AppLovinProvider`/`AdMobProvider`) là static field của `AdManager`, giữ tham chiếu context/activity (qua banner/rewarded listener). App không clear được (static singleton).
**Đề xuất SDK:** dùng `applicationContext` trong provider; clear activity ref ở `onAdHidden`/`onDestroy`.

## 🟡 3. `t3$b.b` / `HashMap.[x]` (obfuscated)
Internal collections/closures của SDK còn retained sau init. Nhỏ, khó truy nguyên do R8 obfuscate. Cần SDK author có mapping để xác định.

---
**Tình trạng app Quick Chess:** sau fix #1 (app-side) + dùng `applicationContext` cho grant VIP/loadRewarded, LeakCanary **không còn dump** cho `SplashActivity`/`VipActivity`/`ChessBoardActivity`. Các leak #2/#3 còn lại là SDK-internal, kích thước nhỏ.
