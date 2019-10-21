package com.violas.wallet.ui.backup

import android.os.Bundle
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.common.INTENT_KET_BACKUP_MNEMONIC_MODE
import com.violas.wallet.common.INTENT_KET_WALLET_NAME
import com.violas.wallet.common.INTENT_KET_WALLET_PWD
import com.violas.wallet.common.INTENT_KET_WALLET_SYSTEM

/**
 * Created by elephant on 2019-10-21 17:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词相关页面基类
 */
abstract class BaseBackupMnemonicActivity : BaseActivity() {

    var backupMnemonicMode: Int = -1
    var walletSystem: Int = -1
    var walletName: String? = null
    var walletPwd: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            backupMnemonicMode =
                savedInstanceState.getInt(INTENT_KET_BACKUP_MNEMONIC_MODE, -1)
        }

        if (backupMnemonicMode == -1) {
            if (intent != null) {
                backupMnemonicMode =
                    intent.getIntExtra(INTENT_KET_BACKUP_MNEMONIC_MODE, -1)
            }

            if (backupMnemonicMode == -1) {
                finish()
                return
            } else if (backupMnemonicMode == BackupMnemonicMode.CREATE_WALLET) {
                walletName = intent?.getStringExtra(INTENT_KET_WALLET_NAME)
                walletPwd = intent?.getStringExtra(INTENT_KET_WALLET_PWD)
            }

        } else if (backupMnemonicMode == BackupMnemonicMode.CREATE_WALLET) {
            walletName = savedInstanceState?.getString(INTENT_KET_WALLET_NAME)
            walletPwd = savedInstanceState?.getString(INTENT_KET_WALLET_PWD)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(INTENT_KET_BACKUP_MNEMONIC_MODE, backupMnemonicMode)
        if (walletSystem != -1) {
            outState.putInt(INTENT_KET_WALLET_SYSTEM, walletSystem)
        }
        walletName?.let {
            outState.putString(INTENT_KET_WALLET_NAME, it)
        }
        walletPwd?.let {
            outState.putString(INTENT_KET_WALLET_PWD, it)
        }
    }
}