package com.violas.wallet.biz.exchangeMapping.transactionProcessor

import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
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

    private val mViolasService by lazy {
        DataRepository.getViolasService()
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
    override fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String {
        val sendAccount = sendAccount as LibraMappingAccount
        val receiveAccount = receiveAccount as ViolasMappingAccount

        var balance = BigDecimal(0)
        val getBalanceCountDownLatch = CountDownLatch(1)
        mLibraService.getBalanceWithCallback(
            sendAccount.getAddress().toHex()
        ) { amount, exception ->
            if (exception == null) {
                balance = BigDecimal(amount)
            }
            getBalanceCountDownLatch.countDown()
        }
        getBalanceCountDownLatch.await()

        if (sendAmount > balance) {
            throw LackOfBalanceException()
        }
        val checkTokenRegister=true
//        val checkTokenRegister = mViolasService.checkTokenRegister(
//            receiveAccount.getAddress().toHex(),
//            receiveAccount.getTokenAddress().toHex()
//            0
//        )

        if (!checkTokenRegister) {
            val publishToken = publishToken(
                Account(
                    KeyPair(
                        receiveAccount.getPrivateKey()!!
                    )
                ),
                //receiveAccount.getTokenAddress().toHex()
                0
            )
            if (!publishToken) {
                throw RuntimeException(
                    getString(
                        R.string.hint_exchange_error
                    )
                )
            }
        }

        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "libra")
        subExchangeDate.put("type", "l2v")
        subExchangeDate.put("to_address", receiveAccount.getAddress().toHex())
        subExchangeDate.put("state", "start")

        val transactionPayload =
            TransactionPayload.optionWithDataPayload(
                ContextProvider.getContext(),
                receiveAddress,
                sendAmount.multiply(BigDecimal("1000000")).toLong(),
                subExchangeDate.toString().toByteArray()
            )

        var sequenceNumber = 0L
        val sequenceNumberCountDownLatch = CountDownLatch(1)
        mLibraService.getSequenceNumberWithCallback(
            sendAccount.getAddress().toHex()
        ) { _sequenceNumber, exception ->
            if (exception == null) {
                sequenceNumber = _sequenceNumber
                sequenceNumberCountDownLatch.countDown()
            } else {
                sequenceNumberCountDownLatch.countDown()
                throw exception
            }
        }
        sequenceNumberCountDownLatch.await()

        val rawTransaction = RawTransaction.optionTransaction(
            sendAccount.getAddress().toHex(),
            transactionPayload,
            sequenceNumber
        )

        val keyPair =
            KeyPair(sendAccount.getPrivateKey()!!)

        var result: Boolean = false
        val countDownLatch = CountDownLatch(1)
        mLibraService.submitTransactionWithCallback(
            rawTransaction,
            keyPair.getPublicKey(),
            keyPair.signMessage(rawTransaction.toHashByteArray())
        ) {
            result = it == null
            countDownLatch.countDown()
        }
        countDownLatch.await()
        if (!result) {
            throw TransferUnknownException()
        }
        return ""
    }

    private fun publishToken(mAccount: Account, tokenAddress: Long): Boolean {
        return true
    }
}