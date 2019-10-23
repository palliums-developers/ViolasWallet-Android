package com.violas.wallet.ui.backup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.base.BaseActivity

/**
 * Created by elephant on 2019-10-21 17:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词相关页面基类
 */
abstract class BaseBackupMnemonicActivity : BaseActivity() {

    companion object {
        const val INTENT_KET_MNEMONIC = "INTENT_KET_MNEMONIC"
        const val INTENT_KET_DELAYABLE = "INTENT_KET_DELAYABLE"
    }

    var mnemonicWords: ArrayList<String>? = null
    var delayable: Boolean = false // 备份可延迟

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent != null) {
            mnemonicWords = intent.getStringArrayListExtra(INTENT_KET_MNEMONIC)
            delayable = intent.getBooleanExtra(INTENT_KET_DELAYABLE, false)
        }

        if (mnemonicWords == null) {
            /*setResult(Activity.RESULT_CANCELED)
            finish()
            return*/
        }
    }

    protected fun getBackupIntent(cls: Class<out Activity>): Intent {
        return Intent(this, cls).apply {
            putStringArrayListExtra(INTENT_KET_MNEMONIC, mnemonicWords)
            putExtra(INTENT_KET_DELAYABLE, delayable)
        }
    }
}