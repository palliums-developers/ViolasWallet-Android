package com.violas.wallet.ui.launch

import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.identity.IdentityActivity
import com.violas.wallet.ui.main.MainActivity
import kotlinx.coroutines.*

class LaunchActivity : BaseAppActivity(), CoroutineScope by MainScope() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_launch
    }

    override fun getPageStyle(): Int {
        return TITLE_STYLE_DARK_BACKGROUND_NO_TITLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {
            delay(1000)
            if (AccountManager().existsWalletAccount()) {
                withContext(Dispatchers.Main) {
                    MainActivity.start(this@LaunchActivity)
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    IdentityActivity.start(this@LaunchActivity)
                    finish()
                }
            }
        }
    }
}
