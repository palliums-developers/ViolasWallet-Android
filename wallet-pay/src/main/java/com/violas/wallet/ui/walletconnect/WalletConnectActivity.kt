package com.violas.wallet.ui.walletconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.palliums.content.App
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.utils.decryptAccount
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import com.violas.walletconnect.jsonrpc.JsonRpcError
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.android.synthetic.main.view_wallet_connect_transfer.view.*
import kotlinx.coroutines.*
import org.palliums.violascore.crypto.Ed25519PublicKey
import org.palliums.violascore.crypto.Signature
import org.palliums.violascore.transaction.*
import java.lang.Exception
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.resume

class WalletConnectActivity : BaseAppActivity() {

    companion object {
        private const val CONNECT_DATA = "connect_data"

        private fun getContext(context: Context, result: (Boolean, Context) -> Unit) {
            var newTaskTag = false
            val contextWrapper: Context = when (context) {
                is App -> context.getTopActivity() ?: context.also { newTaskTag = true }
                is Activity -> context
                else -> context.also { newTaskTag = true }
            }
            return result.invoke(newTaskTag, contextWrapper)
        }

        fun startActivity(
            context: Context,
            mTransactionSwapVo: WalletConnect.TransactionSwapVo
        ) {
            getContext(context) { newTaskTag, newContext ->
                newContext.startActivity(Intent(context, WalletConnectActivity::class.java).apply {
                    if (newTaskTag) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    putExtra(CONNECT_DATA, mTransactionSwapVo)
                })
            }
        }
    }

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mWalletConnect by lazy {
        WalletConnect.getInstance(this)
    }

    // 是否处理了请求
    private var mRequestHandle = false

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect
    }

    override fun getTitleStyle() = TITLE_STYLE_CUSTOM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {
            val mTransactionSwapVo =
                intent.getParcelableExtra<WalletConnect.TransactionSwapVo>(CONNECT_DATA)
            if (mTransactionSwapVo == null) {
                finish()
            }

            showDisplay(mTransactionSwapVo)

            withContext(Dispatchers.Main) {
                btnConfirmLogin.setOnClickListener {
                    confirmAuthorization(mTransactionSwapVo)
                }
                tvCancelLogin.setOnClickListener {
                    cancelAuthorization(mTransactionSwapVo)
                }
            }
        }
    }

    private fun cancelAuthorization(mTransactionSwapVo: WalletConnect.TransactionSwapVo) {
        launch {
            try {
                val success = mWalletConnect.sendErrorMessage(
                    mTransactionSwapVo.requestID,
                    JsonRpcError.userRefused()
                )
                if (success) {
                    mRequestHandle = true
                    withContext(Dispatchers.Main) {
                        finish()
                    }
                } else {
                    showToast(R.string.common_http_socket_timeout)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(R.string.common_http_socket_timeout)
            }
        }
    }

    private fun confirmAuthorization(transactionSwapVo: WalletConnect.TransactionSwapVo) =
        launch(Dispatchers.IO) {
            var signedTx: String? = null
            try {
                if (!transactionSwapVo.isSigned) {
                    val trim = etLoginPwd.text.toString().trim()

                    val account = mAccountStorage.findById(transactionSwapVo.accountId)
                    if (account != null) {
                        val rawTransactionHex = transactionSwapVo.hexTx
                        val hashByteArray =
                            RawTransaction.hashByteArray(rawTransactionHex.hexStringToByteArray())
                        val signature = signTx(account, trim, hashByteArray) ?: return@launch

                        signedTx = SignedTransactionHex(
                            rawTransactionHex,
                            TransactionSignAuthenticator(
                                Ed25519PublicKey(account.publicKey.hexStringToByteArray()),
                                signature
                            )
                        ).toByteArray().toHex()
                    } else {
                        showToast("账户异常")
                    }
                } else {
                    signedTx = transactionSwapVo.hexTx
                }
                if (signedTx == null) {
                    return@launch
                }

                DataRepository.getViolasService().sendTransaction(signedTx)
                mWalletConnect.sendSuccessMessage(transactionSwapVo.requestID, "success")
                mRequestHandle = true
                finish()
                dismissProgress()
            } catch (e: Exception) {
                dismissProgress()
                e.message?.let { it1 -> showToast(it1) }
                e.printStackTrace()
            }
        }

    private suspend fun signTx(
        account: AccountDO,
        pwd: String,
        hashByteArray: ByteArray
    ) = suspendCancellableCoroutine<Signature?> { cont ->
        decryptAccount(
            accountDO = account,
            pwd = pwd,
            pwdErrorCallback = {
                launch(Dispatchers.Main) {
                    if (tvLoginPwdErrorTips.visibility != View.VISIBLE) {
                        tvLoginPwdErrorTips.visibility = View.VISIBLE
                    }
                    cont.resume(null)
                }
            }
        ) {
            val signMessage = it.keyPair.signMessage(hashByteArray)
            cont.resume(signMessage)
        }
    }

    private suspend fun showDisplay(transactionSwapVo: WalletConnect.TransactionSwapVo) =
        withContext(Dispatchers.IO) {
            when (transactionSwapVo.viewType) {
                WalletConnect.TransactionDataType.None.value -> {

                }
                WalletConnect.TransactionDataType.Normal.value -> {

                }
                WalletConnect.TransactionDataType.Transfer.value -> {
                    val viewData = transactionSwapVo.viewData
                    println("transfer data: $viewData")
                    val mTransferDataType = Gson().fromJson(
                        viewData,
                        WalletConnect.TransferDataType::class.java
                    )

                    val amount = BigDecimal(mTransferDataType.amount).divide(
                        BigDecimal("1000000"), RoundingMode.DOWN
                    ).stripTrailingZeros().toPlainString()

                    val view = LayoutInflater.from(this@WalletConnectActivity)
                        .inflate(R.layout.view_wallet_connect_transfer, null)
                    withContext(Dispatchers.Main) {
                        view.tvDescribeAmount.text = amount
                        view.tvDescribeAddress.text = mTransferDataType.to
                        view.tvDescribeFee.text = "0.00"

                        viewGroupContent.removeAllViews()
                        viewGroupContent.addView(view)
                    }
                }
            }
        }

    override fun onDestroy() {
        if (!mRequestHandle) {
            GlobalScope.launch {
                intent.getParcelableExtra<WalletConnect.TransactionSwapVo>(CONNECT_DATA)?.let {
                    mWalletConnect.sendErrorMessage(it.requestID, JsonRpcError.userRefused())
                }
            }
        }
        super.onDestroy()
    }
}
