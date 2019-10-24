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
import com.violas.wallet.utils.DensityUtility
import kotlinx.android.synthetic.main.activity_collection.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        setTitle("收款")
        launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            withContext(Dispatchers.Main) {
                tvAddress.text = currentAccount.address
                btnCopy.setOnClickListener {
                    ClipboardUtils.copy(this@CollectionActivity, currentAccount.address)
                }
            }

            val collectionAddress =
                "${CoinTypes.parseCoinType(currentAccount.coinNumber).coinName().toLowerCase()}:${currentAccount.address}"
            val createQRCodeBitmap = QRUtils.createQRCodeBitmap(
                collectionAddress,
                DensityUtility.dp2px(this@CollectionActivity, 240),
                null
            )
            withContext(Dispatchers.Main) {
                ivQRCode.setImageBitmap(createQRCodeBitmap)
            }
        }
    }
}
