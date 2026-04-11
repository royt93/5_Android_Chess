package com.saigonphantomlabs.sdkadbmob

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.saigonphantomlabs.chess.BuildConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.Collections

//version 20250927
//check gradle
//copy full this class
//check splash ProgressBar#indeterminateTint
//xoa theme v35
//loadBanner them tvLabelAd
//layout_ad_banner them id tvLabelAd
//migrate edge to egde (check setupEdgeToEdge1 & setupEdgeToEdge2)
//copy full themes.xml

/**
 * Production optimization flags
 */
object ProductionConfig {
    // Disable demo features in production
    val ENABLE_DEMO_FEATURES = BuildConfig.DEBUG

    // Reduce logging in production
    val ENABLE_VERBOSE_LOGGING = BuildConfig.DEBUG

    // Optimize memory usage
    val ENABLE_MEMORY_OPTIMIZATION = !BuildConfig.DEBUG
}

/**
 * Safe logging wrapper that doesn't crash in unit tests
 */
object SafeLogger {
    fun d(tag: String, message: String) {
        try {
            if (ProductionConfig.ENABLE_VERBOSE_LOGGING) {
                Log.d(tag, message)
            }
        } catch (e: Exception) {
            // Ignore in unit tests
        }
    }

    fun w(tag: String, message: String) {
        try {
            if (ProductionConfig.ENABLE_VERBOSE_LOGGING) {
                Log.w(tag, message)
            }
        } catch (e: Exception) {
            // Ignore in unit tests
        }
    }
}

object AdMobManager {

    private const val TAG = "roy93~AdMobManager"

    private var application: Application? = null
    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenLoading = false
    private var isAppOpenShowing = false
    private var lastAppOpenLoadTime: Long = 0

    private const val APP_OPEN_AD_TIME_OUT = 4 * 60 * 60 * 1000L // 4 hours

    const val TEST_VSMART_IRIS = "884670AFCACDD337E31BB6153C6DB17E"
    const val TEST_VIVO_Z9 = "05B522309BC31052952BBCD5CC85ACA8"

    private var currentDeviceGAID = ""
    private var isVIPMember = false
    // HashSet để tối ưu hiệu suất lookup O(1) thay vì O(n)
    // Thread-safe VIP Member Management với backward compatibility
    private val setGAIDVipMember = Collections.synchronizedSet(HashSet<String>())

    private var appPreferences: AppPreferences? = null
    private var currentActivity: WeakReference<Activity>? = null

    var interstitialListener: InterstitialAdListener? = null

    private var lastInterstitialErrorTime: Long = 0
    private var lastAppOpenErrorTime: Long = 0
    private val ERROR_COOLDOWN = 15 * 60 * 1000L // 15 phút dưới dạng milliseconds

    // [ML-03] Store splash screen Job so it can be cancelled properly
    private var splashJob: Job? = null

    fun init(
        app: Application?,
        onComplete: (Boolean, String) -> Unit,
    ) {
        if (app == null) {
            return
        }
        appPreferences = AppPreferences.getInstance(app)
        appPreferences?.getGAIDList()?.let {
            setGAIDVipMember.addAll(it)
        }
        Log.d(TAG, "###init setGAIDVipMember size: ${setGAIDVipMember.size}")

            getGAID(app) { gaidCurrent ->
            this.application = app
            this.currentDeviceGAID = gaidCurrent
            isVIPMember = setGAIDVipMember.contains(gaidCurrent)
            Log.d(TAG, "###init Current device GAID: $gaidCurrent, isWhitelistedDevice: $isVIPMember")

            //set test devices for all Roy's devices
            setTestDeviceIds(
                TEST_VSMART_IRIS,
                TEST_VIVO_Z9,
            )

            //set vip member
            if (appPreferences?.isAddVIPMemberFirstInitSuccess() == true) {
                //do nothing
            } else {
                if (BuildConfig.DEBUG) {
                    //do nothing
                } else {
                    val list = getMyListVipDevice()
                    addVIPMember(list)
                    appPreferences?.addVIPMemberFirstInitSuccess()
                }
            }
            onComplete(true, gaidCurrent)
            CoroutineScope(Dispatchers.Default).launch {
                EventBus.sendEvent(true)
            }
        }
    }

    fun getMyListVipDevice(): ArrayList<String> {
        val list = ArrayList<String>()
        list.add("9ad0127d-04be-4b6c-937a-ca3ed7f650b9")//vsmart iris
        list.add("9b6499f2-d4de-4b9e-afdf-ac2a2b127fb1")//ss a50
        list.add("c09b2f04-e145-490c-96f9-dab620074104")//oppo f7
        list.add("c228aa08-bedd-4e6e-adf6-ae5e95bcddae")//vivo v15
        list.add("46259467-0ac4-49c4-a3a2-7d3db3ce4bda")//tecno spark 20 pro +
        list.add("1b7c3e3f-c709-4e85-b26f-dd74c4df2ed7")//vivo 1906
        list.add("adaa42e7-9cc6-4a8a-9c90-d4d87842b12c")//tecno spark go 2024
        list.add("f5a36a2f-5add-4315-a171-0f8dddab78c7")//ss s20u
        list.add("6fbb207d-341d-470d-bb0a-dddd79522b32")//ss a52
        list.add("40f8e222-cf7a-4fac-9913-6809c4c58817")//mipad 5
        list.add("932099db-d381-4b52-98dc-5b96ba8b4ff4")//oppo reno 2f
        list.add("a1339bd1-8ea5-47cd-969e-4b5721b576b7")//redmi note 8+
        list.add("3f2f21d2-85eb-451b-a1a5-003668ba6345")//zte blade
        list.add("261f772c-6a10-499c-b896-4157d9ab6a25")//ss a11
        list.add("460d3f5c-bbe2-46fc-841a-6381e3c93864")//redmi95
        list.add("49606ad7-5cee-43b4-9af7-8aa274644737")//redmi note 13 pro
        list.add("6cf051f8-83f5-43b7-8c1a-1d20ae1f8d93")//redmi pad pro
        list.add("da10cb05-5458-42df-ba86-630732356b35")//vivo z9
        list.add("8f6ccdc1-08fd-4611-abdf-f48bdadb5581")//tablet lenovo
        list.add("66e652de-79ef-4889-8074-9b482fd81b5a")//redmi a3
        list.add("4ed22dd8-e8fb-442e-a75e-081a3d977957")//ss s24u
        return list
    }

    fun getGAID(context: Context, callback: (String) -> Unit) {
        Thread {
            try {
                val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
                val id = info.id ?: ""
                // [WARN-05] Post callback to Main Thread to avoid data race
                Handler(Looper.getMainLooper()).post { callback(id) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback("") }
                Log.d("AdMobManager", "getGAID error $e")
            }
        }.start()
    }

    fun setCurrentActivity(activity: Activity) {
        this.currentActivity = WeakReference(activity)
    }

    // [BUG-03] Clear stale Activity reference when the Activity is destroyed
    fun clearCurrentActivity() {
        this.currentActivity = null
    }

    //search logcat: "to get test ads on this device"
    fun setTestDeviceIds(vararg deviceIds: String) {
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(deviceIds.toList()).build()
        MobileAds.setRequestConfiguration(configuration)
        Log.d(TAG, "setTestDeviceIds deviceIds ${deviceIds.toList()}")
    }

    fun getTestDeviceIds(): List<String> {
        val testDeviceIds = MobileAds.getRequestConfiguration().testDeviceIds
        Log.d(TAG, "getTestDeviceIds testDeviceIds: $testDeviceIds")
        return testDeviceIds
    }

    fun loadBanner(
        context: Context,
        adUnitId: String,
        container: ViewGroup,
        tvLabelAd: TextView,
        adSize: AdSize = AdSize.BANNER,
    ): AdView? {
        if (isVIPMember) {
            Log.d(TAG, "Banner Ad skipped due to whitelist device")
            container.isVisible = false
            tvLabelAd.isVisible = false
            return null
        }
        if (!NetworkUtils.isDeviceConnected(context)) {
            Log.d(TAG, "loadBanner no internet")
            container.isVisible = false
            tvLabelAd.isVisible = false
            return null
        }
        Log.d(TAG, "loadBanner~~~")
        container.isVisible = true
        tvLabelAd.isVisible = false
        val adView = AdView(context).apply {
            setAdSize(adSize)
            setAdUnitId(adUnitId)
            Log.d(TAG, "adListener init~~~")
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG, "✅ Banner Ad Loaded")
                    tvLabelAd.isVisible = true
                    // Track impression with demo
                    AdRevenueDemo.trackImpression(AdType.BANNER)
                    AdFrequencyDemo.recordAdShown(AdType.BANNER)
                    AdSecurityDemo.validateAdImpression()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "❌ Banner Ad Failed to load: ${error.message}")
                    container.isVisible = false
                    tvLabelAd.isVisible = false
                }

                override fun onAdOpened() {
                    Log.d(TAG, "🎯 Banner Ad Clicked")
                    // Track click with demo and security validation
                    if (AdSecurityDemo.validateAdClick()) {
                        AdRevenueDemo.trackClick(AdType.BANNER)
                        Log.d(TAG, "✅ Banner click được chấp nhận")
                    } else {
                        Log.w(TAG, "⚠️ Banner click bị từ chối - có thể spam")
                    }
                }

                override fun onAdClosed() {
                    Log.d(TAG, "📝 Banner Ad Closed")
                }
            }
        }
        container.removeAllViews()
        container.addView(
            adView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    fun loadInterstitial(
        context: Context,
        adUnitId: String,
    ) {
        if (isVIPMember) {
            Log.d(TAG, "Interstitial Ad skipped due to whitelist device")
            return
        }
        if (!NetworkUtils.isDeviceConnected(context)) {
            Log.d(TAG, "loadInterstitial no internet")
            return
        }
        // Kiểm tra thời gian cooldown cho Interstitial
        if (System.currentTimeMillis() - lastInterstitialErrorTime < ERROR_COOLDOWN) {
            Log.d(TAG, "Interstitial Ad skipped due to recent error")
            interstitialListener?.onAdFailedToLoad(
                LoadAdError(
                    /* code = */ 0,
                    /* message = */ "Ad skipped due to cooldown",
                    /* domain = */ "admob-wrapper",
                    /* cause = */ null,
                    /* responseInfo = */ null,
                )
            )
            return
        }

        InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "✅ Interstitial Ad Loaded")
                interstitialAd = ad
                setInterstitialCallback()
                interstitialListener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                lastInterstitialErrorTime = System.currentTimeMillis() // Cập nhật thời điểm lỗi
                Log.d(TAG, "❌ Interstitial Ad Failed to load: ${error.message}. Cooldown started.")
                interstitialAd = null
                interstitialListener?.onAdFailedToLoad(error)
            }
        })
    }

    private fun setInterstitialCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "✅ Interstitial Ad Shown")
                // Track impression with demo
                AdRevenueDemo.trackImpression(AdType.INTERSTITIAL)
                AdFrequencyDemo.recordAdShown(AdType.INTERSTITIAL)
                AdSecurityDemo.validateAdImpression()
                interstitialListener?.onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial Ad Dismissed")
                interstitialAd = null
                interstitialListener?.onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                interstitialAd = null
                interstitialListener?.onAdFailedToShow(adError)
            }

            override fun onAdClicked() {
                Log.d(TAG, "🎯 Interstitial Ad Clicked")
                // Track click with demo and security validation
                if (AdSecurityDemo.validateAdClick()) {
                    AdRevenueDemo.trackClick(AdType.INTERSTITIAL)
                    Log.d(TAG, "✅ Interstitial click được chấp nhận")
                } else {
                    Log.w(TAG, "⚠️ Interstitial click bị từ chối - có thể spam")
                }
                interstitialListener?.onAdClicked()
            }
        }
    }

    fun showInterstitial(activity: Activity, onDoneFlow: (result: Boolean) -> Unit) {
        if (isVIPMember) {
            Log.d(TAG, "⏭️ Interstitial Show Skipped - Device in VIP whitelist")
            interstitialListener?.onAdNotAvailable()
            onDoneFlow(false)
            return
        }

        if (interstitialAd != null) {
            // Lưu lại callback gốc
            val originalCallback = interstitialAd?.fullScreenContentCallback
            // Tạo callback mới có xử lý onDoneFlow
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "✅ Interstitial Ad Shown")
                    // Track impression with demo (same as setInterstitialCallback)
                    AdRevenueDemo.trackImpression(AdType.INTERSTITIAL)
                    AdFrequencyDemo.recordAdShown(AdType.INTERSTITIAL)
                    AdSecurityDemo.validateAdImpression()
                    originalCallback?.onAdShowedFullScreenContent()
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial Ad Dismissed")
                    originalCallback?.onAdDismissedFullScreenContent()
                    onDoneFlow(true) // Ad đóng thành công
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Interstitial Ad Failed to Show: ${adError.message}")
                    originalCallback?.onAdFailedToShowFullScreenContent(adError)
                    onDoneFlow(false) // Hiển thị thất bại
                }

                override fun onAdClicked() {
                    Log.d(TAG, "🎯 Interstitial Ad Clicked")
                    // Track click with demo and security validation
                    if (AdSecurityDemo.validateAdClick()) {
                        AdRevenueDemo.trackClick(AdType.INTERSTITIAL)
                        Log.d(TAG, "✅ Interstitial click được chấp nhận")
                    } else {
                        Log.w(TAG, "⚠️ Interstitial click bị từ chối - có thể spam")
                    }
                    originalCallback?.onAdClicked()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad not ready")
            interstitialListener?.onAdNotAvailable()
            onDoneFlow(false)
        }
    }

    fun loadAppOpenAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Boolean) -> Unit,
    ) {
        Log.d(TAG, "~~~~~ loadAppOpenAd isVIPMember $isVIPMember")
        if (isVIPMember) {
            Log.d(TAG, "App Open Ad skipped due to whitelist device")
            Handler(Looper.getMainLooper()).postDelayed({
                onAdLoaded.invoke(false)
            }, 1_000)
            return
        }
        if (!NetworkUtils.isDeviceConnected(context)) {
            Log.d(TAG, "loadAppOpenAd no internet")
            Handler(Looper.getMainLooper()).postDelayed({
                onAdLoaded.invoke(false)
            }, 1_000)
            return
        }
        // Kiểm tra thời gian cooldown cho App Open
        if (System.currentTimeMillis() - lastAppOpenErrorTime < ERROR_COOLDOWN) {
            Log.d(TAG, "App Open Ad skipped due to recent error")
            Handler(Looper.getMainLooper()).postDelayed({
                onAdLoaded(false)
            }, 1_000)
            return
        }
        if (isAppOpenLoading) {
            if (BuildConfig.DEBUG) {
                //do nothing
            } else {
                if ((System.currentTimeMillis() - lastAppOpenLoadTime) < APP_OPEN_AD_TIME_OUT) {
                    Log.d(TAG, "App Open Ad is still valid or loading")
                    Handler(Looper.getMainLooper()).postDelayed({
                        onAdLoaded.invoke(false)
                    }, 1_000)
                    return
                }
            }
        }
        isAppOpenLoading = true

        AppOpenAd.load(
            context, adUnitId, AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open Ad Loaded")
                    appOpenAd = ad
                    lastAppOpenLoadTime = System.currentTimeMillis()
                    isAppOpenLoading = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        onAdLoaded.invoke(true)
                    }, 500)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    lastAppOpenErrorTime = System.currentTimeMillis() // Cập nhật thời điểm lỗi
                    Log.d(TAG, "App Open Ad Failed to load: ${error.message}. Cooldown started.")
                    isAppOpenLoading = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        onAdLoaded.invoke(false)
                    }, 1_000)
                }
            },
        )
    }

    fun showAppOpenAd(
        activity: Activity,
        onAdDismiss: (Boolean) -> Unit,
    ) {
        if (isVIPMember) {
            Log.d(TAG, "App Open Ad Show Skipped - Device in whitelist")
            onAdDismiss.invoke(true)
            return
        }
        if (isAppOpenShowing) {
            Log.d(TAG, "Already showing App Open Ad")
            onAdDismiss.invoke(true)
            return
        }
        if (appOpenAd != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "App Open Ad Shown")
                    // Track impression with demo
                    AdRevenueDemo.trackImpression(AdType.APP_OPEN)
                    AdFrequencyDemo.recordAdShown(AdType.APP_OPEN)
                    AdSecurityDemo.validateAdImpression()
                    isAppOpenShowing = true
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "App Open Ad Dismissed")
                    appOpenAd = null
                    isAppOpenShowing = false
                    onAdDismiss.invoke(true)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "App Open Ad Failed to Show: ${adError.message}")
                    appOpenAd = null
                    isAppOpenShowing = false
                    onAdDismiss.invoke(true)
                }

                override fun onAdClicked() {
                    Log.d(TAG, "App Open Ad Clicked")
                    // Track click with demo and security validation
                    if (AdSecurityDemo.validateAdClick()) {
                        AdRevenueDemo.trackClick(AdType.APP_OPEN)
                        Log.d(TAG, "✅ App Open click được chấp nhận")
                    } else {
                        Log.w(TAG, "⚠️ App Open click bị từ chối - có thể spam")
                    }
                }
            }
            appOpenAd?.show(activity)
        } else {
            Log.d(TAG, "App Open Ad not ready")
            onAdDismiss.invoke(true)
        }
    }

    /**
     * Kiểm tra xem thiết bị hiện tại có phải VIP member không
     * Sử dụng HashSet để tối ưu hiệu suất - O(1) lookup
     */
    fun isVIPMember(): Boolean {
        return isVIPMember
    }


    /**
     * Thêm các thiết bị vào danh sách VIP
     * Sử dụng HashSet để tối ưu hiệu suất
     */
    fun addVIPMember(listGaidDevice: List<String>) {
        listGaidDevice.forEach { gaidDevice ->
            setGAIDVipMember.add(gaidDevice)
        }
        appPreferences?.saveGAIDList(setGAIDVipMember.toList())
        isVIPMember = setGAIDVipMember.contains(currentDeviceGAID)
        Log.d(TAG, "Thêm VIP members: $listGaidDevice => isVIPMember: $isVIPMember")
        Log.d(TAG, "Tổng VIP members: ${setGAIDVipMember.size}")
    }

    /**
     * Xóa các thiết bị khỏi danh sách VIP
     */
    fun deleteVIPMember(listGaidDevice: List<String>) {
        listGaidDevice.forEach { gaidDevice ->
            setGAIDVipMember.remove(gaidDevice)
        }
        appPreferences?.saveGAIDList(setGAIDVipMember.toList())
        isVIPMember = setGAIDVipMember.contains(currentDeviceGAID)
        Log.d(TAG, "Xóa VIP members: $listGaidDevice => isVIPMember: $isVIPMember")
        Log.d(TAG, "Còn lại VIP members: ${setGAIDVipMember.size}")
    }

    var countInitSplashScreen = 0

    fun initSplashScreen(activity: Activity, onAdLoaded: () -> Unit) {
        countInitSplashScreen++
        Log.d(TAG, "~~~initSplashScreen countInitSplashScreen $countInitSplashScreen")
        if (countInitSplashScreen > 1) {
            onAdLoaded.invoke()
        } else {
            // [ML-03] Cancel any previous job and use take(1) to avoid infinite collectLatest
            splashJob?.cancel()
            splashJob = CoroutineScope(Dispatchers.Default).launch {
                Log.d(TAG, "~~~initSplashScreen launch")
                EventBus.eventFlow.take(1).collect { value ->
                    Log.d(TAG, "initSplashScreen collect: $value")
                    CoroutineScope(Dispatchers.Main).launch {
                        loadAppOpenAd(
                            context = activity,
                            adUnitId = BuildConfig.ADMOB_APP_OPEN_ID,
                            onAdLoaded = { result ->
                                Log.d(TAG, "onAdLoaded result $result")
                                if (result) {
                                    showAppOpenAd(activity) {
                                        onAdLoaded.invoke()
                                    }
                                } else {
                                    onAdLoaded.invoke()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    interface InterstitialAdListener {
        fun onAdLoaded()
        fun onAdFailedToLoad(error: LoadAdError)
        fun onAdShowed()       // Khi ad bắt đầu hiển thị
        fun onAdDismissed()    // Khi ad đóng
        fun onAdClicked()      // Khi user click ad
        fun onAdFailedToShow(error: AdError) // Khi hiển thị lỗi
        fun onAdNotAvailable() // Khi không có ad sẵn sàng
    }
}


/**
 * Quản lý SharedPreferences cho VIP member management
 */
class AppPreferences private constructor(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("loitp_admob", Context.MODE_PRIVATE)

    // Keys cho VIP management
    private val keyListGAID = "keyListGAID"
    private val keyAddVIPMemberFirstInitSuccess = "keyAddVIPMemberFirstInitSuccess"

    // ================ VIP MANAGEMENT ================

    fun saveGAIDList(list: List<String>) {
        sharedPref.edit {
            putStringSet(keyListGAID, list.toSet())
            // Chuyển List thành Set để tự động loại bỏ trùng lặp
        }
    }

    fun getGAIDList(): ArrayList<String> {
        val set = sharedPref.getStringSet(keyListGAID, emptySet()) ?: emptySet()
        return ArrayList(set) // Chuyển Set thành ArrayList
    }

    fun addVIPMemberFirstInitSuccess() {
        sharedPref.edit {
            putBoolean(keyAddVIPMemberFirstInitSuccess, true)
        }
    }

    fun isAddVIPMemberFirstInitSuccess(): Boolean {
        return sharedPref.getBoolean(keyAddVIPMemberFirstInitSuccess, false)
    }

    // ================ UTILITIES ================

    /**
     * Xóa tất cả dữ liệu (dùng cho testing/reset)
     */
    fun clearAllData() {
        sharedPref.edit {
            clear()
        }
    }

    companion object {
        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}

object EventBus {
    private val _eventFlow = MutableSharedFlow<Boolean>(replay = 1)
    val eventFlow = _eventFlow.asSharedFlow()

    suspend fun sendEvent(value: Boolean) {
        _eventFlow.emit(value)
    }
}


object NetworkUtils {
    @SuppressLint("ObsoleteSdkInt")
    fun isDeviceConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }
}

object UIUtils {
    fun setupEdgeToEdge1(window: Window) {
        // Edge-to-edge cho Android 10+ (API 29+)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    fun setupEdgeToEdge2(
        rootView: View,
        paddingTop: Boolean = true,
        paddingBottom: Boolean = true,
    ) {
        // Nếu cần inset padding cho layout chính
        ViewCompat.setOnApplyWindowInsetsListener(rootView, object : androidx.core.view.OnApplyWindowInsetsListener {
            override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    /* left = */ systemBars.left,
                    /* top = */ if (paddingTop) systemBars.top else 0,
                    /* right = */ systemBars.right,
                    /* bottom = */ if (paddingBottom) systemBars.bottom else 0,
                )
                return WindowInsetsCompat.CONSUMED
            }
        })
    }
}

// ===============================================================================
// ADVANCED FEATURES DEMO - Các tính năng nâng cao cho demo
// ===============================================================================

/**
 * Ad Type Constants - Hằng số cho các loại quảng cáo
 */
object AdType {
    const val BANNER = "banner"
    const val INTERSTITIAL = "interstitial"
    const val APP_OPEN = "app_open"
}

/**
 * Demo Revenue Tracking - Theo dõi doanh thu quảng cáo
 */
object AdRevenueDemo {
    private var sessionImpressions = 0
    private var sessionRevenue = 0.0
    private var sessionClicks = 0

    // eCPM ước tính cho từng loại quảng cáo (USD)
    private const val BANNER_ECPM = 0.5
    private const val INTERSTITIAL_ECPM = 2.0
    private const val APP_OPEN_ECPM = 1.5

    fun trackImpression(adType: String) {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        sessionImpressions++
        val revenue = when (adType.lowercase()) {
            AdType.BANNER -> BANNER_ECPM / 1000
            AdType.INTERSTITIAL -> INTERSTITIAL_ECPM / 1000
            AdType.APP_OPEN -> APP_OPEN_ECPM / 1000
            else -> 0.1 / 1000
        }
        sessionRevenue += revenue
        SafeLogger.d("AdRevenueDemo", "💰 $adType impression: +$${String.format("%.4f", revenue)}")
    }

    fun trackClick(adType: String) {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        sessionClicks++
        val clickRevenue = when (adType.lowercase()) {
            AdType.BANNER -> BANNER_ECPM / 100  // Click = 10x impression
            AdType.INTERSTITIAL -> INTERSTITIAL_ECPM / 100
            AdType.APP_OPEN -> APP_OPEN_ECPM / 100
            else -> 0.1 / 100
        }
        sessionRevenue += clickRevenue
        SafeLogger.d("AdRevenueDemo", "🎯 $adType click: +$${String.format("%.4f", clickRevenue)}")
    }

    fun getRevenueStats(): String {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return "Demo features disabled in production"

        val ctr = if (sessionImpressions > 0) (sessionClicks.toDouble() / sessionImpressions * 100) else 0.0
        val ecpm = if (sessionImpressions > 0) (sessionRevenue / sessionImpressions * 1000) else 0.0

        return """
            📊 REVENUE STATISTICS
            💰 Session Revenue: $${String.format("%.4f", sessionRevenue)}
            👁️ Impressions: $sessionImpressions
            🎯 Clicks: $sessionClicks
            📈 CTR: ${String.format("%.2f", ctr)}%
            💹 eCPM: $${String.format("%.4f", ecpm)}
        """.trimIndent()
    }

    fun resetStats() {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        sessionImpressions = 0
        sessionRevenue = 0.0
        sessionClicks = 0
    }
}

/**
 * Demo User Segmentation - Phân khúc người dùng
 */
object UserSegmentationDemo {
    enum class UserSegment {
        NEW_USER, ACTIVE_USER, POWER_USER, CHURNED_USER
    }

    fun getCurrentSegment(): UserSegment {
        // Demo logic dựa trên thời gian hiện tại
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..10 -> UserSegment.NEW_USER
            in 11..16 -> UserSegment.ACTIVE_USER
            in 17..21 -> UserSegment.POWER_USER
            else -> UserSegment.CHURNED_USER
        }
    }

    fun getSegmentInfo(): String {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return "Demo features disabled in production"

        val segment = getCurrentSegment()
        val description = when (segment) {
            UserSegment.NEW_USER -> "Người dùng mới (< 7 ngày)"
            UserSegment.ACTIVE_USER -> "Người dùng tích cực (7-30 ngày)"
            UserSegment.POWER_USER -> "Người dùng trung thành (> 30 ngày)"
            UserSegment.CHURNED_USER -> "Người dùng không hoạt động"
        }

        val adLimit = when (segment) {
            UserSegment.NEW_USER -> "Ít quảng cáo (1 interstitial/giờ)"
            UserSegment.ACTIVE_USER -> "Bình thường (3 interstitial/giờ)"
            UserSegment.POWER_USER -> "Nhiều hơn (5 interstitial/giờ)"
            UserSegment.CHURNED_USER -> "Giảm thiểu (2 interstitial/giờ)"
        }

        return """
            👤 USER SEGMENTATION
            🏷️ Segment: $segment
            📝 Mô tả: $description
            📊 Giới hạn quảng cáo: $adLimit
            ⏰ Thời gian hiện tại: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
        """.trimIndent()
    }
}

/**
 * Demo Ad Frequency Capping - Giới hạn tần suất quảng cáo
 */
object AdFrequencyDemo {
    private val adHistory = mutableMapOf<String, MutableList<Long>>()

    // Giới hạn trong 1 giờ
    private const val MAX_BANNER_PER_HOUR = 10
    private const val MAX_INTERSTITIAL_PER_HOUR = 3
    private const val MAX_APP_OPEN_PER_HOUR = 2

    fun recordAdShown(adType: String) {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        val now = System.currentTimeMillis()
        val history = adHistory.getOrPut(adType) { mutableListOf() }
        history.add(now)

        // Xóa entries cũ hơn 1 giờ
        history.removeAll { (now - it) > 60 * 60 * 1000 }
    }

    fun getFrequencyStats(): String {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return "Demo features disabled in production"

        val now = System.currentTimeMillis()
        val oneHourAgo = now - 60 * 60 * 1000

        val bannerCount = adHistory[AdType.BANNER]?.count { it > oneHourAgo } ?: 0
        val interstitialCount = adHistory[AdType.INTERSTITIAL]?.count { it > oneHourAgo } ?: 0
        val appOpenCount = adHistory[AdType.APP_OPEN]?.count { it > oneHourAgo } ?: 0

        return """
            📊 AD FREQUENCY (1 giờ qua)
            🖼️ Banner: $bannerCount/$MAX_BANNER_PER_HOUR
            📺 Interstitial: $interstitialCount/$MAX_INTERSTITIAL_PER_HOUR
            🚀 App Open: $appOpenCount/$MAX_APP_OPEN_PER_HOUR

            ⏰ Thời gian kiểm tra: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
        """.trimIndent()
    }

    fun shouldShowAd(adType: String): Boolean {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return true

        val now = System.currentTimeMillis()
        val oneHourAgo = now - 60 * 60 * 1000
        val recentCount = adHistory[adType]?.count { it > oneHourAgo } ?: 0

        return when (adType.lowercase()) {
            AdType.BANNER -> recentCount < MAX_BANNER_PER_HOUR
            AdType.INTERSTITIAL -> recentCount < MAX_INTERSTITIAL_PER_HOUR
            AdType.APP_OPEN -> recentCount < MAX_APP_OPEN_PER_HOUR
            else -> true
        }
    }

    fun resetAllHistory() {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        adHistory.clear()
    }
}

/**
 * Demo Security & Anti-Fraud - Bảo vệ chống gian lận
 */
object AdSecurityDemo {
    private var totalClicks = 0
    private var totalImpressions = 0
    private var violationCount = 0
    private var lastClickTime = 0L

    fun validateAdClick(): Boolean {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return true

        val now = System.currentTimeMillis()
        val timeBetweenClicks = now - lastClickTime

        totalClicks++

        // Phát hiện click spam (< 1 giây giữa các click)
        if (timeBetweenClicks < 1000 && lastClickTime > 0) {
            violationCount++
            SafeLogger.w("AdSecurityDemo", "🚨 Phát hiện click spam! Thời gian: ${timeBetweenClicks}ms")
            return false
        }

        lastClickTime = now
        return true
    }

    fun validateAdImpression(): Boolean {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return true

        totalImpressions++
        return true
    }

    fun getSecurityStats(): String {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return "Demo features disabled in production"

        val ctr = if (totalImpressions > 0) (totalClicks.toDouble() / totalImpressions * 100) else 0.0
        val isSuspicious = ctr > 10.0 || violationCount > 3 // CTR > 10% hoặc > 3 vi phạm

        return """
            🛡️ SECURITY STATS
            ${if (isSuspicious) "🚨 NGHI NGỜ" else "✅ AN TOÀN"}

            ⚠️ Vi phạm: $violationCount
            🎯 Total Clicks: $totalClicks
            👁️ Total Impressions: $totalImpressions
            📈 CTR: ${String.format("%.1f", ctr)}%

            📋 Tiêu chí:
            • CTR bình thường: < 10%
            • Vi phạm cho phép: ≤ 3
        """.trimIndent()
    }

    fun resetSecurityStats() {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        totalClicks = 0
        totalImpressions = 0
        violationCount = 0
        lastClickTime = 0L
    }
}

/**
 * Demo Smart Preloading - Preload quảng cáo thông minh
 */
object AdPreloadDemo {
    private var preloadedAds = 0
    private val maxAds = 3
    private var isPreloading = false
    private var lastPreloadTime = 0L

    fun startPreloading() {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return
        if (isPreloading) return

        isPreloading = true
        lastPreloadTime = System.currentTimeMillis()

        // Simulate preloading
        Thread {
            Thread.sleep(2000) // Giả lập thời gian load 2s
            preloadedAds = kotlin.random.Random.nextInt(1, maxAds + 1)
            isPreloading = false
            SafeLogger.d("AdPreloadDemo", "🚀 Preloaded $preloadedAds ads")
        }.start()
    }

    fun getPreloadStats(): String {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return "Demo features disabled in production"

        val timeSinceLastPreload = if (lastPreloadTime > 0) {
            (System.currentTimeMillis() - lastPreloadTime) / 1000
        } else 0

        return """
            🚀 SMART PRELOADING
            📦 Ads sẵn sàng: $preloadedAds/$maxAds
            ${if (isPreloading) "🔄 Đang preload..." else "✅ Hoàn thành"}

            ⏰ Lần cuối preload: ${if (lastPreloadTime > 0) "${timeSinceLastPreload}s trước" else "Chưa bao giờ"}

            💡 Lợi ích:
            • Giảm thời gian chờ
            • Tăng fill rate
            • UX mượt mà hơn
        """.trimIndent()
    }

    fun clearPreloadedAds() {
        if (!ProductionConfig.ENABLE_DEMO_FEATURES) return

        preloadedAds = 0
        lastPreloadTime = 0L
    }
}
