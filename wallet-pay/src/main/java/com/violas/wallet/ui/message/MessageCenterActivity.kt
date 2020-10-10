package com.violas.wallet.ui.message

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.content.App
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.main.MainActivity

/**
 * Created by elephant on 2020/10/10 17:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MessageCenterActivity : BaseAppActivity() {

    companion object {

        fun start(context: Context, extras: Bundle? = null) {
            Intent(context, MessageCenterActivity::class.java)
                .apply {
                    if (extras != null) {
                        putExtras(extras)
                    }
                }
                .start(context)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_message_center
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.message_center)

        // TODO 同步消息，具体的消息处理逻辑
    }

    override fun onBackPressedSupport() {
        // 首页未启动时，返回直接进入首页
        if (!App.existsActivity(MainActivity::class.java)) {
            MainActivity.start(this)
        }
        super.onBackPressedSupport()
    }
}