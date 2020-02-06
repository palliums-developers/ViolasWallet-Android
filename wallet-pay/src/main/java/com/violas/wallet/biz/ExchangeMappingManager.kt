package com.violas.wallet.biz

import androidx.annotation.WorkerThread
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.btc.outputScript.ViolasOutputScript
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex

/**
 * 发送其他平台币的账户
 */
class SendAccount(
    private val sendBTCAddress: String,
    private val sendPrivateKey: ByteArray,
    private val sendPublicKey: ByteArray
) {
    fun getAddress() = sendBTCAddress
    fun getPrivateKey() = sendPrivateKey
    fun getPublicKey() = sendPublicKey
}

/**
 * 接收兑换稳定币的账户
 */
class ReceiveTokenAccount(
    private val toLibraAddress: String,
    private val libraTokenAddress: String = "af955c1d62a74a7543235dbb7fa46ed98948d2041dff67dfdb636a54e84f91fb"
) {
    fun getAddress() = toLibraAddress.hexToBytes()
    fun getTokenAddress() = libraTokenAddress.hexToBytes()
}

class ExchangeMappingManager {

    /**
     * 兑换其他平台币为 Violas 稳定币
     * eg:BTC - vBTC
     */
    @WorkerThread
    suspend fun exchangeBTC2vBTC(
        sendAccount: SendAccount,
        sendAmount: Double,
        receiveBTCAddress: String,
        receiveAccount: ReceiveTokenAccount,
        success: (String) -> Unit,
        error: (Throwable) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            val mTransactionManager: TransactionManager =
                TransactionManager(arrayListOf(sendAccount.getAddress()))
            val checkBalance = mTransactionManager.checkBalance(sendAmount, 3)
            val violasOutputScript = ViolasOutputScript()

            if (!checkBalance) {
                throw LackOfBalanceException()
            }

            mTransactionManager.obtainTransaction(
                sendAccount.getPrivateKey(),
                sendAccount.getPublicKey(),
                checkBalance,
                receiveBTCAddress,
                sendAccount.getAddress(),
                violasOutputScript.requestExchange(
                    receiveAccount.getAddress(),
                    receiveAccount.getTokenAddress()
                )
            ).flatMap {
                try {
                    BitcoinChainApi.get().pushTx(it.signBytes.toHex())
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw TransferUnknownException()
                }
            }.subscribe({
                success.invoke(it)
            }, {
                error.invoke(it)
            }, {
            })
        }
    }

    /**
     * 兑换 Violas 稳定币为其他平台币
     * eg:vBTC - BTC
     */
    suspend fun exchangeNt() {
        withContext(Dispatchers.Main) {

        }
    }
}