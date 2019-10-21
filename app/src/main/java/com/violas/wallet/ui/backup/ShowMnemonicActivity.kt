package com.violas.wallet.ui.backup

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.common.INTENT_KET_BACKUP_MNEMONIC_MODE
import com.violas.wallet.common.INTENT_KET_WALLET_NAME
import com.violas.wallet.common.INTENT_KET_WALLET_PWD
import com.violas.wallet.common.INTENT_KET_WALLET_SYSTEM
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_show_mnemonic.*

/**
 * Created by elephant on 2019-10-21 15:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词页面
 */
class ShowMnemonicActivity : BaseBackupMnemonicActivity() {

    override fun getLayoutResId(): Int {
        return R.layout.activity_show_mnemonic
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.show_mnemonic_title)

        tv_show_mnemonic_next_step.setOnClickListener(this)
    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_show_mnemonic_next_step -> {
                Intent(this, ConfirmMnemonicActivity::class.java)
                    .apply {
                        putExtra(INTENT_KET_BACKUP_MNEMONIC_MODE, backupMnemonicMode)
                        if (walletSystem != -1) {
                            putExtra(INTENT_KET_WALLET_SYSTEM, walletSystem)
                        }
                        if (walletName != null) {
                            putExtra(INTENT_KET_WALLET_NAME, walletName)
                        }
                        if (walletPwd != null) {
                            putExtra(INTENT_KET_WALLET_PWD, walletPwd)
                        }
                    }
                    .start(this)
            }
        }
    }
}