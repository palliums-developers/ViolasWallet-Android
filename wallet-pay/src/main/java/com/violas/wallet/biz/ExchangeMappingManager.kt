package com.violas.wallet.biz

import androidx.annotation.WorkerThread
import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.R
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.btc.outputScript.ViolasOutputScript
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import org.json.JSONObject
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.optionTransaction
import org.palliums.libracore.transaction.optionWithDataPayload
import org.palliums.libracore.wallet.KeyPair
import org.palliums.violascore.transaction.RawTransaction
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.optionTokenWithDataPayload
import org.palliums.violascore.transaction.optionTransaction
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch

// ====== Exchange Pair ========/
interface ExchangeAssert {
    fun getCoinType(): CoinTypes

    fun getName(): String {
        return getCoinType().coinName()
    }
}

interface ExchangeCoin : ExchangeAssert {

}

interface ExchangeToken : ExchangeAssert {
    fun getTokenAddress(): String
}

// ====== Exchange Pair ========/
interface ExchangePair {
    fun getFirst(): ExchangeAssert
    fun getLast(): ExchangeAssert
    fun getRate(): BigDecimal
    fun getReceiveFirstAddress(): String
    fun getReceiveLastAddress(): String
}

class ExchangePairManager() {
    private val mExchangePair = ArrayList<ExchangePair>()

    fun addExchangePair(pair: ExchangePair) {
        mExchangePair.add(pair)
    }

    fun findExchangePair(coinNumber: Int): ExchangePair? {
        mExchangePair.forEach {
            if (it.getFirst().getCoinType().coinType() == coinNumber) {
                return it
            }
        }
        return null
    }
}

// ==== Account Interface ======/
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

// ==== Account Mapping ======/

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

// ==== Transaction Processor Interface ======/

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

// ==== Transaction Processor Impl ======/
/**
 * Violas 代币交易为其他链的币种
 */
class TransactionProcessorViolasTokenToChain() : TransactionProcessor {
    private val mViolasService by lazy {
        DataRepository.getViolasService()
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

        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "violas")
        if (receiveAccount is LibraMappingAccount) {
            subExchangeDate.put("type", "v2l")
            subExchangeDate.put("to_address", receiveAccount.getAddress())
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

        val keyPair = KeyPair(sendAccount.getPrivateKey()!!)

        var result: Boolean = false
        val countDownLatch = CountDownLatch(1)
        mViolasService.sendTransaction(
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
}

class TransactionProcessorBTCTovBTC() : TransactionProcessor {
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

        val subExchangeDate = JSONObject()
        subExchangeDate.put("flag", "libra")
        subExchangeDate.put("type", "l2v")
        subExchangeDate.put("to_address", receiveAccount.getAddress())
        subExchangeDate.put("state", "start")

        val transactionPayload =
            org.palliums.libracore.transaction.TransactionPayload.optionWithDataPayload(
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

        val rawTransaction = org.palliums.libracore.transaction.RawTransaction.optionTransaction(
            sendAccount.getAddress().toHex(),
            transactionPayload,
            sequenceNumber
        )

        val keyPair = KeyPair(sendAccount.getPrivateKey()!!)

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

// ==== Transaction Processor Impl ======/

class ExchangeMappingManager {

    private val mTransactionProcessors = ArrayList<TransactionProcessor>()

    init {
        mTransactionProcessors.add(TransactionProcessorBTCTovBTC())
        mTransactionProcessors.add(TransactionProcessorLibraTovLibra())
        mTransactionProcessors.add(TransactionProcessorViolasTokenToChain())
    }

    fun parseFirstMappingAccount(
        exchangePair: ExchangePair,
        account: AccountDO,
        privateKey: ByteArray? = null
    ): MappingAccount {
        return parseMappingAccount(exchangePair, account, privateKey, true)
    }

    fun parseLastMappingAccount(
        exchangePair: ExchangePair,
        account: AccountDO,
        privateKey: ByteArray? = null
    ): MappingAccount {
        return parseMappingAccount(exchangePair, account, privateKey, false)
    }

    fun parseMappingAccount(
        exchangePair: ExchangePair,
        account: AccountDO,
        privateKey: ByteArray?,
        first: Boolean = true
    ): MappingAccount {
        val exchangeAssert = if (first) {
            exchangePair.getFirst()
        } else {
            exchangePair.getLast()
        }
        return when (exchangeAssert.getCoinType()) {
            CoinTypes.Violas -> {
                ViolasMappingAccount(
                    account.address,
                    (exchangePair.getLast() as ExchangeToken).getTokenAddress(),
                    privateKey
                )
            }
            CoinTypes.Libra -> {
                LibraMappingAccount(
                    account.address,
                    privateKey
                )
            }
            CoinTypes.BitcoinTest,
            CoinTypes.Bitcoin -> {
                BTCMappingAccount(
                    account.publicKey.hexToBytes(),
                    account.address,
                    privateKey
                )
            }
            else -> {
                throw RuntimeException("Unsupported currency")
            }
        }
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
            throw RuntimeException("Unsupported transactions, No TransactionProcessor is available.")
        }
        return transactionProcessor?.handle(sendAccount, receiveAccount, sendAmount, receiveAddress)
    }

    fun getExchangePair(): ExchangePairManager {
        val exchangePairManager = ExchangePairManager()
        val btc2vbtc = object : ExchangePair {
            override fun getFirst(): ExchangeAssert {
                return object : ExchangeCoin {
                    override fun getCoinType(): CoinTypes {
                        return if (Vm.TestNet) {
                            CoinTypes.BitcoinTest
                        } else {
                            CoinTypes.Bitcoin
                        }
                    }
                }
            }

            override fun getLast(): ExchangeAssert {
                return object : ExchangeToken {
                    override fun getTokenAddress(): String {
                        return "af955c1d62a74a7543235dbb7fa46ed98948d2041dff67dfdb636a54e84f91fb"
                    }

                    override fun getCoinType(): CoinTypes {
                        return CoinTypes.Violas
                    }

                    override fun getName(): String {
                        return "vBTC"
                    }
                }
            }

            override fun getRate(): BigDecimal {
                return BigDecimal(1)
            }

            override fun getReceiveFirstAddress(): String {
                return "2MxBZG7295wfsXaUj69quf8vucFzwG35UWh"
            }

            override fun getReceiveLastAddress(): String {
                return "fd0426fa9a3ba4fae760d0f614591c61bb53232a3b1138d5078efa11ef07c49c"
            }
        }

        val libra2vlibra = object : ExchangePair {
            override fun getFirst(): ExchangeAssert {
                return object : ExchangeCoin {
                    override fun getCoinType(): CoinTypes {
                        return CoinTypes.Libra
                    }
                }
            }

            override fun getLast(): ExchangeAssert {
                return object : ExchangeToken {
                    override fun getTokenAddress(): String {
                        return "61b578c0ebaad3852ea5e023fb0f59af61de1a5faf02b1211af0424ee5bbc410"
                    }

                    override fun getCoinType(): CoinTypes {
                        return CoinTypes.Violas
                    }

                    override fun getName(): String {
                        return "vLibra"
                    }
                }
            }

            override fun getRate(): BigDecimal {
                return BigDecimal(1)
            }

            override fun getReceiveFirstAddress(): String {
                return "29223f25fe4b74d75ca87527aed560b2826f5da9382e2fb83f9ab740ac40b8f7"
            }

            override fun getReceiveLastAddress(): String {
                return "fd0426fa9a3ba4fae760d0f614591c61bb53232a3b1138d5078efa11ef07c49c"
            }
        }

        exchangePairManager.addExchangePair(
            btc2vbtc
        )
        exchangePairManager.addExchangePair(
            libra2vlibra
        )
        return exchangePairManager
    }
}

