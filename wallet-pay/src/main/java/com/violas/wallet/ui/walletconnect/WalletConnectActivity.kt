package com.violas.wallet.ui.walletconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.palliums.content.App
import com.palliums.extensions.getShowErrorMessage
import com.palliums.utils.setSystemBar
import com.quincysx.crypto.bitcoin.script.Script
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.walletconnect.*
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import com.violas.walletconnect.jsonrpc.JsonRpcError
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.*
import org.palliums.lib.jsonrpc.ResponseExceptions
import org.palliums.violascore.crypto.Ed25519PublicKey
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.SignedTransactionHex
import org.palliums.violascore.transaction.TransactionSignAuthenticator
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
            mTransactionSwapVo: TransactionSwapVo
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
                (newContext as? Activity)?.overridePendingTransition(
                    R.anim.activity_bottom_in,
                    R.anim.activity_none
                )
            }
        }
    }

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mWalletConnect by lazy { WalletConnect.getInstance(this) }

    private lateinit var mTransactionSwapVo: TransactionSwapVo

    // 是否处理了请求
    private var mRequestHandled = false
    private var mRequestProcessing = false

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect
    }

    override fun getTitleStyle() = PAGE_STYLE_NOT_TITLE

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_none, R.anim.activity_bottom_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setSystemBar(lightModeStatusBar = true, lightModeNavigationBar = true)
        super.onCreate(savedInstanceState)

        launch(Dispatchers.IO) {
            val transactionSwapVo = intent.getParcelableExtra<TransactionSwapVo>(CONNECT_DATA)
            if (transactionSwapVo == null) {
                finish()
                return@launch
            }

            mTransactionSwapVo = transactionSwapVo
            showDisplay()

            withContext(Dispatchers.Main) {
                btnConfirm.setOnClickListener {
                    confirmAuthorization()
                }
                ivClose.setOnClickListener {
                    cancelAuthorization()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        launch(Dispatchers.IO) {
            if (mRequestProcessing) return@launch
            val transactionSwapVo = intent?.getParcelableExtra<TransactionSwapVo>(CONNECT_DATA)
                ?: return@launch

            showProgress()
            delay(1000)
            mTransactionSwapVo = transactionSwapVo
            showDisplay()
            dismissProgress()
        }
    }

    private fun cancelAuthorization() {
        launch {
            mRequestProcessing = true
            val transactionSwapVo = mTransactionSwapVo
            try {
                val success = mWalletConnect.sendErrorMessage(
                    transactionSwapVo.requestID,
                    JsonRpcError.userRefused()
                )
                if (success) {
                    mRequestHandled = true
                    finish()
                } else {
                    mRequestProcessing = false
                    showToast(R.string.common_http_socket_timeout)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mRequestProcessing = false
                showToast(R.string.common_http_socket_timeout)
            }
        }
    }

    private fun confirmAuthorization() =
        launch(Dispatchers.IO) {
            mRequestProcessing = true
            val transactionSwapVo = mTransactionSwapVo
            var signedTx: String? = null
            try {
                if (!transactionSwapVo.isSigned) {
                    val account = mAccountStorage.findById(transactionSwapVo.accountId)
                    when (account?.coinNumber) {
                        getDiemCoinType().coinNumber() -> {
                            // <editor-fold defaultstate="collapsed" desc="处理 Diem 交易">
                            val rawTransactionHex = transactionSwapVo.hexTx
                            val hashByteArray =
                                org.palliums.libracore.transaction.RawTransaction.hashByteArray(
                                    rawTransactionHex.hexStringToByteArray()
                                )
                            val privateKey = showPasswordSignTx(account) ?: return@launch

                            val keyPair =
                                org.palliums.libracore.crypto.KeyPair.fromSecretKey(privateKey)
                            val signature = keyPair.signMessage(hashByteArray)

                            signedTx = org.palliums.libracore.transaction.SignedTransactionHex(
                                rawTransactionHex,
                                org.palliums.libracore.transaction.TransactionSignAuthenticator(
                                    org.palliums.libracore.crypto.Ed25519PublicKey(account.publicKey.hexStringToByteArray()),
                                    signature
                                )
                            ).toByteArray().toHex()
                            // </editor-fold>
                        }
                        getViolasCoinType().coinNumber() -> {
                            // <editor-fold defaultstate="collapsed" desc="处理 Violas 交易">
                            val rawTransactionHex = transactionSwapVo.hexTx
                            val hashByteArray =
                                RawTransaction.hashByteArray(rawTransactionHex.hexStringToByteArray())
                            val privateKey = showPasswordSignTx(account) ?: return@launch

                            val keyPair = KeyPair.fromSecretKey(privateKey)
                            val signature = keyPair.signMessage(hashByteArray)

                            signedTx = SignedTransactionHex(
                                rawTransactionHex,
                                TransactionSignAuthenticator(
                                    Ed25519PublicKey(account.publicKey.hexStringToByteArray()),
                                    signature
                                )
                            ).toByteArray().toHex()
                            // </editor-fold>
                        }
                        getBitcoinCoinType().coinNumber() -> {
                            // <editor-fold defaultstate="collapsed" desc="处理 Bitcoin 交易">

                            val privateKey = showPasswordSignTx(account) ?: return@launch

                            val data = Gson().fromJson<BitcoinTransferData>(
                                transactionSwapVo.viewData,
                                BitcoinTransferData::class.java
                            )
                            val mTransactionManager: TransactionManager =
                                TransactionManager(arrayListOf(data.payerAddress))
                            val checkBalance =
                                mTransactionManager.checkBalance(data.amount / 100000000.0, 3)

                            if (!checkBalance) {
                                throw LackOfBalanceException()
                            }

                            val script = try {
                                if (data.data.isEmpty()) {
                                    null
                                } else
                                    Script(data.data.hexStringToByteArray())
                            } catch (e: java.lang.Exception) {
                                null
                            }

                            val txId: String = suspendCancellableCoroutine { coroutin ->
                                val subscribe = mTransactionManager.obtainTransaction(
                                    privateKey,
                                    account.publicKey.hexStringToByteArray(),
                                    checkBalance,
                                    data.payeeAddress,
                                    data.changeForm,
                                    script
                                ).flatMap {
                                    try {
                                        BitcoinChainApi.get()
                                            .pushTx(it.signBytes.toHex())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        throw TransferUnknownException()
                                    }
                                }.subscribe({
                                    coroutin.resume(it)
                                }, {
                                    coroutin.resumeWithException(it)
                                })
                                coroutin.invokeOnCancellation {
                                    subscribe.dispose()
                                }
                            }
                            if (txId.isNotEmpty()) {

                            }
                            // </editor-fold>
                        }
                        else -> {
                            showToast(R.string.common_tips_account_error)
                        }
                    }
                } else {
                    signedTx = transactionSwapVo.hexTx
                }
                if (signedTx != null && transactionSwapVo.isSend) {
                    when (transactionSwapVo.coinType) {
                        getViolasCoinType() -> {
                            DataRepository.getViolasRpcService().submitTransaction(signedTx)
                        }
                        getDiemCoinType() -> {
                            DataRepository.getDiemRpcService().submitTransaction(signedTx)
                        }
                    }
                }
                if (!transactionSwapVo.isSend) {
                    if (signedTx == null) {
                        mWalletConnect.sendErrorMessage(
                            transactionSwapVo.requestID,
                            JsonRpcError.invalidParams("sign")
                        )
                    } else {
                        mWalletConnect.sendSuccessMessage(transactionSwapVo.requestID, signedTx)
                    }
                } else {
                    mWalletConnect.sendSuccessMessage(transactionSwapVo.requestID, "success")
                }

                CommandActuator.postDelay(RefreshAssetsCommand(), 2000)
                mRequestHandled = true
                finish()
                dismissProgress()
            } catch (e: Exception) {
                e.printStackTrace()

                dismissProgress()
                showToast(
                    e.getShowErrorMessage(
                        failedDesc = if (e is ResponseExceptions) e.msg else null
                    )
                )

                when (e) {
                    is LackOfBalanceException ->
                        JsonRpcError.lackOfBalanceError()
                    is ResponseExceptions ->
                        if (e.code != -1) JsonRpcError(e.code, e.msg) else null
                    else -> {
                        withContext(Dispatchers.Main) {
                            mRequestProcessing = false
                        }
                        null
                    }

                }?.let {
                    mWalletConnect.sendErrorMessage(transactionSwapVo.requestID, it)
                    mRequestHandled = true
                    close()
                }
            }
        }

    private suspend fun showPasswordSignTx(account: AccountDO) =
        suspendCancellableCoroutine<ByteArray?> { cont ->
            authenticateAccount(account) {
                showProgress()
                launch(Dispatchers.IO) {
                    val decryptPrivateKey = it
                    if (decryptPrivateKey == null) {
                        cont.resumeWithException(WrongPasswordException())
                        return@launch
                    }
                    cont.resume(decryptPrivateKey)
                }
            }
        }

    private suspend fun showDisplay() = launch(Dispatchers.IO) {
        val transactionSwapVo = mTransactionSwapVo
        ViewFillers().getView(
            this@WalletConnectActivity,
            viewGroupContent,
            tvTitle,
            transactionSwapVo.coinType,
            transactionSwapVo.isSend,
            transactionSwapVo.viewType,
            transactionSwapVo.viewData
        )?.let {
            withContext(Dispatchers.Main) {
                viewGroupContent.removeAllViews()
                viewGroupContent.addView(it)
            }
        }
    }

    override fun onDestroy() {
        if (!mRequestHandled) {
            GlobalScope.launch {
                intent.getParcelableExtra<TransactionSwapVo>(CONNECT_DATA)?.let {
                    mWalletConnect.sendErrorMessage(it.requestID, JsonRpcError.userRefused())
                }
            }
        }
        super.onDestroy()
    }
}
