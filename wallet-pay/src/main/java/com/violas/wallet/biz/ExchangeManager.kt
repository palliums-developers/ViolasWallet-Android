package com.violas.wallet.biz

import android.content.Context
import com.palliums.utils.exceptionAsync
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexRepository
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.coroutines.GlobalScope
import org.json.JSONObject
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.SignedTransaction
import org.palliums.violascore.transaction.TransactionSignAuthenticator
import org.palliums.violascore.transaction.optionTransaction
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal

class ExchangeManager {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val mTokenManager by lazy {
        TokenManager()
    }

    private val receiveAddress = "c71caa520e123d122c310177c08fa0d2"

    @Throws(Exception::class)
    suspend fun revokeOrder(
        privateKey: ByteArray,
        dexOrder: DexOrderDTO,
        dexService: DexRepository
    ): Boolean {
        return try {
            // 1.获取撤销兑换token数据的签名字符
            val account = Account(KeyPair.fromSecretKey(privateKey))

            val sequenceNumber = GlobalScope.exceptionAsync {
                getSequenceNumber(account.getAddress().toHex())
            }

            val optionUndoExchangePayloadWithData =
                optionUndoExchangePayloadWithData(dexOrder.version.toLong())

            val optionUndoExchangePayload = mTokenManager.transferTokenPayload(
                dexOrder.tokenGiveAddress.toLong(),
                receiveAddress,
                0,
                optionUndoExchangePayloadWithData
            )

            val rawTransaction = RawTransaction.optionTransaction(
                account.getAddress().toHex(),
                optionUndoExchangePayload,
                sequenceNumber.await()
            )

            val signedTransaction = SignedTransaction(
                rawTransaction,
                TransactionSignAuthenticator(
                    account.keyPair.getPublicKey(),
                    account.keyPair.signMessage(rawTransaction.toHashByteArray())
                )
            )
            val signedtxn = signedTransaction.toByteArray().toHex()

            // 2.通知交易中心撤销订单，交易中心此时只会标记需要撤销订单的状态为CANCELLING并停止兑换，失败会抛异常
            // 不管通知交易中心撤销订单有没有成功，都要将撤销兑换token数据上链
            try {
                dexService.revokeOrder(dexOrder.version, signedtxn)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3.撤销兑换token数据上链，只有上链后，交易中心扫区块扫到解析撤销订单才会更改订单状态为CANCELED
            mViolasService.sendTransaction(signedTransaction)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exchangeToken(
        context: Context,
        account: Account,
        fromCoin: IToken,
        fromCoinAmount: BigDecimal,
        toCoin: IToken,
        toCoinAmount: BigDecimal
    ): Boolean {
        val sequenceNumber = GlobalScope.exceptionAsync {
            getSequenceNumber(account.getAddress().toHex())
        }

        val optionExchangePayloadWithData = optionExchangePayloadWithData(
            toCoin.tokenIdx(),
            toCoinAmount.multiply(BigDecimal("1000000")).toLong()
        )
        val optionExchangePayload = mTokenManager.transferTokenPayload(
            fromCoin.tokenIdx(),
            receiveAddress,
            fromCoinAmount.multiply(BigDecimal("1000000")).toLong(),
            optionExchangePayloadWithData
        )

        try {
            val rawTransaction = RawTransaction.optionTransaction(
                account.getAddress().toHex(),
                optionExchangePayload,
                sequenceNumber.await()
            )

            mViolasService.sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private suspend fun getSequenceNumber(address: String): Long {
        return mViolasService.getSequenceNumber(address)
    }

    private fun optionExchangePayloadWithData(
        exchangeTokenIdx: Long,
        exchangeReceiveAmount: Long
    ): ByteArray {
        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "sub_ex")
        subExchangeDate.put("tokenid", exchangeTokenIdx)
        subExchangeDate.put("amount", exchangeReceiveAmount)
        subExchangeDate.put("fee", 0)
        subExchangeDate.put("exp", 1000)
        return subExchangeDate.toString().toByteArray()
    }

    private fun optionUndoExchangePayloadWithData(
        version: Long
    ): ByteArray {
        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "wd_ex")
        subExchangeDate.put("ver", version)
        return subExchangeDate.toString().toByteArray()
    }
}
