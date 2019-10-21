package com.violas.wallet.ui.backup

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.utils.start
import kotlinx.android.synthetic.main.activity_backup_prompt.*

/**
 * Created by elephant on 2019-10-21 13:58.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份提示页面
 */
class BackupPromptActivity : BaseActivity() {

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
                Intent(this, ShowMnemonicActivity::class.java).start(this)
            }
        }
    }
}