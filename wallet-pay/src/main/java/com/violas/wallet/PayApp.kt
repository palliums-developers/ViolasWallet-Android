package com.violas.wallet

import android.content.Context
import com.palliums.content.App
import com.tencent.bugly.Bugly
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.walletconnect.WalletConnect

class PayApp : App() {
    override fun onCreate() {
        super.onCreate()
        Bugly.init(applicationContext, "f4d9c546fa", false)
        resetWalletConnect()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        MultiLanguageUtility.init(newBase)
        MultiLanguageUtility.attachBaseContext(newBase)
    }

    fun resetWalletConnect() {
        // 暂时不考虑多进程
        WalletConnect.getInstance(this).restore()
    }
}