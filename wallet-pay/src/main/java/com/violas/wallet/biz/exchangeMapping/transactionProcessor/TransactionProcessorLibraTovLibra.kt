package com.violas.wallet.biz.exchangeMapping.transactionProcessor

import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.exchangeMapping.LibraMappingAccount
import com.violas.wallet.biz.exchangeMapping.MappingAccount
import com.violas.wallet.biz.exchangeMapping.ViolasMappingAccount
import com.violas.wallet.repository.DataRepository
import org.json.JSONObject
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.RawTransaction
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.optionTransaction
import org.palliums.libracore.transaction.optionWithDataPayload
import org.palliums.violascore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

class TransactionProcessorLibraTovLibra() : TransactionProcessor {
    private val mTokenManager by lazy {
        TokenManager()
    }
    private val mLibraService by lazy {
        DataRepository.getLibraService()
    }

    override fun dispense(sendAccount: MappingAccount, receiveAccount: MappingAccount): Boolean {
        return sendAccount is LibraMappingAccount
                && sendAccount.isSendAccount()
                && receiveAccount is ViolasMappingAccount
                && receiveAccount.isSendAccount()
    }

    @WorkerThread
    @Throws(Exception::class)
    override suspend fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String {
        val sendAccount = sendAccount as LibraMappingAccount
        val receiveAccount = receiveAccount as ViolasMappingAccount

        var balance = mLibraService.getBalance(
            sendAccount.getAddress().toHex()
        ).let { BigDecimal(it) }

        if (sendAmount > balance) {
            throw LackOfBalanceException()
        }
        val checkTokenRegister = mTokenManager.isPublish(receiveAccount.getAddress().toHex())

        if (!checkTokenRegister) {
            try {
                publishToken(
                    Account(KeyPair(receiveAccount.getPrivateKey()!!))
                )
            } catch (e: Exception) {
                throw RuntimeException(
                    getString(R.string.hint_exchange_error)
                )
            }
        }
        val authKeyPrefix = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "libra")
        subExchangeDate.put("type", "l2v")
        subExchangeDate.put("to_address", (authKeyPrefix + receiveAccount.getAddress()).toHex())
        subExchangeDate.put("state", "start")

        val transactionPayload =
            TransactionPayload.optionWithDataPayload(
                ContextProvider.getContext(),
                receiveAddress,
                sendAmount.multiply(BigDecimal("1000000")).toLong(),
                subExchangeDate.toString().toByteArray()
            )

        val sequenceNumber = mLibraService.getSequenceNumber(sendAccount.getAddress().toHex())

        val rawTransaction = RawTransaction.optionTransaction(
            sendAccount.getAddress().toHex(),
            transactionPayload,
            sequenceNumber
        )

        val keyPair =
            KeyPair(sendAccount.getPrivateKey()!!)

        mLibraService.submitTransaction(
            rawTransaction,
            keyPair.getPublicKey(),
            keyPair.signMessage(rawTransaction.toHashByteArray())
        )
        return ""
    }

    private suspend fun publishToken(mAccount: Account) {
        return mTokenManager.publishToken(mAccount)
    }
}