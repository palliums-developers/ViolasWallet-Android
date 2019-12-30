package com.violas.wallet

import android.content.Context
import com.palliums.content.App
import com.tencent.bugly.Bugly
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility

class SSOApp : App() {
    override fun onCreate() {
        super.onCreate()
        Bugly.init(applicationContext, "beaf9fe271", false);
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        MultiLanguageUtility.init(newBase)
        MultiLanguageUtility.attachBaseContext(newBase)
    }
}