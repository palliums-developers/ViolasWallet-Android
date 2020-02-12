package com.violas.wallet.biz

import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.btc.outputScript.ViolasOutputScript
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

interface BTCAccount {
    fun getAddress(): String
    fun getPublicKey(): ByteArray
}

interface LibraAccount {
    fun getAddress(): ByteArray
}

interface ViolasAccount {
    fun getAddress(): ByteArray
    fun getTokenAddress(): ByteArray
}

abstract class MappingAccount(private val sendPrivateKey: ByteArray? = null) {
    fun getPrivateKey() = sendPrivateKey

    fun isSendAccount() = getPrivateKey() != null
}

class BTCMappingAccount(
    private val publicKey: ByteArray,
    private val BTCAddress: String,
    privateKey: ByteArray? = null
) : MappingAccount(privateKey), BTCAccount {
    override fun getAddress() = BTCAddress

    override fun getPublicKey() = publicKey
}

class LibraMappingAccount(
    private val address: String,
    privateKey: ByteArray? = null

) : MappingAccount(privateKey), LibraAccount {
    override fun getAddress() = address.hexToBytes()
}

class ViolasMappingAccount(
    private val address: String,
    private val tokenAddress: String,
    privateKey: ByteArray? = null
) : MappingAccount(privateKey), ViolasAccount {
    override fun getAddress() = address.hexToBytes()
    override fun getTokenAddress() = tokenAddress.hexToBytes()
}

interface TransactionProcessor {
    fun dispense(sendAccount: MappingAccount, receiveAccount: MappingAccount): Boolean

    @Throws(Exception::class)
    fun handle(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String
}

class TransactionProcessorBTCTovBTC() : TransactionProcessor {
    private val mViolasService = DataRepository.getViolasService()

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
                Account(KeyPair(receiveAccount.getPrivateKey()!!)),
                receiveAccount.getTokenAddress().toHex()
            )
            if (!publishToken) {
                throw RuntimeException(getString(R.string.hint_exchange_error))
            }
        }

        val mTransactionManager: TransactionManager =
            TransactionManager(arrayListOf(sendAccount.getAddress()))
        val checkBalance = mTransactionManager.checkBalance(sendAmount.toDouble(), 3)
        val violasOutputScript = ViolasOutputScript()

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
                BitcoinChainApi.get().pushTx(it.signBytes.toHex())
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

class ExchangeMappingManager {

    private val mTransactionProcessors = ArrayList<TransactionProcessor>()

    init {
        mTransactionProcessors.add(TransactionProcessorBTCTovBTC())
    }

    suspend fun exchangeMapping(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String? {
       return dispenseTransfer(sendAccount, receiveAccount, sendAmount, receiveAddress)
    }

    private fun dispenseTransfer(
        sendAccount: MappingAccount,
        receiveAccount: MappingAccount,
        sendAmount: BigDecimal,
        receiveAddress: String
    ): String? {
        var transactionProcessor: TransactionProcessor? = null
        mTransactionProcessors.forEach {
            if (it.dispense(sendAccount, receiveAccount)) {
                transactionProcessor = it
                return@forEach
            }
        }
        if (transactionProcessor == null) {
            throw RuntimeException("不支持的转账")
        }
        return transactionProcessor?.handle(sendAccount, receiveAccount, sendAmount, receiveAddress)
    }
}