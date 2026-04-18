package com.saigonphantomlabs

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.google.android.gms.ads.MobileAds
import com.saigonphantomlabs.chess.BuildConfig
import com.roy.sdkadbmob.AdManager
import com.roy.sdkadbmob.AdSafetyConfig
import com.roy.sdkadbmob.AdSdkConfig
import java.lang.ref.WeakReference

class MyApplication : Application() {

    /**
     * Own activity tracker — used by the custom App Open lifecycle observer below.
     * Mirrors what SDK does internally, but gives us control over which activities to skip.
     */
    private var currentActivity: WeakReference<Activity>? = null

    override fun onCreate() {
        super.onCreate()
        setupAd()
    }

    private fun setupAd() {
        val adConfig = AdSdkConfig(
            isEnableAdmob          = BuildConfig.IS_ENABLE_ADMOB,
            isDebug                = BuildConfig.DEBUG,
            admobBannerId          = BuildConfig.ADMOB_BANNER_ID,
            admobInterstitialId    = BuildConfig.ADMOB_INTERSTITIAL_ID,
            admobAppOpenId         = BuildConfig.ADMOB_APP_OPEN_ID,
            applovinBannerId       = BuildConfig.APPLOVIN_BANNER_ID,
            applovinInterstitialId = BuildConfig.APPLOVIN_INTERSTITIAL_ID,
            applovinAppOpenId      = BuildConfig.APPLOVIN_APP_OPEN_ID
        )

        // Gắn Config ở Main Thread trước để SplashActivity không crash khi đọc isEnableAdmob
        AdManager.setConfig(adConfig)

        // Khởi tạo AdSafetyConfig sớm để session clock đúng từ lúc app launch
        AdManager.earlyInit(this)

        if (BuildConfig.IS_ENABLE_ADMOB) {
            Log.d("roy93~", "MyApplication: AdMob mode, initializing MobileAds")
            MobileAds.initialize(this) {
                initAdManager(adConfig)
            }
        } else {
            Log.d("roy93~", "MyApplication: AppLovin mode, initializing AppLovinSdk")
            val initConfig = AppLovinSdkInitializationConfiguration.builder(
                BuildConfig.APPLOVIN_SDK_KEY,
                this
            )
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .build()
            AppLovinSdk.getInstance(this).initialize(initConfig) {
                initAdManager(adConfig)
            }
        }
    }

    private fun initAdManager(adConfig: AdSdkConfig) {
        AdManager.init(this@MyApplication, adConfig) { success, gaidCurrent ->
            Log.d("roy93~", "AdManager init success=$success, gaidCurrent=$gaidCurrent")
            if (success) {
                // Must run on Main Thread — ProcessLifecycleOwner.addObserver requires it
                Handler(Looper.getMainLooper()).post {
                    // ⚠️ We intentionally do NOT call AdManager.registerAppOpenAdLifecycle()
                    // because that observer cannot be customized to skip ChessBoardActivity.
                    // Instead we register our own equivalent below.
                    registerCustomAppOpenLifecycle()
                }
            }
        }
    }

    /**
     * Custom replacement for AdManager.registerAppOpenAdLifecycle().
     *
     * Identical behaviour to the SDK's built-in version, with one extra policy rule:
     *   • "ChessBoardActivity" is blacklisted → App Open Ad is NEVER shown while
     *     the user is actively playing chess, preventing gameplay interruption.
     *
     * Policy basis:
     *   - AppLovin MAX: "App Open ads must not interrupt ongoing game sessions."
     *   - Google AdMob: "Do not place ads where they interfere with app functionality."
     */
    private fun registerCustomAppOpenLifecycle() {
        // ── Part 1: track current activity for SDK internals (interstitial context etc.) ──
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, b: Bundle?) {
                currentActivity = WeakReference(a)
                AdManager.setCurrentActivity(a)
            }
            override fun onActivityStarted(a: Activity) {
                currentActivity = WeakReference(a)
                AdManager.setCurrentActivity(a)
            }
            override fun onActivityResumed(a: Activity) {
                currentActivity = WeakReference(a)
                AdManager.setCurrentActivity(a)
            }
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        // ── Part 2: App Open Ad on resume from background ──
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                // AdSafetyConfig guards: cold-start skip, 5s background minimum, rapid-resume detection
                if (!AdSafetyConfig.canShowAppOpenOnResume()) return

                val activity = currentActivity?.get() ?: return
                val activityName = activity.javaClass.simpleName

                // ✅ Policy: skip on SplashActivity (initSplashScreen handles it separately)
                // ✅ Policy: skip on ChessBoardActivity (must not interrupt gameplay)
                if (activityName == "SplashActivity" || activityName == "ChessBoardActivity") {
                    Log.d("roy93~", "AppOpen ⏭️ skipped — blacklisted activity: $activityName")
                    return
                }

                Log.d("roy93~", "AppOpen 🔄 attempting to show on $activityName")
                // showAppOpenAd handles "ad not ready" gracefully (calls onAdDismiss immediately)
                AdManager.showAppOpenAd(
                    activity = activity,
                    onAdDismiss = {
                        Log.d("roy93~", "AppOpen ✅ dismissed — preloading next ad")
                        AdManager.loadAppOpenAd(activity.applicationContext) { loaded ->
                            Log.d("roy93~", "AppOpen preload result=$loaded")
                        }
                    }
                )
            }

            override fun onStop(owner: LifecycleOwner) {
                // Record background time so AdSafetyConfig can enforce the 5s minimum
                AdSafetyConfig.recordAppWentBackground()
                Log.d("roy93~", "AppOpen ⏸️ app went to background")
            }
        })

        Log.d("roy93~", "registerCustomAppOpenLifecycle ✅ registered — ChessBoardActivity blacklisted")
    }
}

