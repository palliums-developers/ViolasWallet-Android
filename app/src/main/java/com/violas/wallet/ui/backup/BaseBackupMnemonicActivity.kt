package com.violas.wallet.ui.backup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.violas.wallet.base.BaseActivity

/**
 * Created by elephant on 2019-10-21 17:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 备份助记词相关页面基类
 */
abstract class BaseBackupMnemonicActivity : BaseActivity() {

    companion object {
        const val INTENT_KET_MNEMONIC_WORDS = "INTENT_KET_MNEMONIC_WORDS"
        const val INTENT_KET_MNEMONIC_FROM = "INTENT_KET_MNEMONIC_FROM"

        const val BACKUP_REQUEST_CODE = 100
    }

    var mnemonicWords: ArrayList<String>? = null
    var mnemonicFrom: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent != null) {
            mnemonicWords = intent.getStringArrayListExtra(INTENT_KET_MNEMONIC_WORDS)
            mnemonicFrom = intent.getIntExtra(INTENT_KET_MNEMONIC_FROM, -1)
        }

        if (mnemonicWords == null || mnemonicFrom == -1) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        Log.e(this.javaClass.simpleName, "mnemonic words => ${mnemonicWords!!.joinToString(" ")}")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == BACKUP_REQUEST_CODE) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    protected fun getBackupIntent(cls: Class<out Activity>): Intent {
        return Intent(this, cls).apply {
            putStringArrayListExtra(INTENT_KET_MNEMONIC_WORDS, mnemonicWords)
            putExtra(INTENT_KET_MNEMONIC_FROM, mnemonicFrom)
        }
    }
}