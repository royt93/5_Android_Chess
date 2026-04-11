package com.saigonphantomlabs

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.saigonphantomlabs.sdkadbmob.AdMobManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupAdmob()
    }

    private fun setupAdmob() {
        // [BUG-05] MobileAds.initialize() must be called on the Main Thread (Google requirement).
        // AdMobManager.init() internally spawns its own Thread for GAID, so no IO dispatcher needed.
        MobileAds.initialize(this@MyApplication) {
            AdMobManager.init(this@MyApplication) { success, gaidCurrent ->
                Log.d("roy93~", "AdMobManager init success $success, gaidCurrent $gaidCurrent")
            }
        }
    }
}
