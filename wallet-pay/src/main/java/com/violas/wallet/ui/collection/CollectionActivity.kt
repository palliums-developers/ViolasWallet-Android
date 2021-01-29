package com.violas.wallet.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.bertsir.zbar.utils.QRUtils
import com.palliums.extensions.expandTouchArea
import com.palliums.utils.DensityUtility
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.common.Vm
import com.violas.wallet.utils.ClipboardUtils
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.wallet.AccountIdentifier
import org.palliums.libracore.wallet.IntentIdentifier
import org.palliums.violascore.serialization.hexToBytes
import java.util.*

/**
 * 收款页面
 */
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
        return PAGE_STYLE_LIGHT_MODE_PRIMARY_TOP_BAR
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXT_IS_TOKEN, isToken)
        outState.putLong(EXT_TOKEN_ID, mTokenId)
        outState.putLong(EXT_ACCOUNT_ID, mAccountId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.receive_title)

        if (savedInstanceState != null) {
            isToken = savedInstanceState.getBoolean(EXT_IS_TOKEN, false)
            mTokenId = savedInstanceState.getLong(EXT_TOKEN_ID, 0L)
            mAccountId = savedInstanceState.getLong(EXT_ACCOUNT_ID, -1L)
        } else if (intent != null) {
            isToken = intent.getBooleanExtra(EXT_IS_TOKEN, false)
            mTokenId = intent.getLongExtra(EXT_TOKEN_ID, 0L)
            mAccountId = intent.getLongExtra(EXT_ACCOUNT_ID, -1L)
        }

        if (mAccountId == -1L) {
            finish()
            return
        }

        launch(Dispatchers.IO) {
            val currentAccount = AccountManager().getAccountById(mAccountId)

            withContext(Dispatchers.Main) {
                tvAddress.text = currentAccount.address
//                tvWalletName.text = currentAccount.walletNickname
                btnCopy.setOnClickListener {
                    ClipboardUtils.copy(this@CollectionActivity, currentAccount.address)
                }
                btnCopy.expandTouchArea(20)
                tvAddress.setOnClickListener {
                    ClipboardUtils.copy(this@CollectionActivity, currentAccount.address)
                }
            }

            val collectionAddress = when (currentAccount.coinNumber) {
                CoinTypes.Bitcoin.coinType(), CoinTypes.BitcoinTest.coinType() -> {
                    "${
                        CoinTypes.parseCoinType(currentAccount.coinNumber).fullName()
                            .toLowerCase(Locale.CHINA)
                    }:${currentAccount.address}"
                }
                CoinTypes.Libra.coinType() -> {
                    val tokenDo = mTokenManager.findTokenById(mTokenId)
                    val network = if (Vm.TestNet) {
                        AccountIdentifier.NetworkPrefix.TestnetPrefix
                    } else {
                        AccountIdentifier.NetworkPrefix.MainnetPrefix
                    }
                    IntentIdentifier(
                        AccountIdentifier(
                            network,
                            AccountAddress(currentAccount.address.hexToBytes())
                        ), currency = tokenDo?.name
                    ).encode()
                }
                CoinTypes.Violas.coinType() -> {
                    val tokenDo = mTokenManager.findTokenById(mTokenId)
                    val network = if (Vm.TestNet) {
                        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.TestnetPrefix
                    } else {
                        org.palliums.violascore.wallet.AccountIdentifier.NetworkPrefix.MainnetPrefix
                    }
                    org.palliums.violascore.wallet.IntentIdentifier(
                        org.palliums.violascore.wallet.AccountIdentifier(
                            network,
                            org.palliums.violascore.transaction.AccountAddress(currentAccount.address.hexToBytes())
                        ), currency = tokenDo?.name
                    ).encode()
                }
                else -> null
            }
            collectionAddress?.let {
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
}
