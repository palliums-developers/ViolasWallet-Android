package com.violas.wallet.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.AccountNotExistsException
import com.violas.wallet.event.ChangeAccountNameEvent
import com.violas.wallet.repository.DataRepository
import kotlinx.android.synthetic.main.activity_account_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class AccountInfoActivity : BaseActivity() {
    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        fun start(context: Context, accountId: Long) {
            val intent = Intent(context, AccountInfoActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivity(intent)
        }
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_account_info
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("钱包详情")

        launch(Dispatchers.IO) {
            try {
                val account =
                    mAccountManager.getAccountById(intent.getLongExtra(EXT_ACCOUNT_ID, 1))
                withContext(Dispatchers.Main) {
                    editName.hint = account.walletNickname
                }
            } catch (e: AccountNotExistsException) {
                e.printStackTrace()
                finish()
            }
        }

        btnConfirm.setOnClickListener {
            val name = editName.text.toString().trim()
            if (name.isEmpty()) {
                showToast("请输入新的钱包名称")
                return@setOnClickListener
            }
            launch(Dispatchers.IO) {
                try {
                    val account =
                        mAccountManager.getAccountById(intent.getLongExtra(EXT_ACCOUNT_ID, 1))
                    account.walletNickname = name
                    DataRepository.getAccountStorage().update(account)
                    EventBus.getDefault().post(ChangeAccountNameEvent())
                    showToast("修改成功")
                    finish()
                } catch (e: AccountNotExistsException) {
                    e.printStackTrace()
                    finish()
                }
            }
        }
    }
}
