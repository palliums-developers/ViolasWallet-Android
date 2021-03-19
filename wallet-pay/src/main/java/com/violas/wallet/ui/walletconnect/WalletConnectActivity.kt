package com.violas.wallet.ui.walletconnect

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.palliums.content.App
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
import com.violas.wallet.walletconnect.messageHandler.TransferBitcoinDataType
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import com.violas.walletconnect.jsonrpc.JsonRpcError
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.coroutines.*
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
    private val mWalletConnect by lazy {
        WalletConnect.getInstance(this)
    }

    // 是否处理了请求
    private var mRequestHandle = false

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
            val mTransactionSwapVo =
                intent.getParcelableExtra<TransactionSwapVo>(CONNECT_DATA)
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

    private fun cancelAuthorization(mTransactionSwapVo: TransactionSwapVo) {
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

    private fun confirmAuthorization(transactionSwapVo: TransactionSwapVo) =
        launch(Dispatchers.IO) {
            var signedTx: String? = null
            try {
                if (!transactionSwapVo.isSigned) {
                    val account = mAccountStorage.findById(transactionSwapVo.accountId)
                    when (account?.coinNumber) {
                        getDiemCoinType().coinNumber() -> {
                            // <editor-fold defaultstate="collapsed" desc="处理 Libra 交易">
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

                            val fromJson = Gson().fromJson<TransferBitcoinDataType>(
                                transactionSwapVo.viewData,
                                TransferBitcoinDataType::class.java
                            )
                            val mTransactionManager: TransactionManager =
                                TransactionManager(arrayListOf(fromJson.form))
                            val checkBalance =
                                mTransactionManager.checkBalance(fromJson.amount / 100000000.0, 3)

                            if (!checkBalance) {
                                throw LackOfBalanceException()
                            }

                            val script = try {
                                if (fromJson.data.isEmpty()) {
                                    null
                                } else
                                    Script(fromJson.data.hexStringToByteArray())
                            } catch (e: java.lang.Exception) {
                                null
                            }

                            val txId: String = suspendCancellableCoroutine { coroutin ->
                                val subscribe = mTransactionManager.obtainTransaction(
                                    privateKey,
                                    account.publicKey.hexStringToByteArray(),
                                    checkBalance,
                                    fromJson.to,
                                    fromJson.changeForm,
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
                            DataRepository.getViolasChainRpcService().submitTransaction(signedTx)
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
                mRequestHandle = true
                finish()
                dismissProgress()
            } catch (e: LackOfBalanceException) {
                dismissProgress()
                mWalletConnect.sendErrorMessage(
                    transactionSwapVo.requestID,
                    JsonRpcError.lackOfBalanceError()
                )
                e.message?.let { it1 -> showToast(it1) }
            } catch (e: Exception) {
                dismissProgress()
                e.message?.let { it1 -> showToast(it1) }
                e.printStackTrace()
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

    private suspend fun showDisplay(transactionSwapVo: TransactionSwapVo) = launch(Dispatchers.IO) {
        ViewFillers().getView(
            this@WalletConnectActivity,
            viewGroupContent,
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
        if (!mRequestHandle) {
            GlobalScope.launch {
                intent.getParcelableExtra<TransactionSwapVo>(CONNECT_DATA)?.let {
                    mWalletConnect.sendErrorMessage(it.requestID, JsonRpcError.userRefused())
                }
            }
        }
        super.onDestroy()
    }
}
