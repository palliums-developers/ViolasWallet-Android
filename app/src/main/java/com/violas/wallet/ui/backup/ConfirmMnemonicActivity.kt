package com.violas.wallet.ui.backup

import android.os.Bundle
import android.view.View
import com.violas.wallet.R
import kotlinx.android.synthetic.main.activity_confirm_mnemonic.*

/**
 * Created by elephant on 2019-10-21 15:18.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 确认助记词页面
 */
class ConfirmMnemonicActivity : BaseBackupMnemonicActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_confirm_mnemonic
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.confirm_mnemonic_title)

        tv_confirm_mnemonic_complete.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_confirm_mnemonic_complete -> {

            }
        }
    }
}