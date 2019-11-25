package com.violas.wallet.ui.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.palliums.utils.start
import com.violas.wallet.R
import com.violas.wallet.ui.main.MainActivity
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
        fun start(
            context: Context,
            mnemonicWords: ArrayList<String>,
            @BackupMnemonicFrom
            mnemonicFrom: Int,
            requestCode: Int = -100
        ) {
            val intent = Intent(context, BackupPromptActivity::class.java).apply {
                putStringArrayListExtra(INTENT_KET_MNEMONIC_WORDS, mnemonicWords)
                putExtra(INTENT_KET_MNEMONIC_FROM, mnemonicFrom)
            }

            if (requestCode != -100 && context is Activity) {
                context.startActivityForResult(intent, requestCode)
            } else if (requestCode != -100 && context is Fragment) {
                context.startActivityForResult(intent, requestCode)
            } else {
                context.startActivity(intent)
            }
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_backup_prompt
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.backup_mnemonic_prompt_title)
        if (mnemonicFrom == BackupMnemonicFrom.CREATE_IDENTITY) {
            // 如果是创建身份后进入该页面，则不支持后退
            setTitleRightText(R.string.backup_mnemonic_prompt_menu)
            setTitleRightTextColor(R.color.def_text_btn)
            setTitleLeftViewVisibility(View.GONE)
        }
        tv_backup_prompt_next_step.setOnClickListener(this)
    }

    override fun onTitleRightViewClick() {
        MainActivity.start(this)
        finish()
    }

    override fun onBackPressedSupport() {
        if (mnemonicFrom == BackupMnemonicFrom.CREATE_IDENTITY) {
            // 如果是创建身份后进入该页面，则不支持后退
            return
        }

        super.onBackPressedSupport()
    }

    override fun onViewClick(view: View) {
        when (view) {
            tv_backup_prompt_next_step -> {
                getBackupIntent(ShowMnemonicActivity::class.java)
                    .start(this, BACKUP_REQUEST_CODE)
            }
        }
    }
}