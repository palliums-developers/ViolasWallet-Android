package com.violas.wallet.ui.identity

import android.os.Bundle
import android.view.View
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity

import kotlinx.android.synthetic.main.activity_identity.*

class IdentityActivity : BaseActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_identity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("身份管理")
    }

}
