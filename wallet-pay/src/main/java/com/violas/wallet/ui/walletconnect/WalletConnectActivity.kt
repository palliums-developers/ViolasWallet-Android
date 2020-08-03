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
import com.quincysx.crypto.CoinTypes
import com.quincysx.crypto.bitcoin.script.Script
import com.quincysx.crypto.utils.Base64
import com.violas.wallet.R
import com.violas.wallet.base.BaseAppActivity
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.biz.exchange.processor.ViolasOutputScript
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.ui.main.market.bean.IAssetsMark
import com.violas.wallet.utils.authenticateAccount
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.wallet.walletconnect.walletConnectMessageHandler.*
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import com.violas.walletconnect.jsonrpc.JsonRpcError
import kotlinx.android.synthetic.main.activity_wallet_connect.*
import kotlinx.android.synthetic.main.view_wallet_connect_none.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_publish.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_transfer.view.*
import kotlinx.android.synthetic.main.view_wallet_connect_transfer.view.tvDescribeSender
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
            }
        }
    }

    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mWalletConnect by lazy {
        WalletConnect.getInstance(this)
    }
    private val mAccountManager by lazy {
        AccountManager()
    }

    // 是否处理了请求
    private var mRequestHandle = false

    override fun getLayoutResId(): Int {
        return R.layout.activity_wallet_connect
    }

    override fun getTitleStyle() = PAGE_STYLE_NOT_TITLE

    override fun onCreate(savedInstanceState: Bundle?) {
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
                        CoinTypes.Libra.coinType() -> {
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
                        CoinTypes.Violas.coinType() -> {
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
                        CoinTypes.Bitcoin.coinType(),
                        CoinTypes.BitcoinTest.coinType() -> {
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
                            val violasOutputScript = ViolasOutputScript()

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
                            showToast("账户异常")
                        }
                    }
                } else {
                    signedTx = transactionSwapVo.hexTx
                }
                if (signedTx != null) {
                    when (transactionSwapVo.coinType) {
                        CoinTypes.Violas -> {
                            DataRepository.getViolasChainRpcService().submitTransaction(signedTx)
                        }
                        CoinTypes.Libra -> {
                            DataRepository.getLibraService().submitTransaction(signedTx)
                        }
                    }
                }

                mWalletConnect.sendSuccessMessage(transactionSwapVo.requestID, "success")
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 2000)
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
            authenticateAccount(account, mAccountManager) {
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

    private suspend fun showDisplay(transactionSwapVo: TransactionSwapVo) =
        withContext(Dispatchers.IO) {
            when (transactionSwapVo.viewType) {
                TransactionDataType.None.value -> {
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
                TransactionDataType.PUBLISH.value -> {
                    val viewData = transactionSwapVo.viewData
                    println("transfer data: $viewData")
                    val mPublishDataType = Gson().fromJson(
                        viewData,
                        PublishDataType::class.java
                    )
                    val view = LayoutInflater.from(this@WalletConnectActivity)
                        .inflate(R.layout.view_wallet_connect_publish, viewGroupContent, false)
                    withContext(Dispatchers.Main) {
                        view.tvDescribeSender.text = mPublishDataType.form
                        view.tvDescribeCoinName.text = mPublishDataType.coinName
                        viewGroupContent.removeAllViews()
                        viewGroupContent.addView(view)
                    }
                }
                TransactionDataType.Transfer.value -> {
                    val viewData = transactionSwapVo.viewData
                    println("transfer data: $viewData")
                    val mTransferDataType = Gson().fromJson(
                        viewData,
                        TransferDataType::class.java
                    )

                    val amount = BigDecimal(mTransferDataType.amount).divide(
                        BigDecimal("1000000"), 6, RoundingMode.DOWN
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
                TransactionDataType.BITCOIN_TRANSFER.value -> {
                    val viewData = transactionSwapVo.viewData
                    println("transfer data: $viewData")
                    val mTransferDataType = Gson().fromJson(
                        viewData,
                        TransferBitcoinDataType::class.java
                    )

                    val amount = BigDecimal(mTransferDataType.amount).divide(
                        BigDecimal("1000000"), 6, RoundingMode.DOWN
                    ).stripTrailingZeros().toPlainString()

                    val view = LayoutInflater.from(this@WalletConnectActivity)
                        .inflate(R.layout.view_wallet_connect_transfer, viewGroupContent, false)
                    withContext(Dispatchers.Main) {
                        view.tvDescribeSender.text = mTransferDataType.form
                        view.tvDescribeAddress.text = mTransferDataType.to
                        view.tvDescribeAmount.text = "$amount BTC"
                        view.tvDescribeFee.text = "0.00 BTC"

                        viewGroupContent.removeAllViews()
                        viewGroupContent.addView(view)
                    }
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
