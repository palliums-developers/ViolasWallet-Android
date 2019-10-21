package com.violas.wallet.ui.backup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.common.INTENT_KET_BACKUP_MNEMONIC_MODE
import com.violas.wallet.common.INTENT_KET_WALLET_NAME
import com.violas.wallet.common.INTENT_KET_WALLET_PWD
import com.violas.wallet.common.INTENT_KET_WALLET_SYSTEM
import com.violas.wallet.ui.wallet.WalletSystem
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_backup_prompt.*

/**
 * Created by elephant on 2019-10-21 13:58.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份提示页面
 */
class BackupPromptActivity : BaseBackupMnemonicActivity() {

    companion object {

        @JvmStatic
        fun startFromIdentityWallet(context: Context) {
            Intent(context, BackupPromptActivity::class.java)
                .apply {
                    putExtra(INTENT_KET_BACKUP_MNEMONIC_MODE, BackupMnemonicMode.IDENTITY_WALLET)
                }
                .start(context)
        }

        @JvmStatic
        fun startFromCreateWallet(
            context: Context,
            @WalletSystem
            walletSystem: Int,
            walletName: String,
            walletPwd: String
        ) {
            Intent(context, BackupPromptActivity::class.java)
                .apply {
                    putExtra(INTENT_KET_BACKUP_MNEMONIC_MODE, BackupMnemonicMode.CREATE_WALLET)
                    putExtra(INTENT_KET_WALLET_SYSTEM, walletSystem)
                    putExtra(INTENT_KET_WALLET_NAME, walletName)
                    putExtra(INTENT_KET_WALLET_PWD, walletPwd)
                }
                .start(context)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_backup_prompt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.backup_mnemonic_prompt_title)
        setTitleRightText(R.string.backup_mnemonic_prompt_menu)

        tv_backup_prompt_next_step.setOnClickListener(this)
    }

    override fun onTitleRightViewClick() {

    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_backup_prompt_next_step -> {
                Intent(this, ShowMnemonicActivity::class.java)
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