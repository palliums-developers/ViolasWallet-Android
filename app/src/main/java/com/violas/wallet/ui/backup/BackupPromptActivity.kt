package com.violas.wallet.ui.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_backup_prompt.*
import org.palliums.libracore.mnemonic.Mnemonic

/**
 * Created by elephant on 2019-10-21 13:58.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份提示页面
 */
class BackupPromptActivity : BaseBackupMnemonicActivity() {

    companion object {

        @JvmStatic
        fun start(
            context: Context,
            mnemonicWords: ArrayList<String>,
            delayable: Boolean = false
        ) {
            Intent(context, BackupPromptActivity::class.java)
                .apply {
                    putStringArrayListExtra(INTENT_KET_MNEMONIC, mnemonicWords)
                    putExtra(INTENT_KET_DELAYABLE, delayable)
                }
                .start(context)
        }

        @JvmStatic
        fun start(
            activity: Activity,
            requestCode: Int,
            mnemonicWords: ArrayList<String>,
            delayable: Boolean = false
        ) {
            Intent(activity, BackupPromptActivity::class.java)
                .apply {
                    putStringArrayListExtra(INTENT_KET_MNEMONIC, mnemonicWords)
                    putExtra(INTENT_KET_DELAYABLE, delayable)
                }
                .start(activity, requestCode)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_backup_prompt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.backup_mnemonic_prompt_title)
        if (delayable) {
            setTitleRightText(R.string.backup_mnemonic_prompt_menu)
        }

        tv_backup_prompt_next_step.setOnClickListener(this)

        mnemonicWords = Mnemonic.English().generate()
        Log.e("BackupPromptActivity","mnemonic words: ${mnemonicWords!!.joinToString(" ")}")
    }

    override fun onTitleRightViewClick() {
        // TODO 跳转到主页面
        finish()
    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_backup_prompt_next_step -> {
                getBackupIntent(ShowMnemonicActivity::class.java).start(this)
            }
        }
    }
}