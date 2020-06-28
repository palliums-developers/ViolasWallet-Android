package com.violas.wallet.walletconnect


import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.appcenter.crashes.Crashes
import com.palliums.violas.error.ViolasException
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.walletconnect.WalletConnectActivity
import com.violas.wallet.walletconnect.transferDataHandler.TransferDecodeEngine
import com.violas.walletconnect.WCClient
import com.violas.walletconnect.WCSessionStoreItem
import com.violas.walletconnect.WCSessionStoreType
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.jsonrpc.JsonRpcErrorResponse
import com.violas.walletconnect.jsonrpc.JsonRpcResponse
import com.violas.walletconnect.models.WCPeerMeta
import com.violas.walletconnect.models.session.WCSession
import com.violas.walletconnect.models.violas.WCViolasSendRawTransaction
import com.violas.walletconnect.models.violas.WCViolasSendTransaction
import com.violas.walletconnect.models.violas.WCViolasSignRawTransaction
import com.violas.walletconnect.models.violasprivate.WCViolasAccount
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.violascore.serialization.LCSInputStream
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.lbrStructTagType
import org.palliums.violascore.transaction.storage.TypeTag
import java.lang.Exception

enum class WalletConnectStatus {
    None, Login
}

interface WalletConnectListener {
    fun onLogin()
    fun onDisconnect()
}

interface WalletConnectSessionListener {
    fun onRequest(id: Long, peer: WCPeerMeta)
}

class WalletConnect private constructor(val context: Context) : CoroutineScope by MainScope() {

    companion object {
        @Volatile
        private var instance: WalletConnect? = null

        fun getInstance(context: Context): WalletConnect {
            return instance ?: synchronized(this) {
                instance ?: WalletConnect(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    var mWalletConnectListener: WalletConnectListener? = null
    var mWalletConnectSessionListener: WalletConnectSessionListener? = null

    private val mGsonBuilder = GsonBuilder()
    private val httpClient: OkHttpClient = OkHttpClient()
    val mWCClient: WCClient = WCClient(httpClient, mGsonBuilder)
    private val mWCSessionStoreType =
        WCSessionStoreType(WCSessionStoreType.getSharedPreferences(context), mGsonBuilder)

    init {
        listenerClientEvent()
        mWCClient.addSocketListener(object : WebSocketListener() {
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                mWalletConnectListener?.onDisconnect()
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                mWalletConnectListener?.onLogin()
            }
        })
    }

    //    private val mAccountManager by lazy { AccountManager() }
    private val mViolasService by lazy { DataRepository.getViolasService() }
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    fun restore() {
        launch(Dispatchers.IO) {
            Log.e("Wallet Connwct","Restore Connect")
            mWCSessionStoreType
                .session?.let {
                    mWCClient.connect(it.session, it.remotePeerMeta, it.peerId, it.remotePeerId)
                }
        }
    }

    fun connect(
        msg: String
    ): Boolean {
        val from = WCSession.from(msg) ?: return false
        val wcPeerMeta = WCPeerMeta(
            "violasPay", "https://www.violas.io"
        )
        mWCClient.connect(from, wcPeerMeta)
        return true
    }

    private fun listenerClientEvent() {
        mWCClient.onSessionRequest = { id, peer ->
            mWCClient.session?.let { session ->
                val wcSessionStoreItem = WCSessionStoreItem(
                    session,
                    mWCClient.peerId ?: "",
                    mWCClient.remotePeerId ?: "",
                    peer
                )
                mWCSessionStoreType.session = wcSessionStoreItem
            }
            mWalletConnectSessionListener?.onRequest(id, peer) ?: rejectSession()
//            WalletConnectAuthorizationActivity.startActivity(context, id, peer)
        }
        mWCClient.onDisconnect = { _, _ ->
            mWCSessionStoreType.session = null
            mWalletConnectListener?.onDisconnect()
        }
        mWCClient.onViolasSendRawTransaction = { id, violasSendRawTransaction ->
            convertAndCheckTransaction(id, violasSendRawTransaction)
        }
        mWCClient.onViolasSendTransaction = { id, violasSendTransaction ->
            convertAndCheckTransaction(id, violasSendTransaction)
        }
        mWCClient.onViolasSignTransaction = { id, violasSignRawTransaction ->
            convertAndCheckTransaction(id, violasSignRawTransaction)
        }
        mWCClient.onGetAccounts = { id ->
            val accounts = mAccountStorage.loadAll().map {
                WCViolasAccount(
                    coinType = when (it.coinNumber) {
                        CoinTypes.Violas.coinType() -> "violas"
                        CoinTypes.Libra.coinType() -> "libra"
                        else -> "bitcoin"
                    },
//                    name = it.walletNickname,
                    name = "",
                    address = it.address,
                    walletType = 0
                )
            }
            sendSuccessMessage(id, accounts)
        }
    }

    private fun <T> convertAndCheckTransaction(requestID: Long, tx: T) = runBlocking {
        try {
            val mTransactionSwapVo: TransactionSwapVo? = when (tx) {
                is WCViolasSignRawTransaction -> {
                    val account = mAccountStorage.findByCoinTypeAndCoinAddress(
                        CoinTypes.Violas.coinType(),
                        tx.address
                    )

                    if (account == null) {
                        sendInvalidParameterErrorMessage(requestID, "Account does not exist.")
                        return@runBlocking
                    }

                    val rawTransaction =
                        RawTransaction.decode(LCSInputStream(tx.message.hexStringToByteArray()))

                    val decode = TransferDecodeEngine(rawTransaction).decode()

                    TransactionSwapVo(
                        requestID,
                        rawTransaction.toByteArray().toHex(),
                        false,
                        account.id,
                        decode.first.value,
                        decode.second
                    )
                }
                is WCViolasSendRawTransaction -> {
                    // todo 正在考虑要不要自动上链。
                    TransactionSwapVo(
                        requestID,
                        tx.tx,
                        true,
                        -1L,
                        TransactionDataType.None.value, ""
                    )
                }
                is WCViolasSendTransaction -> {
                    val account = mAccountStorage.findByCoinTypeAndCoinAddress(
                        CoinTypes.Violas.coinType(),
                        tx.from
                    )

                    if (account == null) {
                        sendInvalidParameterErrorMessage(requestID, "Account does not exist.")
                        return@runBlocking
                    }

                    val gasUnitPrice = tx.gasUnitPrice ?: 0
                    val maxGasAmount = tx.maxGasAmount ?: 1_000_000
                    val expirationTime = tx.expirationTime ?: System.currentTimeMillis() + 1000
                    val gasCurrencyCode = tx.gasCurrencyCode ?: lbrStructTagType()
                    val sequenceNumber = tx.sequenceNumber ?: -1

                    val payload = TransactionPayload.Script(
                        tx.payload.code.hexToBytes(),
                        tx.payload.tyArgs.map { TypeTag.decode(LCSInputStream(it.hexToBytes())) },
                        tx.payload.args.map {
                            when (it.type.toLowerCase()) {
                                "address" -> {
                                    TransactionArgument.newAddress(it.value)
                                }
                                "bool" -> {
                                    TransactionArgument.newBool(it.value.toBoolean())
                                }
                                "number" -> {
                                    TransactionArgument.newU64(it.value.toLong())
                                }
                                "bytes" -> {
                                    TransactionArgument.newByteArray(it.value.hexToBytes())
                                }
                                else -> {
                                    TransactionArgument.newByteArray(it.value.toByteArray())
                                }
                            }
                        }
                    )

                    Log.e("WalletConnect", Gson().toJson(payload))

                    val generateRawTransaction = mViolasService.generateRawTransaction(
                        TransactionPayload(payload),
                        tx.from,
                        sequenceNumber,
                        gasCurrencyCode,
                        maxGasAmount,
                        gasUnitPrice,
                        expirationTime - System.currentTimeMillis()
                    )

                    val decode = TransferDecodeEngine(generateRawTransaction).decode()

                    TransactionSwapVo(
                        requestID,
                        generateRawTransaction.toByteArray().toHex(),
                        false,
                        account.id,
                        decode.first.value,
                        decode.second
                    )
                }
                else -> null
            }

            mTransactionSwapVo?.let {
                WalletConnectActivity.startActivity(context, mTransactionSwapVo)
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
            sendInvalidParameterErrorMessage(
                requestID,
                "Parameters of the abnormal"
            )
            Crashes.trackError(e)
        } catch (e: ViolasException.AccountNoActivation) {
            e.printStackTrace()
            sendErrorMessage(
                requestID,
                JsonRpcError.accountNotActivationError("Please check whether the account is activated.")
            )
            Crashes.trackError(e)
        } catch (e: Exception) {
            e.printStackTrace()
            sendInvalidParameterErrorMessage(
                requestID,
                "Transaction resolution failed"
            )
            Crashes.trackError(e)
        }
    }

    private fun sendInvalidParameterErrorMessage(id: Long, msg: String) {
        sendErrorMessage(id, JsonRpcError.invalidParams("Invalid Parameter:$msg"))
    }

    fun <T> sendSuccessMessage(id: Long, result: T): Boolean {
        return try {
            val response = JsonRpcResponse(
                id = id,
                result = result
            )
            val toJson = Gson().toJson(response)
            mWCClient.encryptAndSend(toJson).also {
                restore()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Crashes.trackError(e)
            false
        }
    }

    fun sendErrorMessage(id: Long, result: JsonRpcError): Boolean {
        return try {
            val response = JsonRpcErrorResponse(
                id = id,
                error = result
            )
            val toJson = Gson().toJson(response)
            mWCClient.encryptAndSend(toJson).also {
                restore()
            }
        } catch (e: Exception) {
            Crashes.trackError(e)
            e.printStackTrace()
            false
        }
    }

    fun approveSession(accounts: List<String>, chainId: String): Boolean {
        return try {
            mWCClient.approveSession(accounts, chainId).also {
                restore()
            }
        } catch (e: Exception) {
            Crashes.trackError(e)
            e.printStackTrace()
            false
        }
    }

    fun rejectSession(message: String = "Session rejected"): Boolean {
        return try {
            mWCClient.rejectSession(message).also {
                restore()
            }
        } catch (e: Exception) {
            Crashes.trackError(e)
            e.printStackTrace()
            false
        }
    }

    fun isConnected(): Boolean {
        return mWCClient.isConnected
    }

    fun disconnect(): Boolean {
        return mWCClient.disconnect().also {
            restore()
        }
    }

    /**
     * 传递给转账确认页面的数据类型
     */
    enum class TransactionDataType(val value: Int) {
        Transfer(0), None(1), PUBLISH(2);

        companion object {
            fun decode(value: Int): TransactionDataType {
                return when (value) {
                    Transfer.value -> {
                        Transfer
                    }
                    None.value -> {
                        None
                    }
                    PUBLISH.value -> {
                        PUBLISH
                    }
                    else -> {
                        None
                    }
                }
            }
        }
    }

    data class TransferDataType(
        val form: String,
        val to: String,
        val amount: Long,
        val coinName: String,
        // base64
        val data: String
    )

    data class PublishDataType(
        val form: String
    )

    /**
     * 传递给转账确认页面的数据类型
     */
    data class TransactionSwapVo(
        val requestID: Long,
        val hexTx: String,
        val isSigned: Boolean = true,
        val accountId: Long = -1,
        val viewType: Int,
        val viewData: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readString() ?: "",
            parcel.readByte() != 0.toByte(),
            parcel.readLong(),
            parcel.readInt(),
            parcel.readString() ?: ""
        ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(requestID)
            parcel.writeString(hexTx)
            parcel.writeByte(if (isSigned) 1 else 0)
            parcel.writeLong(accountId)
            parcel.writeInt(viewType)
            parcel.writeString(viewData)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<TransactionSwapVo> {
            override fun createFromParcel(parcel: Parcel): TransactionSwapVo {
                return TransactionSwapVo(parcel)
            }

            override fun newArray(size: Int): Array<TransactionSwapVo?> {
                return arrayOfNulls(size)
            }
        }
    }
}