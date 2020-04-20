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

    override suspend fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String {
        val sendAccount = sendAccount as ViolasMappingAccount

        var balance = mTokenManager.getTokenBalance(
            sendAccount.getAddress().toHex(),
            sendAccount.getTokenIdx()
        ).let { BigDecimal(it) }

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


        val transactionPayload = mTokenManager.transferTokenPayload(
            sendAccount.getTokenIdx(),
            receiveAddress,
            sendAmount.multiply(BigDecimal("1000000")).toLong(),
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
        
        mViolasService.sendTransaction(
            rawTransaction,
            keyPair.getPublicKey(),
            keyPair.signMessage(rawTransaction.toHashByteArray())
        )
        return ""
    }
}