package com.violas.wallet.biz.exchangeMapping

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessor
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessorBTCTovBTC
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessorLibraTovLibra
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessorViolasTokenToChain
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.entity.AccountDO
import org.palliums.libracore.serialization.hexToBytes
import java.math.BigDecimal

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
                    (exchangePair.getLast() as ExchangeToken).getName(),
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
        val btc2vbtc = object :
            ExchangePair {
            override fun getFirst() = ExchangeCoinImpl(
                if (Vm.TestNet) {
                    CoinTypes.BitcoinTest
                } else {
                    CoinTypes.Bitcoin
                }
            )

            override fun getLast() = ExchangeTokenImpl(
                CoinTypes.Violas,
                "vBTC",
                "af955c1d62a74a7543235dbb7fa46ed98948d2041dff67dfdb636a54e84f91fb"
            )

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

        val libra2vLibra = object :
            ExchangePair {
            override fun getFirst() = ExchangeCoinImpl(CoinTypes.Libra)

            override fun getLast() = ExchangeTokenImpl(
                CoinTypes.Violas,
                "vLibra",
                "61b578c0ebaad3852ea5e023fb0f59af61de1a5faf02b1211af0424ee5bbc410"
            )

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
            libra2vLibra
        )
        return exchangePairManager
    }
}

