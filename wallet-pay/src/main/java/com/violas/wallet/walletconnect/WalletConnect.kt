package com.violas.wallet.walletconnect


import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.walletconnect.WCClient
import com.violas.walletconnect.WCSessionStoreItem
import com.violas.walletconnect.WCSessionStoreType
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.jsonrpc.JsonRpcErrorResponse
import com.violas.walletconnect.jsonrpc.JsonRpcResponse
import com.violas.walletconnect.models.violasprivate.WCViolasAccount
import okhttp3.OkHttpClient

class WalletConnect private constructor(val context: Context) {

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

    private val mGsonBuilder = GsonBuilder()
    var mWCClient: WCClient? = null
        private set
    private val httpClient: OkHttpClient = OkHttpClient()
    private val mWCSessionStoreType =
        WCSessionStoreType(WCSessionStoreType.getSharedPreferences(context), mGsonBuilder)

    //    private val mAccountManager by lazy { AccountManager() }
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    fun restore() {
        mWCSessionStoreType
            .session?.let {
                mWCClient = WCClient(httpClient, mGsonBuilder)
                listenerClientEvent()
                mWCClient?.connect(it.session, it.remotePeerMeta, it.peerId, it.remotePeerId)
            }
    }

    private fun listenerClientEvent() {
        mWCClient?.onSessionRequest = { id, peer ->
            mWCClient?.session?.let { session ->
                val wcSessionStoreItem = WCSessionStoreItem(
                    session,
                    mWCClient?.peerId ?: "",
                    mWCClient?.remotePeerId ?: "",
                    peer
                )
                mWCSessionStoreType.session = wcSessionStoreItem
            }
            mWCClient?.approveSession(arrayListOf("account1"), "chainId")
        }
        mWCClient?.onDisconnect = { _, _ ->
            mWCSessionStoreType.session = null
        }
        mWCClient?.onViolasSendRawTransaction = { id, _ ->
            sendSuccessMessage(id, "Success Violas Send Raw Transaction")
        }
        mWCClient?.onViolasSendTransaction = { id, _ ->
            sendSuccessMessage(id, "Success Violas Send Transaction")
        }
        mWCClient?.onViolasSignTransaction = { id, _ ->
            sendSuccessMessage(id, "Success Violas Sign Transaction")
        }
        mWCClient?.onViolasSign = { id, _ ->
            sendSuccessMessage(id, "Success Violas Sign")
        }
        mWCClient?.onGetAccounts = { id ->
            val accounts = mAccountStorage.loadAll().map {
                WCViolasAccount(
                    walletType = it.walletType,
                    coinType = when (it.coinNumber) {
                        CoinTypes.Violas.coinType() -> "violas"
                        CoinTypes.Libra.coinType() -> "libra"
                        else -> "bitcoin"
                    },
                    name = it.walletNickname,
                    address = it.address
                )
            }
            sendSuccessMessage(id, accounts)
        }
    }

    fun <T> sendSuccessMessage(id: Long, result: T) {
        val response = JsonRpcResponse(
            id = id,
            result = result
        )
        val toJson = Gson().toJson(response)
        mWCClient?.encryptAndSend(toJson)
    }

    fun sendErrorMessage(id: Long, result: JsonRpcError) {
        val response = JsonRpcErrorResponse(
            id = id,
            error = result
        )
        val toJson = Gson().toJson(response)
        mWCClient?.encryptAndSend(toJson)
    }

}