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
        handlerError()
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

    private fun handlerError() {
        // 捕获主线程 catch 防止闪退
        // 不能防止 Activity onCreate 主线程报错，这样会因为 Activity 生命周期没走完而崩溃。
        Handler().post(Runnable {
            while (true) {
                try {
                    Looper.loop()
                } catch (e: Throwable) {
                    // 异常发给 AppCenter
                    Log.e("Main Thread Catch", "如果软件 ANR 请检查报错信息是否在 Activity 的 onCreate() 方法。")
                    Crashes.trackError(e)
                    e.printStackTrace()
                }
            }
        })
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