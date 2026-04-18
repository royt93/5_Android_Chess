package com.saigonphantomlabs

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.saigonphantomlabs.language.LanguageManager

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lang = LanguageManager.getLanguage(newBase)
        val context = LanguageManager.applyLocale(newBase, lang)
        super.attachBaseContext(context)
    }
}
