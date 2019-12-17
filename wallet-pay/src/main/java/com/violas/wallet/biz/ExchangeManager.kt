package com.violas.wallet.biz

import android.content.Context
import com.palliums.content.ContextProvider
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.quotes.bean.IToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.utils.HexUtils
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.*

class ExchangeManager {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    private val receiveAddress = "07e92f79c67fdd6b80ed9103636a49511363de8c873bc709966fffb2e3fcd095"

    suspend fun undoExchangeToken(
        account: Account,
        giveTokenAddress: String,
        version: Long
    ): Boolean {
        val sequenceNumber = GlobalScope.async { getSequenceNumber(account.getAddress().toHex()) }
        val optionExchangePayload = optionUndoExchangePayload(
            receiveAddress,
            if (giveTokenAddress.startsWith("0x"))
                giveTokenAddress.replace("0x", "")
            else
                giveTokenAddress,
            version
        )

        val rawTransaction =
            optionTransaction(account, optionExchangePayload, sequenceNumber.await())

        val channel = Channel<Boolean>()
        mViolasService.sendTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.sign(rawTransaction.toByteArray())
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
        val optionExchangePayload = optionExchangePayload(
            context,
            receiveAddress,
            fromCoin.tokenAddress(),
            toCoin.tokenAddress(),
            fromCoinAmount.multiply(BigDecimal("1000000")).toLong(),
            toCoinAmount.multiply(BigDecimal("1000000")).toLong()
        )

        val rawTransaction =
            optionTransaction(account, optionExchangePayload, sequenceNumber.await())

        val channel = Channel<Boolean>()
        mViolasService.sendTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.sign(rawTransaction.toByteArray())
        ) {
            GlobalScope.launch {
                channel.send(it)
                channel.close()
            }
        }
        return channel.receive()
    }

    private fun optionUndoExchangePayload(
        receiveAddress: String,
        sendTokenAddress: String,
        version: Long
    ): TransactionPayload {
        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "wd_ex")
        subExchangeDate.put("ver", version)

        val addressArgument = TransactionArgument.newAddress(receiveAddress)
        val amountArgument = TransactionArgument.newU64(0)
        val dateArgument =
            TransactionArgument.newByteArray(subExchangeDate.toString().toByteArray())

        val moveEncode = Move.violasTokenEncode(
            ContextProvider.getContext().assets.open("move/transfer_with_data.json"),
            sendTokenAddress.hexToBytes()
        )

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(addressArgument, amountArgument, dateArgument)
            )
        )
    }

    private fun optionExchangePayload(
        context: Context,
        receiveAddress: String,
        sendTokenAddress: String,
        exchangeTokenAddress: String,
        exchangeSendAmount: Long,
        exchangeReceiveAmount: Long
    ): TransactionPayload {

        val subExchangeDate = JSONObject()
        subExchangeDate.put("type", "sub_ex")
        subExchangeDate.put("addr", exchangeTokenAddress)
        subExchangeDate.put("amount", exchangeReceiveAmount)
        subExchangeDate.put("fee", 0)
        subExchangeDate.put("exp", 1000)

        val addressArgument = TransactionArgument.newAddress(receiveAddress)
        val amountArgument = TransactionArgument.newU64(exchangeSendAmount)
        val dateArgument =
            TransactionArgument.newByteArray(subExchangeDate.toString().toByteArray())

        val moveEncode = Move.violasTokenEncode(
            context.assets.open("move/transfer_with_data.json"),
            sendTokenAddress.hexToBytes()
        )

        return TransactionPayload(
            TransactionPayload.Script(
                moveEncode,
                arrayListOf(addressArgument, amountArgument, dateArgument)
            )
        )
    }

    private fun optionTransaction(
        account: Account,
        payload: TransactionPayload,
        sequenceNumber: Long,
        maxGasAmount: Long = 280_000,
        gasUnitPrice: Long = 0,
        expiration: Long = 1000
    ): RawTransaction {
        val senderAddress = account.getAddress().toHex()
        val rawTransaction = RawTransaction(
            AccountAddress(HexUtils.fromHex(senderAddress)),
            sequenceNumber,
            payload,
            maxGasAmount,
            gasUnitPrice,
            (Date().time / 1000) + expiration
        )

        println("rawTransaction ${HexUtils.toHex(rawTransaction.toByteArray())}")
        return rawTransaction
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
