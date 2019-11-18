package com.violas.wallet.ui.tokenInfo

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.ui.collection.CollectionActivity
import com.violas.wallet.ui.transfer.TransferActivity
import com.violas.wallet.utils.ClipboardUtils
import com.violas.wallet.utils.convertAmountToDisplayUnit
import kotlinx.android.synthetic.main.activity_token_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TokenInfoActivity : BaseAppActivity() {
    companion object {
        private const val EXT_TOKEN_ID = "1"
        fun start(fragment: Fragment, tokenId: Long = 0, responseCode: Int) {
            val intent = Intent(fragment.activity, TokenInfoActivity::class.java)
                .apply {
                    putExtra(EXT_TOKEN_ID, tokenId)
                }
            fragment.startActivityForResult(intent, responseCode)
        }
    }

    override fun getLayoutResId(): Int {
        return R.layout.activity_token_info
    }

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    private var mTokenDo: TokenDo? = null
    private var mAccountDO: AccountDO? = null

    private val mTokenManager by lazy {
        TokenManager()
    }

    private val mAccountManager by lazy {
        AccountManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {
            val tokenId = intent.getLongExtra(EXT_TOKEN_ID, -1)
            val tokenDo = mTokenManager.findTokenById(tokenId)
            if (tokenDo == null) {
                showToast(getString(R.string.hint_unknown_error))
                finish()
                return@launch
            }
            mTokenDo = tokenDo
            try {
                mAccountDO = mAccountManager.getAccountById(tokenDo.account_id)
            } catch (e: Exception) {
                showToast(getString(R.string.hint_unknown_error))
                finish()
            }
            withContext(Dispatchers.Main) {
                title = mTokenDo!!.name
                refreshAccountData()
                tvUnit.text = mTokenDo!!.name
                tvAddress.text = mAccountDO!!.address
                ivCopy.setOnClickListener {
                    ClipboardUtils.copy(applicationContext, mAccountDO!!.address)
                }
            }
        }
        btnTransfer.setOnClickListener {
            launch(Dispatchers.IO) {
                mTokenDo?.apply {
                    TransferActivity.start(
                        this@TokenInfoActivity,
                        account_id,
                        "",
                        0,
                        true,
                        id!!
                    )
                }
            }
        }
        btnCollection.setOnClickListener {
            launch(Dispatchers.IO) {
                mAccountDO?.apply {
                    mTokenDo?.id?.let {
                        CollectionActivity.start(this@TokenInfoActivity, id, true, it)
                    }
                }
            }
        }
    }

    private fun refreshAccountData() {
        launch(Dispatchers.IO) {
            val currentAccount = mAccountManager.currentAccount()
            withContext(Dispatchers.Main) {
                mTokenDo?.apply {
                    mTokenManager.getTokenBalance(currentAccount.address, tokenAddress) {
                        try {
                            setAmount(it)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun setAmount(currentAccount: Long) {
        val convertAmountToDisplayUnit =
            convertAmountToDisplayUnit(currentAccount, CoinTypes.VToken)
        tvAmount.text = "${convertAmountToDisplayUnit.first}"
    }
}
