package com.violas.wallet.ui.identity.importIdentity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.MnemonicException
import kotlinx.android.synthetic.main.activity_import_identity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportIdentityActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ImportIdentityActivity::class.java))
        }
    }

    override fun getLayoutResId() = R.layout.activity_import_identity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("导入钱包")

        btnConfirm.setOnClickListener {
            val mnemonic = editMnemonicWord.text.toString().trim()
            val walletName = editName.text.toString().trim()
            val password = editPassword.text.toString().trim().toByteArray()
            showProgress()
            launch(Dispatchers.IO) {
                try {
                    val wordList = mnemonic.trim().split(" ")
                        .map { it.trim() }
                        .toList()
                    AccountManager().importIdentity(
                        this@ImportIdentityActivity,
                        wordList,
                        walletName,
                        password
                    )
                    withContext(Dispatchers.Main) {
                        dismissProgress()
                        showToast("成功")
                    }
                } catch (e: MnemonicException) {
                    showToast("助记词错误")
                }
            }
        }
    }

}
