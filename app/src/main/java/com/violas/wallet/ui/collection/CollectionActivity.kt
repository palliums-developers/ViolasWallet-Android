package com.violas.wallet.ui.collection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import cn.bertsir.zbar.utils.QRUtils
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.base.BaseActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.utils.ClipboardUtils
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.jessyan.autosize.utils.AutoSizeUtils
import java.util.*


class CollectionActivity : BaseActivity() {
    companion object {
        private const val EXT_ACCOUNT_ID = "0"
        fun start(context: Context, accountId: Long) {
            val intent = Intent(context, CollectionActivity::class.java)
            intent.putExtra(EXT_ACCOUNT_ID, accountId)
            context.startActivity(intent)
        }
    }

    override fun getLayoutResId() = R.layout.activity_collection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_colletction)
        setTitleStyle(TITLE_STYLE_GREY_BACKGROUND)
        launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            withContext(Dispatchers.Main) {
                tvAddress.text = currentAccount.address
                tvWalletName.text = currentAccount.walletNickname
                btnCopy.setOnClickListener {
                    ClipboardUtils.copy(this@CollectionActivity, currentAccount.address)
                }
            }

            val collectionAddress =
                "${CoinTypes.parseCoinType(currentAccount.coinNumber).fullName().toLowerCase(Locale.CHINA)}:${currentAccount.address}"
            val createQRCodeBitmap = QRUtils.createQRCodeBitmap(
                collectionAddress,
                AutoSizeUtils.dp2px(this@CollectionActivity, 164.toFloat()),
                null
            )
            withContext(Dispatchers.Main) {
                ivQRCode.setImageBitmap(createQRCodeBitmap)
            }
        }
    }
}
