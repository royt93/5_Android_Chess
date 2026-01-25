package com.saigonphantomlabs

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.saigonphantomlabs.chess.BuildConfig
import com.saigonphantomlabs.sdkadbmob.AdMobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setupAdmob()
    }

    private fun setupAdmob() {
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MyApplication) {}
            AdMobManager.init(this@MyApplication) { success, gaidCurrent ->
                Log.d("roy93~", "AdMobManager init success $success, gaidCurrent $gaidCurrent")
            }
        }
    }
}
