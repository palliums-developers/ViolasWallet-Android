package com.violas.wallet

import android.content.Context
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.palliums.content.App
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility

class SSOApp : App() {
    override fun onCreate() {
        super.onCreate()
        if (!AppCenter.isConfigured()) {
            AppCenter.start(
                this,
                "90ae9fd6-30f2-4779-bd8c-4b0c63f1aa6a",
                Analytics::class.java, Crashes::class.java
            )
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        MultiLanguageUtility.init(newBase)
        MultiLanguageUtility.attachBaseContext(newBase)
    }
}