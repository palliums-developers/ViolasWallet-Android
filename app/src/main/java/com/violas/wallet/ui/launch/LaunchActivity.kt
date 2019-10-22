package com.violas.wallet.ui.launch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.ui.identity.IdentityActivity
import com.violas.wallet.ui.main.MainActivity
import kotlinx.coroutines.*

class LaunchActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        launch(Dispatchers.IO) {
            delay(2000)
            if (AccountManager().existsWalletAccount()) {
                withContext(Dispatchers.Main) {
                    finish()
                    MainActivity.start(this@LaunchActivity)
                }
            } else {
                withContext(Dispatchers.Main) {
                    finish()
                    IdentityActivity.start(this@LaunchActivity)
                }
            }
        }
    }
}
