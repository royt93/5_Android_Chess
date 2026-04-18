# Kế Hoạch Thay Thế/Nâng Cấp Quảng Cáo Sang SDK `_260321AdmobWrapper`

## 1. Trạng Thái Hiện Tại (Trước Migration)
Project Android Chess hiện đang sử dụng wrapper quảng cáo tự build (`com.saigonphantomlabs.sdkadbmob`). 
Các loại quảng cáo và touchpoint hiện tại:
- **App Open Ad:** Được load và tự handle delay timeout tại `SplashActivity`.
- **Banner Ad:** Nằm trong layout `a_chess_board.xml` và được gọi `loadBanner` từ `ChessBoardActivity`.
- **Interstitial Ad:** Được gọi `loadInterstitial` và `showInterstitial` tại `ChessBoardActivity` khi user nhấn các nút (Restart, Undo) hoặc lúc end game.
- **Reward Ad:** Theo code hiện hành, **KHÔNG CÓ TỒN TẠI** điểm chạm dùng chức năng nhận reward.

Các IDs quảng cáo cấu hình tại `app/build.gradle` chỉ có của nền tảng AdMob (Banner, Interstitial, App Open).

## 2. Kế Hoạch Nâng Cấp (Sau Migration với SDK `_260321AdmobWrapper v1.1.1`)
Mục tiêu là tuân thủ chặt rule "không tự tiện thêm touchpoint mới", cải thiện độ ổn định và tự động tích hợp 7 lớp bảo vệ Safety của Wrapper mà không gây bug, leak memory.

### Các Thông Tin Cần Bổ Sung Để Build Thành Công:
Dựa vào document của `_260321AdmobWrapper`, cấu hình `AdSdkConfig` bắt buộc cần các Keys/IDs của mạng AppLovin MAX. Vì project này hiện chưa có AppLovin IDs, **tôi cần bạn cung cấp thêm:**
- `APPLOVIN_SDK_KEY`
- `APPLOVIN_BANNER_ID`
- `APPLOVIN_INTERSTITIAL_ID`
- `APPLOVIN_APP_OPEN_ID`
*(Nếu không có, cần xác nhận sử dụng String empty `""` và đặt `IS_ENABLE_ADMOB = true`)*

### Lộ Trình Thực Hiện (Step-by-step):
**Bước 1: Cleanup codebase**
- Xóa đứt điểm package `com.saigonphantomlabs.sdkadbmob` để dọn dẹp memory leak.
- Refactor file `build.gradle`, thêm repository `jitpack.io` (root) và implementation Wrapper (`com.github.royt93:AdmobWrapper:1.1.1`).
- Bổ sung các properties cho SDK Applovin vào file AndroidManifest và BuildConfig.

**Bước 2: Application Init (`MyApplication.kt`)**
- Thực hiện setup khởi tạo `AdSdkConfig`, gọi `AdManager.setConfig()`, và `AdManager.earlyInit()` đảm bảo theo tiêu chuẩn Document của SDK. Thay thế hàm khởi tạo cũ của class `AdMobManager`.

**Bước 3: Tích hợp Splash & App Open Ad**
- Tại `SplashActivity`, xóa code timeout handle cũ. Update lên hàm `AdManager.initSplashScreen(this)`. SDK mới sẽ tự lo xử lý timeout 8s hay 10s cực kì an toàn bằng session minimum của AdSafety. 

**Bước 4: Tích hợp Banner Ad**
- Tại `ChessBoardActivity`, cập nhật hàm get Adview: `adView = AdManager.loadBanner(...)`.
- **Quan trọng:** Gắn đủ 3 Lifecycle event (`onResume`, `onPause`, `onDestroy`) gọi qua AdManager để chống leak và impression ảo.

**Bước 5: Tích hợp Interstitial Ad**
- Cập nhật chạm gọi `AdManager.loadInterstitial(this)` thay cho code cũ.
- Tại listener Button Undo/Restart, gọi `AdManager.showInterstitial(this)`. Ở đây cơ chế Throttle 60s và Hourly Cap tự động của wrapper sẽ bảo vệ App khỏi spam impressions. Tốt hơn cấu hình cũ.

**Bước 6: Verification & Double Check**
- Thực hiện build `assembleDebug` đảm bảo code compile thành công.
- Double check memory leak và logic touchpoint. Verify Ad policies (Session minimum, Ads/hour) được tuân thủ nghiêm túc.

--- 
> **Lưu ý cuối cùng:** Do Reward Ad không có ở source này nên tôi sẽ SKIP Reward Ad hoàn toàn theo đúng nguyên tắc Không Chèn Trái Phép. Phía trên là plan chi tiết tuân chuẩn Document. Khi bạn sẵn sàng cung cấp AppLovin ID, hãy báo để tiến hành triển khai!
