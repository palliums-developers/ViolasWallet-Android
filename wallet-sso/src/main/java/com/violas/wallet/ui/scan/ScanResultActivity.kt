package com.violas.wallet.ui.scan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import kotlinx.android.synthetic.main.activity_scan_result.*

class ScanResultActivity : BaseAppActivity() {
    companion object {
        private const val EXT_MSG = "a"
        fun start(context: Context, msg: String) {
            Intent(context, ScanResultActivity::class.java).apply {
                putExtra(EXT_MSG, msg)
            }.start(context)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_scan_result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_scan_result)
        tvResult.text = intent.getStringExtra(EXT_MSG)
    }
}
