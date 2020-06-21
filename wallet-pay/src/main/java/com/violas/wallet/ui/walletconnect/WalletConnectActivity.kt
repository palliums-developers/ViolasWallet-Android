package com.violas.wallet.ui.walletconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.wallet.widget.dialog.PasswordInputDialog
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import com.violas.walletconnect.jsonrpc.JsonRpcError
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.android.synthetic.main.view_wallet_connect_none.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_transfer.view.*
import kotlinx.coroutines.*
import org.palliums.violascore.crypto.Ed25519PublicKey
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.crypto.Signature
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.SignedTransactionHex
import org.palliums.violascore.transaction.TransactionSignAuthenticator
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class WalletConnectActivity : BaseAppActivity() {

    companion object {
        private const val CONNECT_DATA = "connect_data"

        private fun getContext(context: Context, result: (Boolean, Context) -> Unit) {
            var newTaskTag = false
            val contextWrapper: Context = when (context) {
                is App -> context.getTopActivity() ?: context.also { newTaskTag = true }
                is Activity -> context
                is Fragment -> {
                    if (context.activity == null) {
                        newTaskTag = true
                        context.applicationContext
                    } else {
                        context.activity!!
                    }
                }
                else -> context.also { newTaskTag = true }
            }
            return result.invoke(newTaskTag, contextWrapper)
        }

        fun startActivity(
            context: Context,
            mTransactionSwapVo: WalletConnect.TransactionSwapVo
        ) {
            getContext(context) { newTaskTag, newContext ->
                newContext.startActivity(
                    Intent(
                        newContext,
                        WalletConnectActivity::class.java
                    ).apply {
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

    override fun getTitleStyle() = PAGE_STYLE_CUSTOM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitleLeftViewVisibility(View.GONE)

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
                ivClose.setOnClickListener {
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
                    val account = mAccountStorage.findById(transactionSwapVo.accountId)
                    if (account != null) {
                        val rawTransactionHex = transactionSwapVo.hexTx
                        val hashByteArray =
                            RawTransaction.hashByteArray(rawTransactionHex.hexStringToByteArray())
                        val signature = showPasswordSignTx(account, hashByteArray) ?: return@launch

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

    private suspend fun showPasswordSignTx(account: AccountDO, hashByteArray: ByteArray) =
        suspendCancellableCoroutine<Signature?> { cont ->
            PasswordInputDialog()
                .setConfirmListener { password, dialogFragment ->
                    dialogFragment.dismiss()
                    showProgress()
                    launch(Dispatchers.IO) {
                        val decryptPrivateKey =
                            SimpleSecurity.instance(ContextProvider.getContext())
                                .decrypt(password, account.privateKey)
                        if (decryptPrivateKey == null) {
                            cont.resumeWithException(WrongPasswordException())
                            return@launch
                        }
                        val keyPair = KeyPair.fromSecretKey(decryptPrivateKey)
                        val signMessage = keyPair.signMessage(hashByteArray)
                        cont.resume(signMessage)
                    }
                }.show(supportFragmentManager)
        }

    private suspend fun showDisplay(transactionSwapVo: WalletConnect.TransactionSwapVo) =
        withContext(Dispatchers.IO) {
            when (transactionSwapVo.viewType) {
                WalletConnect.TransactionDataType.None.value -> {
                    val create = GsonBuilder().setPrettyPrinting().create()
                    val viewData = transactionSwapVo.viewData

                    val je: JsonElement = JsonParser.parseString(viewData)
                    val prettyJsonStr2: String = create.toJson(je)
                    val view = LayoutInflater.from(this@WalletConnectActivity)
                        .inflate(R.layout.view_wallet_connect_none, viewGroupContent, false)
                    withContext(Dispatchers.Main) {
                        view.tvContent.text = prettyJsonStr2
                        viewGroupContent.removeAllViews()
                        viewGroupContent.addView(view)
                    }
                }
                WalletConnect.TransactionDataType.PUBLISH.value -> {
                    val viewData = transactionSwapVo.viewData
                    println("transfer data: $viewData")
                    val mPublishDataType = Gson().fromJson(
                        viewData,
                        WalletConnect.PublishDataType::class.java
                    )
                    val view = LayoutInflater.from(this@WalletConnectActivity)
                        .inflate(R.layout.view_wallet_connect_publish, viewGroupContent, false)
                    withContext(Dispatchers.Main) {
                        view.tvDescribeSender.text = mPublishDataType.form

                        viewGroupContent.removeAllViews()
                        viewGroupContent.addView(view)
                    }
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
                        .inflate(R.layout.view_wallet_connect_transfer, viewGroupContent, false)
                    withContext(Dispatchers.Main) {
                        view.tvDescribeSender.text = mTransferDataType.form
                        view.tvDescribeAddress.text = mTransferDataType.to
                        view.tvDescribeAmount.text = "$amount ${mTransferDataType.coinName}"
                        view.tvDescribeFee.text = "0.00 ${mTransferDataType.coinName}"

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
