# Kế Hoạch Upgrade Quảng Cáo `AdmobApplovinWrapper` 1.1.1 → 1.1.5 + VIP Feature

> Nguồn: `doc/AD_PROMPT_AOS.MD` (template). Quyết định đã chốt: SDK **1.1.5**, VIP host = **VipActivity**, scope = **Full**.
> ⚠️ Đây là **UPGRADE** (project đã chạy wrapper 1.1.1), KHÔNG phải migrate from-scratch.

---

## ⏸️ TRẠNG THÁI PAUSE — 2026-06-11 (resume ~2026-06-14)

**Update 2026-06-12 (resume)**: Phần code VIP/ads đã được **commit ở `08bd5e1 "update ad"`**. Item #2 (blacklist `VipActivity` khỏi App Open) **ĐÃ LÀM** — `MyApplication.kt:141` nay skip cả `VipActivity`; `assembleDebug` xanh. Còn lại item 1/3/4 cần device thật / dịch giả native.

**Update 2026-06-12 (UI revamp VIP — verify trên Pixel `2B051FDH3006MU`)**:
- 🔴 **ROOT BUG light mode**: theme app là `Theme.Material3.DayNight` nhưng màn VIP luôn nền tối → ở **light mode**, component Material lấy màu mặc định sáng (onSurface/outline tối) trên nền tối = **chữ/nút vô hình**. FIX: thêm `Theme.Chess.Vip` (ép `Theme.Material3.Dark`, khoá colorSurface/onSurface/outline tông tối), gán cho `VipActivity` trong manifest. Verify thật ở `night=no` → render đủ.
- Layout revamp: gom status/get-vip/plans thành các card có section header (`vip_section_get`/`vip_section_plans`, dịch 15 locale); input bo tròn filled gold; nút **Activate** filled gold + `enabled=false` mặc định, TextWatcher bật khi nhập key + pop anim; pulse Watch-ad giảm 1.05→1.03 + margin 6dp + `clipChildren=false` (hết cut mép); nút buy/restore disabled set màu tường minh (hết tàng hình); footer `privacy_options` đổi nhãn → "Ad Choices" (15 locale) + divider.
- Tests: **149 unit/widget PASS** (0 fail); `assembleRelease` (lintVitalRelease, R8) xanh ~13.2MB.

**Update 2026-06-12 (round feedback 2 — 5 điểm + bug watch-ad)**:
- 🔴 **BUG watch-ad → 3 ngày VIP KHÔNG grant** (verify thật trên device, đã FIX + xác nhận grant): `onResume` gọi `AdManager.loadRewarded()` vô điều kiện → khi đóng rewarded, VipActivity resume → reload **hủy instance ad vừa xem TRƯỚC khi `onAdHidden` fire** (log: "No operations on a destroyed instance"). SDK chỉ trả reward (`onRewardedDone(earned)`) trong `onAdHidden` → grant bị mất. FIX: cờ `rewardedInFlight` (set khi showRewarded, clear trong callback) → onResume KHÔNG reload khi đang dở; preload lượt kế trong callback. Grant cũng chuyển dùng `applicationContext` + persist vô điều kiện (không gate `_binding`). Verify device: "activateVipByKey ✅ VIP activated for 3 days" + crown gold + active-state UI. Test regression `watchAd_grantsVip`.
- Điểm 1: nút VIP crown ở home thêm entry-anim + scale heartbeat (mạnh hơn 3 nút kia) + crown wiggle ±9° (`MainActivity`).
- Điểm 2: "Ad Choices" (privacy_options) chỉ hiện khi UMP `PrivacyOptionsRequirementStatus.REQUIRED` (EEA); non-EEA ẩn cả entry + divider. Dùng UMP api (exposed qua wrapper).
- Điểm 3: viền input "Enter VIP code" dùng ColorStateList (gold focus / xám dịu unfocused) thay viền sáng gắt.
- Điểm 4: dialog Material You success/failed (thay Toast) + 3 string dialog × 15 locale (escape `'`).
- Điểm 5: bỏ chip "VIP Active" header (overlay) → báo trạng thái trên nút crown home (gold-filled `bg_btn_vip_active` + crown đậm).
- Phụ: label "VIP %d days" dùng `Math.round` (hết lệch "2 days" vs "3 days left").
- Tests: **150 PASS** (0 fail); `assembleRelease` xanh.

### Đã hoàn thành & verify
- Code: upgrade SDK 1.1.5, consent UMP, rewarded, VIP screen (13 UI + 5 anim), i18n 14 ngôn ngữ, banner autoManageLifecycle.
- Unit/widget tests: **129 test PASS** (gồm 46 VIP test). `assembleDebug`/`assembleRelease` xanh.
- **Đã test thật trên Pixel 7 Pro** (wireless adb `192.168.21.185:42797`, app đã cài bản debug có fix):
  - ✅ Splash → consent (UMP/GMS) → App Open (AppLovin) show→dismiss→MainActivity.
  - ✅ Nút VIP ở MainActivity → mở VipActivity (+138ms, không crash).
  - ✅ VipActivity free-state render đúng (hero "Free User", acquire-controls, buy disabled). Xem `/tmp/vip_free.png`.
  - ✅ Rewarded AppLovin (ID `0cf28eca430409d9`) load + **Shown** + **Earned** runtime thật.
  - ✅ Không FATAL/crash/leak trong logcat.

### 🔴 BUG phát hiện qua device test + ĐÃ FIX (chờ re-verify trên device)
**Single-secret bug**: lib `activateVipByKey` chỉ chấp nhận key == `adConfig.vipKeySecret` (= key 30 ngày). Code cũ gọi `activateVipByKey(VIP_3D_KEY, 3)` cho rewarded/typed-3day → key ≠ secret → **return false → VIP KHÔNG được grant** (logcat: Earned nhưng vẫn "Free User").
- **Fix đã apply** (`VipActivity.kt`): `applyVip(days)` LUÔN grant bằng `AdKeys.VIP_SECRET` + `days`; `days` do `VipKeys.lookupDays()` validate cục bộ. `grantViaRewarded()` → `applyVip(3)`.
- **Fix phụ** (`a_vip_management.xml`): `etKey` inputType `textCapCharacters` → `textVisiblePassword` (key mixed-case, tránh auto-hoa + autocorrect làm sai key).
- **Regression test đã thêm** (`VipActivityActiveStateTest`): `redeem3dKeyViaField` + `redeem30dKeyViaField` → PASS (fail với code cũ). Đã rebuild + cài lại lên device.

### ▶️ ĐIỂM RESUME (việc cần làm tiếp)
1. **Re-verify fix trên device** (đang dở khi pause): xác nhận grant VIP thành công sau fix.
   - Cách deterministic: mở VipActivity → gõ key 30 ngày `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` (gõ TAY trên máy cho chuẩn — `adb input text` làm hỏng ký tự `!` và case) → Activate → phải thấy active-state (countdown/progress/card) + confetti.
   - Hoặc Watch ad → xem hết → grant 3 ngày (lưu ý nhiễu App Open, xem mục 2).
2. ✅ **ĐÃ LÀM (2026-06-12)** — blacklist `VipActivity` khỏi App Open (như `ChessBoardActivity`): `MyApplication.registerCustomAppOpenLifecycle()` nay skip cả `VipActivity`, tránh App Open đè lên rewarded ("Display failed: another fullscreen ad already showing"). `assembleDebug` xanh.
   - **Refactor để testable**: tách `MyApplication.APP_OPEN_BLACKLIST` (Set) + `shouldSkipAppOpen(name)` (pure predicate, fail-safe null→skip) ra `companion object`; observer gọi predicate thay logic inline.
   - **Test 3 tầng cho mọi case (20 mới PASS + 3 integration compile-PASS)**:
     - Unit thuần JVM `AppOpenPolicyTest` (14): 3 blacklisted→skip · main/promotion/unknown→show · null→skip · empty/lowercase/uppercase/FQN/whitespace→không match · blacklist contract (size + exact set).
     - Widget Robolectric `AppOpenPolicyRobolectricTest` (6): **rename-guard** — nối predicate với `simpleName` runtime của activity class thật (Splash/ChessBoard/Vip→skip, Main/Promotion→show) + blacklist không có entry mồ côi.
     - Integration `AppOpenPolicyInstrumentedTest` (androidTest, 3): xác minh contract tên-lớp↔policy trên runtime device thật (`./gradlew connectedDebugAndroidTest`).
3. **km (Khmer) + lo (Lào)**: nhờ native review trước publish (dịch best-effort).
4. **Espresso integration** (`VipActivityEspressoTest`): chạy `./gradlew connectedDebugAndroidTest` trên device (mới compile-pass, chưa chạy runtime).
5. Khi xong hết + user duyệt → mới commit.

---

## 0. Trạng thái hiện tại (audit)

| # | Câu hỏi | Trả lời |
|---|---|---|
| 1 | Đã apply wrapper chưa? | ✅ Có — `AdmobWrapper:1.1.1`, package `com.roy.sdkadbmob.*`. Không còn SDK cũ. |
| 2 | `Application.onCreate` init gì? | `MyApplication.kt:35` `setupAd()` → `setConfig` + `earlyInit` + init thủ công AppLovin/AdMob + **custom App Open lifecycle** (blacklist `SplashActivity`/`ChessBoardActivity`). |
| 3 | Splash có consent + App Open warmup? | App Open warmup ✅ (`initSplashScreen`, `SplashActivity.java:76`). **Consent UMP ❌ chưa có.** |
| 4 | Activity nào load banner/interstitial? | `ChessBoardActivity` — banner `:151`, interstitial load `:155`, show `:255`/`:527`, banner lifecycle `:567/573/613`. |
| 5 | Đã có VIP screen? | ❌ Chưa. Chưa có rewarded. → **Tạo mới VipActivity.** |

**Ngầm định kiến trúc**: Activities = Java; `MyApplication`/`BaseActivity` = Kotlin; Material `1.13.0`; **KHÔNG có Navigation Component** → VIP host bằng `VipActivity` (Kotlin, extends `BaseActivity`), nội bộ có thể inflate `FVipManagement` view-logic.

---

## 1. Touchpoint table

| Slot | File hiện tại | Trigger | API mới | Trạng thái |
|---|---|---|---|---|
| Banner | `ChessBoardActivity` | `onCreate` | `AdManager.loadBanner` | Giữ nguyên (đã có) |
| Interstitial | `ChessBoardActivity` | Undo / Restart / end-game | `AdManager.showInterstitial` | Giữ nguyên trigger (đã có) |
| App Open (splash) | `SplashActivity` | warmup trước khi vào Main | `AdManager.initSplashScreen` | Giữ nguyên (đã có) |
| App Open (resume) | `MyApplication` | `ProcessLifecycleOwner` ON_START | custom observer (blacklist board) | Giữ nguyên (đã có) |
| **Rewarded** | **VipActivity** | nút "Xem QC → 3 ngày VIP" | `loadRewarded` + `showRewarded`, fallback `showInterstitial` | **MỚI** |
| **VIP-by-key** | **VipActivity** | nhập key | `activateVipByKey` / `showVipByKeyDialog` | **MỚI** |
| **Consent** | **SplashActivity** | `onCreate` | `requestConsentInfoUpdate` | **MỚI** |

---

## 2. Step-by-step plan

### Bước 1 — Bump dependency + BuildConfig
- `app/build.gradle:155`: `com.github.royt93:AdmobWrapper:1.1.1` → **`com.github.royt93:AdmobApplovinWrapper:1.1.5`**.
- Import `com.roy.sdkadbmob.*` **GIỮ NGUYÊN** (artifact đổi tên, package không đổi).
- Thêm `buildConfigField` cho **cả `debug` lẫn `release`**:
  - `APPLOVIN_REWARDED_ID` — *(cần user cấp)*
  - `ADMOB_REWARDED_ID` — *(cần user cấp; debug có thể dùng test ID Google `ca-app-pub-3940256099942544/5224354917`)*
  - `PRIVACY_POLICY_URL` — *(cần user cấp)*

### Bước 2 — `MyApplication.kt` (App init + config mở rộng)
- Bổ sung vào `AdSdkConfig`: `admobRewardedId`, `applovinRewardedId`, `applovinSdkKey = BuildConfig.APPLOVIN_SDK_KEY`, `vipKeySecret = AdKeys.VIP_SECRET` (Base64-decode key 30 ngày).
- **GIỮ custom App Open lifecycle** (blacklist board) — KHÔNG thay bằng `registerAppOpenAdLifecycle`/`initialize()` của SDK vì sẽ mất rule skip `ChessBoardActivity`.
- Tùy chọn: gắn `AdSafetyLimits` debug-friendly (`if (BuildConfig.DEBUG)`).

### Bước 3 — Consent UMP (`SplashActivity.java`)
- `onCreate`: gọi `AdManager.requestConsentInfoUpdate(this, ...)` **trước** `initSplashScreen`; block App Open/Banner/Interstitial tới khi consent resolved (lib tự lo personalized).
- Ghi nhận 3 scenario (không cần chạy thực): Non-EEA skip; EEA accept; EEA deny.

### Bước 4 — VIP infra (Kotlin, package mới `com.saigonphantomlabs.feature.vip`)
- `VipKeys.kt` — Base64 của key 30 ngày + 3 ngày → `lookupDays()`.
- `VipPrefs.kt` — lưu `grantedAtMs` + `userRedeemedOnce` (lib chưa expose grantedAt).
- `AdKeys.kt` (`common/const`) — `VIP_SECRET` (Base64) + `PRIVACY_POLICY_URL` từ BuildConfig.

### Bước 5 — `VipActivity` (UI mandatory: 13 component + 5 animation)
- Layout `a_vip_management.xml` (Material 3), ViewBinding.
- 13 component theo template 10.3: hero card, activation/expiry row, **progress bar elapsed-semantic** (rỗng→đầy), countdown `CountDownTimer`, nhập key, nút "Xem QC→3 ngày VIP" (rewarded + fallback interstitial), 4 nút buy disabled, single active VIP card (grace 🎁 vs redeemed), restore disabled, revoke-all, privacy footer.
- 5 animation: pulse / slide-in / crown shimmer / count-up / confetti + haptic.
- Memory rules 10.5: `CountDownTimer.cancel()`, animator `.cancel()`, `_binding=null` ở `onDestroy`, callback check `isFinishing`/`binding==null`.

### Bước 6 — `MainActivity` entry + badge
- Chip "VIP" vàng (visible khi `isVipByKeyActive()`), refresh ở `onResume`.
- Nút/menu mở `VipActivity` (Intent).

### Bước 7 — strings vi + en (template 10.7) + drawable icons.

---

## 3. So sánh trước / sau

| Khía cạnh | Trước (1.1.1) | Sau (1.1.5) |
|---|---|---|
| Provider | AppLovin MAX (AdMob fallback) | Như cũ |
| Architecture | provider-based | Như cũ + rewarded/VIP providers |
| App Open | splash warmup + custom resume | Giữ nguyên (blacklist board) |
| Consent | ❌ không | ✅ UMP ở Splash |
| Frequency cap | AdSafety mặc định | + debug-friendly limits (optional) |
| VIP | ❌ không có | ✅ auto-trial 1 ngày + key + rewarded |
| Rewarded | ❌ | ✅ trong VipActivity |
| Memory leak risk | banner manual lifecycle (ok) | + VIP screen tuân memory rules 10.5 |
| Lifecycle | custom App Open observer | Giữ nguyên |

---

## 4. Thông tin & quyết định (ĐÃ CHỐT)

| Field | Giá trị |
|---|---|
| `APPLOVIN_REWARDED_ID` | `0cf28eca430409d9` (debug + release) ✅ |
| `ADMOB_REWARDED_ID` | test ID `ca-app-pub-3940256099942544/5224354917` (cả debug + release — AppLovin active nên admob không chạy runtime) ✅ |
| `PRIVACY_POLICY_URL` | `https://loitp.notion.site/Term-Privacy-Policy-Disclaimer-319b1cd8783942fa8923d2a3c9bce60f` ✅ |
| VIP key 30 ngày | `9fA0q7eN!27cLx04@21993Y2u0I7#Q0` → Base64 hardcode |
| VIP key 3 ngày | `eQ7@93L0f!2Y2707xN04021993u0I#2aK` → Base64 hardcode |

**Quyết định triển khai:**
- SDK: **1.1.5** · VIP host: **VipActivity** (Kotlin, extends BaseActivity) · scope: **Full**.
- Animation: **thêm konfetti-xml 2.0.5 + shimmer 0.5.0** (user chấp nhận tradeoff size cho UX đẹp).
- Banner lifecycle: **đổi sang `autoManageLifecycle=true`** (bỏ 3 override `bannerResume/Pause/Destroy` ở ChessBoardActivity).
- App Open custom lifecycle (blacklist board): **GIỮ NGUYÊN**.
- Buy VIP (30d/90d/1y/lifetime) + Restore Purchase: **disabled** ("sắp ra mắt", chờ IAP).

AppLovin SDK key + banner/inter/appopen + AdMob banner/inter/appopen: **đã có sẵn** trong `app/build.gradle`.

---

## 5. Build phải xanh
- `./gradlew :app:assembleDebug`
- `./gradlew :app:assembleRelease`

---

## 6. KẾT QUẢ TRIỂN KHAI (đã hoàn thành)

### Migrate Result
- **Build**: assembleDebug = ✅ PASS · assembleRelease (R8 full + shrinkResources) = ✅ PASS
- **Slots**: Banner ✅ (autoManageLifecycle) · Interstitial ✅ giữ trigger · App Open ✅ giữ custom lifecycle · **Rewarded ✅ mới** · **Consent UMP ✅ mới** · **VIP-by-key ✅ mới**
- **APK size**: debug 20M · **release 13M** (konfetti+shimmer R8 nuốt gọn, không cần thêm proguard)
- **Policy (5/5 PASS)**: banner dock bottom không che nút · App Open không show splash thuần (warmup→navigate) + skip board · interstitial trông cậy AdSafetyLimits.GAME (release) / TEST (debug) · rewarded chỉ trigger khi user bấm · không fake close button
- **Memory review (OK)**: VipActivity dispose đủ (CountDownTimer.cancel, animators.cancel, shimmer.stop, _binding=null, callback guard `_binding==null`) · ChessBoard banner SDK auto-manage · MyApplication dùng WeakReference
- **VIP UI**: 13/13 components + 5/5 animations (pulse/slide-in/shimmer/count-up bump/confetti+haptic)
- **Files changed**: build.gradle, MyApplication.kt, SplashActivity.java, ChessBoardActivity.java, MainActivity.java, AndroidManifest, colors/strings(vi+en), a_main.xml + **8 file mới** (AdKeys/VipKeys/VipPrefs/VipActivity.kt, a_vip_management.xml, 8 drawable)

### Files mới
```
common/consts/AdKeys.kt · feature/vip/{VipKeys,VipPrefs,VipActivity}.kt
res/layout/a_vip_management.xml
res/drawable/{ic_workspace_premium,ic_confirmation_number,ic_card_giftcard,
              ic_restore,ic_play_circle,ic_arrow_back,
              bg_vip_status_header_active,bg_vip_status_header_free}.xml
```

### 6.1 — Audit fixes (round 2)
- 🔴 **Fixed VIP-reduction bug**: ẩn acquire-controls (nhập key + xem QC) khi `isVipByKeyActive()` + `applyVip()` extend-guard chặn activate làm rút ngắn thời hạn (lib ghi đè `expiry=now+days`, không cộng dồn). Trước fix: VIP 30 ngày bấm "xem QC" → còn 3 ngày.
- 🟡 **GDPR re-consent**: thêm entry "Privacy options" ở footer VIP → `AdManager.showConsentFormIfAvailable()` (EEA mới show, non-EEA no-op).
- 🟢 `tvEntryRemaining` đổi sang "Còn %d ngày" (bỏ trùng expiry row); first-tick countdown không bump (hết giật); pulse chỉ chạy khi watch-ad visible.

### 6.2 — i18n đầy đủ + Tests (round 3)
- 🌐 **Dịch native 14 ngôn ngữ**: thêm VIP strings (28 vip_* + privacy_policy/privacy_options/confirm) cho **en (base) + vi + de/es/fr/id/ja/km/ko/lo/ms/pt/th/tr/zh**. `vip_remaining` countdown localize đơn vị d/h/m/s theo từng ngôn ngữ (CJK: 日/時/分/秒, th/km/lo theo chữ bản địa). `assembleRelease` (lintVitalRelease) PASS → không thiếu translation / sai format-arg.
  - ⚠️ Khuyến nghị native review riêng cho **km (Khmer)** + **lo (Lào)** trước khi publish (best-effort, ngữ pháp phức tạp).
- 🧪 **Tests (44 VIP unit/widget PASS · full suite 127 PASS, 0 fail)**:
  - Tách `VipMath.kt` (pure) → unit-test trực tiếp JVM.
  - `VipMathTest` (21): elapsedProgress (atGrant/atExpiry/mid/clamp/zero-total/clock-skew), remaining decompose, daysLeftCeil, isExtension extend-guard.
  - `VipKeysTest` (8, Robolectric): decode 30d/3d, lookupDays valid/trim/invalid/empty/case-sensitive.
  - `VipPrefsTest` (5, Robolectric): grantedAt save/get/clear, userRedeemed.
  - `AdKeysTest` (3, Robolectric): VIP_SECRET decode + single-secret contract + privacy URL https.
  - `VipActivityRobolectricTest` (3, widget): **free-state** UI, invalid-key toast (ShadowToast), free-user label.
  - `VipActivityActiveStateTest` (4, widget): **active-state** (seed VIP qua activateVipByKey) — countdown/progress/card hiện + acquire-controls ẩn + revoke enabled; grace 🎁 label; redeemed "%d days" label; **revoke dialog flow** → quay về free-state.
  - `VipActivityEspressoTest` (androidTest, 3): integration — compile PASS; chạy bằng `./gradlew connectedDebugAndroidTest` trên device/emulator.
  - AdManager singleton cô lập qua `@Before`/`@After` clear (chống leak chéo giữa test class).

### Lưu ý vận hành
- VIP key (30d/3d) Base64-obfuscated trong `VipKeys.kt`/`AdKeys.kt` — grep plain key trong source = 0 match.
- AdMob rewarded đang dùng test ID; khi bật `IS_ENABLE_ADMOB=true` để chạy AdMob production phải đổi sang rewarded ID thật.
- Auto-trial VIP 1 ngày là built-in SDK (chỉ chạy ở **release** build, debug skip) — không re-implement ở app code.
- Grace entry detect bằng proxy `isVipByKeyActive() && !VipPrefs.userRedeemedAtLeastOnce()` (lib chưa expose `isAddVIPMemberFirstInitSuccess()` ra public API).
</content>
</invoke>
