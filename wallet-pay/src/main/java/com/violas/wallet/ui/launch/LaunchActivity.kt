package com.violas.wallet.ui.launch

import android.os.Bundle
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LaunchActivity : BaseAppActivity(){

    override fun getLayoutResId(): Int {
        return R.layout.activity_launch
    }

    override fun getTitleStyle(): Int {
        return PAGE_STYLE_NOT_TITLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        super.onCreate(savedInstanceState)

        launch(Dispatchers.Main) {
            delay(1000)
            MainActivity.start(this@LaunchActivity)
            finish()
        }
    }
}
