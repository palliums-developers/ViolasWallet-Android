package com.violas.wallet.biz

import android.content.Context
import com.palliums.content.ContextProvider
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.dex.DexOrderDTO
import com.violas.wallet.repository.http.dex.DexRepository
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal

class ExchangeManager {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val receiveAddress = "07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095"

    @Throws(Exception::class)
    suspend fun revokeOrder(
        privateKey: ByteArray,
        dexOrder: DexOrderDTO,
        dexService: DexRepository
    ): Boolean {
        // 1.获取撤销兑换token数据的签名字符
        val account = Account(KeyPair.fromSecretKey(privateKey))

        val sequenceNumber = GlobalScope.async {
            getSequenceNumber(account.getAddress().toHex())
        }
        val optionExchangePayload = TransactionPayload.optionUndoExchangePayload(
            ContextProvider.getContext(),
            receiveAddress,
            if (dexOrder.tokenGiveAddress.startsWith("0x"))
                dexOrder.tokenGiveAddress.replace("0x", "")
            else
                dexOrder.tokenGiveAddress,
            dexOrder.version.toLong()
        )

        val rawTransaction = RawTransaction.optionTransaction(
            account.getAddress().toHex(),
            optionExchangePayload,
            sequenceNumber.await()
        )

        val signedTransaction = SignedTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.signRawTransaction(rawTransaction.toByteArray())
        )
        val signedtxn = signedTransaction.toByteArray().toHex()

        // 2.通知交易中心撤销订单，交易中心此时只会标记需要撤销订单的状态为CANCELLING并停止兑换，失败会抛异常
        dexService.revokeOrder(dexOrder.version, signedtxn)

        // 3.撤销兑换token数据上链，只有上链后，交易中心扫区块扫到解析撤销订单才会更改订单状态为CANCELED
        val channel = Channel<Boolean>()
        mViolasService.sendTransaction(signedtxn) {
            GlobalScope.launch {
                channel.send(it)
                channel.close()
            }
        }
        return channel.receive()
    }

    suspend fun undoExchangeToken(
        account: Account,
        giveTokenAddress: String,
        version: Long
    ): Boolean {
        val sequenceNumber = GlobalScope.async { getSequenceNumber(account.getAddress().toHex()) }
        val optionExchangePayload = TransactionPayload.optionUndoExchangePayload(
            ContextProvider.getContext(),
            receiveAddress,
            if (giveTokenAddress.startsWith("0x"))
                giveTokenAddress.replace("0x", "")
            else
                giveTokenAddress,
            version
        )

        val rawTransaction =
            RawTransaction.optionTransaction(
                account.getAddress().toHex(),
                optionExchangePayload,
                sequenceNumber.await()
            )

        val channel = Channel<Boolean>()
        mViolasService.sendTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.signRawTransaction(rawTransaction.toByteArray())
        ) {
            GlobalScope.launch {
                channel.send(it)
                channel.close()
            }
        }
        return channel.receive()
    }

    suspend fun exchangeToken(
        context: Context,
        account: Account,
        fromCoin: IToken,
        fromCoinAmount: BigDecimal,
        toCoin: IToken,
        toCoinAmount: BigDecimal
    ): Boolean {
        val sequenceNumber = GlobalScope.async { getSequenceNumber(account.getAddress().toHex()) }
        val optionExchangePayload = TransactionPayload.optionExchangePayload(
            context,
            receiveAddress,
            fromCoin.tokenAddress(),
            toCoin.tokenAddress(),
            fromCoinAmount.multiply(BigDecimal("1000000")).toLong(),
            toCoinAmount.multiply(BigDecimal("1000000")).toLong()
        )

        val rawTransaction =
            RawTransaction.optionTransaction(
                account.getAddress().toHex(),
                optionExchangePayload,
                sequenceNumber.await()
            )

        val channel = Channel<Boolean>()
        mViolasService.sendTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.signRawTransaction(rawTransaction.toByteArray())
        ) {
            GlobalScope.launch {
                channel.send(it)
                channel.close()
            }
        }
        return channel.receive()
    }

    private suspend fun getSequenceNumber(address: String): Long {
        val channel = Channel<Long>()
        mViolasService.getSequenceNumber(address, { sequenceNumber ->
            GlobalScope.launch {
                channel.send(sequenceNumber)
            }
        }, {
            GlobalScope.launch {
                channel.send(0L)
                channel.close()
            }
        })
        return channel.receive()
    }
}
