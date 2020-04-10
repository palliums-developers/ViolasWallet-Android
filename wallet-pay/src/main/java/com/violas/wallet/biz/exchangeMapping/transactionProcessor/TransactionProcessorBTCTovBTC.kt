package com.violas.wallet.biz.exchangeMapping.transactionProcessor

import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.btc.outputScript.ViolasOutputScript
import com.violas.wallet.biz.exchangeMapping.BTCMappingAccount
import com.violas.wallet.biz.exchangeMapping.MappingAccount
import com.violas.wallet.biz.exchangeMapping.ViolasMappingAccount
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

class TransactionProcessorBTCTovBTC :
    TransactionProcessor {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    override fun dispense(sendAccount: MappingAccount, receiveAccount: MappingAccount): Boolean {
        return sendAccount is BTCMappingAccount
                && sendAccount.isSendAccount()
                && receiveAccount is ViolasMappingAccount
                && receiveAccount.isSendAccount()
    }

    @Throws(Exception::class)
    override fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String {
        val sendAccount = sendAccount as BTCMappingAccount
        val receiveAccount = receiveAccount as ViolasMappingAccount

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

        val mTransactionManager: TransactionManager =
            TransactionManager(arrayListOf(sendAccount.getAddress()))
        val checkBalance = mTransactionManager.checkBalance(sendAmount.toDouble(), 3)
        val violasOutputScript =
            ViolasOutputScript()

        if (!checkBalance) {
            throw LackOfBalanceException()
        }

        var result: String? = ""
        mTransactionManager.obtainTransaction(
            sendAccount.getPrivateKey(),
            sendAccount.getPublicKey(),
            checkBalance,
            receiveAddress,
            sendAccount.getAddress(),
            violasOutputScript.requestExchange(
                receiveAccount.getAddress(),
                receiveAccount.getTokenAddress()
            )
        ).flatMap {
            try {
                BitcoinChainApi.get()
                    .pushTx(it.signBytes.toHex())
            } catch (e: Exception) {
                e.printStackTrace()
                throw TransferUnknownException()
            }
        }.subscribe({
            result = it
        }, {
            throw it
        }, {
        })
        return result ?: ""
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