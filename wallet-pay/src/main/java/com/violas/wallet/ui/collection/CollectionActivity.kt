package com.violas.wallet.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.bertsir.zbar.utils.QRUtils
import com.palliums.utils.DensityUtility
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.utils.ClipboardUtils
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class CollectionActivity : BaseAppActivity() {
    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        private const val EXT_IS_TOKEN = "1"
        private const val EXT_TOKEN_ID = "2"
        fun start(context: Context, accountId: Long, isToken: Boolean = false, tokenId: Long = 0) {
            val intent = Intent(context, CollectionActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            intent.putExtra(EXT_IS_TOKEN, isToken)
            intent.putExtra(EXT_TOKEN_ID, tokenId)
            context.startActivity(intent)
        }
    }

    private var isToken = false
    private var mTokenId = 0L
    private var mAccountId = 0L

    private val mTokenManager by lazy {
        TokenManager()
    }

    override fun getLayoutResId() = R.layout.activity_collection

    override fun getTitleStyle(): Int {
        return TITLE_STYLE_GREY_BACKGROUND
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_colletction)

        isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
        mTokenId = intent.getLongExtra(EXT_TOKEN_ID, 0L)
        mAccountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)

        if (mAccountId == -1L) {
            finish()
            return
        }

        launch(Dispatchers.IO) {
            val currentAccount = AccountManager().getAccountById(mAccountId)

            withContext(Dispatchers.Main) {
                tvAddress.text = currentAccount.address
                tvWalletName.text = currentAccount.walletNickname
                btnCopy.setOnClickListener {
                    ClipboardUtils.copy(this@CollectionActivity, currentAccount.address)
                }
            }

            val prefix = if (isToken) {
                val tokenDo = mTokenManager.findTokenById(mTokenId)
                if (tokenDo != null) {
                    withContext(Dispatchers.Main) {
                        tvWalletName.text = "${currentAccount.walletNickname}-${tokenDo.name}"
                    }
                    "-${tokenDo.name.toLowerCase(Locale.CHINA)}"
                } else {
                    ""
                }
            } else {
                ""
            }

            val collectionAddress =
                "${CoinTypes.parseCoinType(currentAccount.coinNumber).fullName().toLowerCase(Locale.CHINA)}${prefix}:${currentAccount.address}"
            val createQRCodeBitmap = QRUtils.createQRCodeBitmap(
                collectionAddress,
                DensityUtility.dp2px(this@CollectionActivity, 164),
                null
            )
            withContext(Dispatchers.Main) {
                ivQRCode.setImageBitmap(createQRCodeBitmap)
            }
        }
    }
}
