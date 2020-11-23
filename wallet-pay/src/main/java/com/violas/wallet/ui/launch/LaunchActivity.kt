package com.violas.wallet.ui.launch

import android.graphics.Color
import android.os.Bundle
import com.palliums.content.App
import com.palliums.utils.setSystemBar
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.common.KEY_MSG_TXID
import com.violas.wallet.common.KEY_MSG_TYPE
import com.violas.wallet.ui.main.MainActivity
import com.violas.wallet.ui.message.MessageCenterActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LaunchActivity : BaseAppActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_launch
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_NOT_TITLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val hasLaunch = App.existsActivity(MainActivity::class.java)

        val msgType = intent?.extras?.getString(KEY_MSG_TYPE)
        val msgTxid = intent?.extras?.getString(KEY_MSG_TXID)
        val fromNotification = !msgType.isNullOrBlank() && !msgTxid.isNullOrBlank()

        if (hasLaunch && fromNotification) {
            // 应用在后台收到消息推送时，App已启动，直接跳转到消息中心页面
            MessageCenterActivity.start(this, intent?.extras)
            finish()
            return
        }


        window.setSystemBar(
            layoutToStatusBar = true,
            layoutToNavigationBar = true,
            lightModeStatusBar = false,
            lightModeNavigationBar = false,
            statusBarColorBelowM = Color.TRANSPARENT,
            navigationBarColorBelowO = Color.TRANSPARENT
        )
        super.onCreate(savedInstanceState)

        launch(Dispatchers.Main) {
            delay(1000)
            if (fromNotification) {
                // 点击消息通知栏进入App时，直接进入消息中心页面，返回时进入主页
                MessageCenterActivity.start(this@LaunchActivity, intent?.extras)
            } else {
                MainActivity.start(this@LaunchActivity)
            }
            finish()
        }
    }
}
