package com.violas.wallet

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.CrashesListener
import com.palliums.content.App
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.viewModel.WalletConnectViewModel


class PayApp : App() {
    override fun onCreate() {
        System.setProperty("kotlinx.coroutines.debug", "on")
        super.onCreate()
        handlerAppCenter()
        resetWalletConnect()
    }

    private fun handlerAppCenter() {
        if (!AppCenter.isConfigured()) {
            AppCenter.start(
                this,
                "90ae9fd6-30f2-4779-bd8c-4b0c63f1aa6a",
                Analytics::class.java, Crashes::class.java
            )
            val customListener: CrashesListener =
                object : AbstractCrashesListener() {
                    override fun shouldAwaitUserConfirmation(): Boolean {
                        return true
                    }
                }
            Crashes.setListener(customListener)
            Crashes.notifyUserConfirmation(Crashes.SEND)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        MultiLanguageUtility.init(newBase)
        MultiLanguageUtility.attachBaseContext(newBase)
    }

    fun resetWalletConnect() {
        // 暂时不考虑多进程
        WalletConnectViewModel.getViewModelInstance(this)
    }
}