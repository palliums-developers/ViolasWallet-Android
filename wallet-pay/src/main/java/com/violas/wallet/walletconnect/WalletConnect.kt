package com.violas.wallet.walletconnect


import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.appcenter.crashes.Crashes
import com.violas.wallet.walletconnect.walletConnectMessageHandler.IWalletConnectMessage
import com.violas.wallet.walletconnect.walletConnectMessageHandler.WalletConnectMessageHandler
import com.violas.walletconnect.WCClient
import com.violas.walletconnect.WCSessionStoreItem
import com.violas.walletconnect.WCSessionStoreType
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.jsonrpc.JsonRpcErrorResponse
import com.violas.walletconnect.jsonrpc.JsonRpcResponse
import com.violas.walletconnect.models.WCPeerMeta
import com.violas.walletconnect.models.session.WCSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

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

class WalletConnect private constructor(val context: Context) : CoroutineScope by MainScope(),
    IWalletConnectMessage {

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
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(3,TimeUnit.MINUTES)
            .build()
    }
    private val mWCClient: WCClient = WCClient(httpClient, mGsonBuilder)
    private val mWCSessionStoreType =
        WCSessionStoreType(WCSessionStoreType.getSharedPreferences(context), mGsonBuilder)

    private val mWalletConnectMessageHandler by lazy {
        WalletConnectMessageHandler(context, this)
    }

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

    fun restore() {
        launch(Dispatchers.IO) {
            Log.e("Wallet Connwct", "Restore Connect")
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
        mWCClient.onFailure = { throwable ->
            throwable.printStackTrace()
        }
        mWCClient.onDisconnect = { _, _ ->
            disconnectAndReset()
        }
        mWCClient.onViolasSendRawTransaction = { id, violasSendRawTransaction ->
            mWalletConnectMessageHandler.convertAndCheckTransaction(id, violasSendRawTransaction)
        }
        mWCClient.onViolasSendTransaction = { id, violasSendTransaction ->
            mWalletConnectMessageHandler.convertAndCheckTransaction(id, violasSendTransaction)
        }
        mWCClient.onViolasSignTransaction = { id, violasSignRawTransaction ->
            mWalletConnectMessageHandler.convertAndCheckTransaction(id, violasSignRawTransaction)
        }
        mWCClient.onGetAccounts = { id ->
            mWalletConnectMessageHandler.handlerGetAccounts(id)
        }
        mWCClient.onLibraSendTransaction = { id, libraSendTransaction ->
            mWalletConnectMessageHandler.convertAndCheckTransaction(id, libraSendTransaction)
        }
        mWCClient.onBitcoinSendTransaction = { id, bitcoinSendTransaction ->
            mWalletConnectMessageHandler.convertAndCheckTransaction(id, bitcoinSendTransaction)
        }
    }

    private fun disconnectAndReset() {
        mWCSessionStoreType.session = null
        mWalletConnectListener?.onDisconnect()
    }

    override fun <T> sendSuccessMessage(id: Long, result: T): Boolean {
        return try {
            val response = JsonRpcResponse(
                id = id,
                result = result
            )
            val toJson = Gson().toJson(response)
            mWCClient.encryptAndSend(toJson).also {
                if (!it) {
                    restore()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Crashes.trackError(e)
            false
        }
    }

    override fun sendErrorMessage(id: Long, result: JsonRpcError): Boolean {
        return try {
            val response = JsonRpcErrorResponse(
                id = id,
                error = result
            )
            val toJson = Gson().toJson(response)
            mWCClient.encryptAndSend(toJson).also {
                if (!it) {
                    restore()
                }
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
                if (!it) {
                    restore()
                }
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
                if (!it) {
                    restore()
                }
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
        return if (mWCSessionStoreType.session != null) {
            val killSession = mWCClient.killSession()
            disconnectAndReset()
            killSession
        } else {
            try {
                mWCClient.disconnect()
            } catch (e: Exception) {
            }
            true
        }
    }
}