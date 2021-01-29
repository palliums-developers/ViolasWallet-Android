package com.violas.wallet

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lxj.xpopup.XPopup
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.CrashesListener
import com.palliums.content.App
import com.palliums.extensions.createNotificationChannel
import com.palliums.extensions.getNotificationManager
import com.palliums.utils.getColorByAttrId
import com.violas.wallet.ui.changeLanguage.MultiLanguageUtility
import com.violas.wallet.viewModel.MessageViewModel
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.WalletConnectViewModel


class PayApp : App() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            System.setProperty("kotlinx.coroutines.debug", "on")
        }
        super.onCreate()

        handlerError()
        handlerAppCenter()
        initXPopup()

        initAccountAndAssets()

        // TODO 暂时屏蔽消息推送通知
        //initMessagePush()
        //initMessageNotification()

        initWalletConnect()
    }

    private fun initAccountAndAssets() {
        WalletAppViewModel.getViewModelInstance()
    }

    private fun initMessagePush() {
        MessageViewModel.getInstance()
    }

    private fun initWalletConnect() {
        // 暂时不考虑多进程
        WalletConnectViewModel.getViewModelInstance(this)
    }

    private fun initXPopup() {
        XPopup.setPrimaryColor(getColorByAttrId(R.attr.colorPrimary, this))
        XPopup.setShadowBgColor(Color.parseColor("#98000000"))
    }

    private fun initMessageNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = getNotificationManager()
        createNotificationChannel(
            getString(R.string.notification_channel_id_transaction),
            getString(R.string.notification_channel_name_transaction),
            NotificationManager.IMPORTANCE_HIGH,
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE,
            notificationManager = notificationManager
        )
        createNotificationChannel(
            getString(R.string.notification_channel_id_events),
            getString(R.string.notification_channel_name_events),
            NotificationManager.IMPORTANCE_DEFAULT,
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC,
            notificationManager = notificationManager
        )
        createNotificationChannel(
            getString(R.string.notification_channel_id_update),
            getString(R.string.notification_channel_name_update),
            NotificationManager.IMPORTANCE_LOW,
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC,
            notificationManager = notificationManager
        )
        createNotificationChannel(
            getString(R.string.notification_channel_id_default),
            getString(R.string.notification_channel_name_default),
            NotificationManager.IMPORTANCE_LOW,
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC,
            notificationManager = notificationManager
        )
    }

    private fun handlerAppCenter() {
        if (!AppCenter.isConfigured()) {
            AppCenter.start(
                this,
                "3ea4dea9-9be1-4423-ada7-99f5ca24457a",
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
}