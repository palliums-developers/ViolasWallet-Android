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
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

class TransactionProcessorLibraTovLibra() :
    TransactionProcessor {
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
        mLibraService.getBalance(sendAccount.getAddress().toHex()) {
            balance = BigDecimal(it)
            getBalanceCountDownLatch.countDown()
        }
        getBalanceCountDownLatch.await()

        if (sendAmount > balance) {
            throw LackOfBalanceException()
        }

        val checkTokenRegister = mViolasService.checkTokenRegister(
            receiveAccount.getAddress().toHex(),
            receiveAccount.getTokenAddress().toHex()
        )

        if (!checkTokenRegister) {
            val publishToken = publishToken(
                Account(
                    KeyPair(
                        receiveAccount.getPrivateKey()!!
                    )
                ),
                receiveAccount.getTokenAddress().toHex()
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
        mLibraService.getSequenceNumber(sendAccount.getAddress().toHex(), {
            sequenceNumber = it
            sequenceNumberCountDownLatch.countDown()
        }, {
            sequenceNumberCountDownLatch.count
            throw it
        })
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
        mLibraService.sendTransaction(
            rawTransaction,
            keyPair.getPublicKey(),
            keyPair.sign(rawTransaction.toByteArray())
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

    private fun publishToken(mAccount: Account, tokenAddress: String): Boolean {
        val countDownLatch = CountDownLatch(1)
        var exec = false
        DataRepository.getViolasService()
            .publishToken(
                ContextProvider.getContext(),
                mAccount,
                tokenAddress
            ) {
                exec = it
                countDownLatch.countDown()
            }
        try {
            countDownLatch.await()
        } catch (e: Exception) {
            exec = false
        }
        return exec
    }
}