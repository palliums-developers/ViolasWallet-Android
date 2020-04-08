package com.violas.wallet.biz.exchangeMapping.transactionProcessor

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.exchangeMapping.BTCMappingAccount
import com.violas.wallet.biz.exchangeMapping.LibraMappingAccount
import com.violas.wallet.biz.exchangeMapping.MappingAccount
import com.violas.wallet.biz.exchangeMapping.ViolasMappingAccount
import com.violas.wallet.repository.DataRepository
import org.json.JSONObject
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.optionTokenWithDataPayload
import org.palliums.violascore.transaction.optionTransaction
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

/**
 * Violas 代币交易为其他链的币种
 */
class TransactionProcessorViolasTokenToChain : TransactionProcessor {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }
    private val mTokenManager by lazy {
        TokenManager()
    }

    override fun dispense(sendAccount: MappingAccount, receiveAccount: MappingAccount): Boolean {
        return sendAccount is ViolasMappingAccount
                && sendAccount.isSendAccount()
                && ((receiveAccount is BTCMappingAccount) or (receiveAccount is LibraMappingAccount))
    }

    override fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String {
        val sendAccount = sendAccount as ViolasMappingAccount

        var balance = BigDecimal(0)
        val getBalanceCountDownLatch = CountDownLatch(1)
        mViolasService.getBalance(
            sendAccount.getAddress().toHex(),
            arrayListOf(sendAccount.getTokenAddress().toHex())
        ) { _, tokens, result ->
            if (result) {
                tokens?.forEach {
                    if (it.address == sendAccount.getTokenAddress().toHex()) {
                        balance = BigDecimal(it.balance)
                    }
                }
            }
            getBalanceCountDownLatch.countDown()
        }
        getBalanceCountDownLatch.await()

        if (sendAmount.multiply(BigDecimal("1000000")) > balance) {
            throw LackOfBalanceException()
        }

        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "violas")
        if (receiveAccount is LibraMappingAccount) {
            subExchangeDate.put("type", "v2l")
            subExchangeDate.put("to_address", receiveAccount.getAddress().toHex())
        } else if (receiveAccount is BTCMappingAccount) {
            subExchangeDate.put("type", "v2b")
            subExchangeDate.put("to_address", receiveAccount.getAddress())
        }
        subExchangeDate.put("state", "start")


        val transactionPayload = TransactionPayload.optionTokenWithDataPayload(
            ContextProvider.getContext(),
            receiveAddress,
            sendAmount.multiply(BigDecimal("1000000")).toLong(),
            sendAccount.getTokenAddress().toHex(),
            subExchangeDate.toString().toByteArray()
        )

        val sequenceNumber = mViolasService.getSequenceNumber(sendAccount.getAddress().toHex())

        val rawTransaction = RawTransaction.optionTransaction(
            sendAccount.getAddress().toHex(),
            transactionPayload,
            sequenceNumber
        )

        val keyPair =
            KeyPair(sendAccount.getPrivateKey()!!)

        var result: Boolean = false
        val countDownLatch = CountDownLatch(1)
        mViolasService.sendTransaction(
            rawTransaction,
            keyPair.getPublicKey(),
            keyPair.signRawTransaction(rawTransaction.toByteArray())
        ) {
            result = it
            countDownLatch.countDown()
        }
        countDownLatch.await()
        if (!result) {
            throw TransferUnknownException()
        }
        return ""
    }
}