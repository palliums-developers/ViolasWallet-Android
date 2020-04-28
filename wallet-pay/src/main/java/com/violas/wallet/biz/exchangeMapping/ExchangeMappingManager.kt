package com.violas.wallet.biz.exchangeMapping

import com.palliums.utils.exceptionAsync
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessor
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessorBTCTovBTC
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessorLibraTovLibra
import com.violas.wallet.biz.exchangeMapping.transactionProcessor.TransactionProcessorViolasTokenToChain
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mappingExchange.MappingType
import kotlinx.coroutines.*
import org.palliums.libracore.serialization.hexToBytes
import java.lang.Exception
import java.math.BigDecimal

class ExchangeMappingManager {

    private val mMappingExchangeService by lazy {
        DataRepository.getMappingExchangeService()
    }
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
                    (exchangePair.getLast() as ExchangeToken).getTokenIdx(),
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

    private suspend fun dispenseTransfer(
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

    suspend fun getExchangePair(): ExchangePairManager {
        val exchangePairManager = ExchangePairManager()

        return coroutineScope {
            val btc2vbtc = exceptionAsync { loadBTC2vBTC() }
            val libra2vLibra = exceptionAsync { loadLibra2vLibra() }

            btc2vbtc.await()?.let {
                exchangePairManager.addExchangePair(it)
            }
            libra2vLibra.await()?.let {
                exchangePairManager.addExchangePair(it)
            }

            exchangePairManager
        }
    }

    // update 2020.04.24
    //
    // vbtc映射btc时接收vbtc的地址（violas地址）：
    // cae5f8464c564aabb684ecbcc19153e9
    //
    // btc映射vbtc时接收btc的地址（比特币地址）：
    // 2N2YasTUdLbXsafHHmyoKUYcRRicRPgUyNB
    //
    // libra映射vlibra时接收libra的地址（libra地址)：
    // 0a82179351b8ecb6c5e68ab7b08622de
    //
    // vlibra映射libra时接收vlibra的地址（violas地址）
    // ee1e24e8fc664894709c947b74823b2f
    //
    // violas上用的module：
    // 00000000000000000000000000000000e1be1ab8360a35a0259f1c93e3eac736
    //
    // vbtc token_id: 2
    // vlibra token_id: 3
    //之前提供过类似接口（没有token_id),具体可用跟钱包端沟通，
    private suspend fun loadLibra2vLibra(): ExchangePair? {
        val libraToVLibraInfoDeferred =
            GlobalScope.exceptionAsync { mMappingExchangeService.getMappingInfo(MappingType.LibraToVlibra) }
        val vLibraToLibraInfoDeferred =
            GlobalScope.exceptionAsync { mMappingExchangeService.getMappingInfo(MappingType.VlibraToLibra) }

        val libraToVLibraInfo = libraToVLibraInfoDeferred.await().data
        val vLibraToLibraInfo = vLibraToLibraInfoDeferred.await().data

        if (libraToVLibraInfo == null || vLibraToLibraInfo == null) {
            return null
        }

        return object :
            ExchangePair {
            override fun getFirst() = ExchangeCoinImpl(CoinTypes.Libra)

            override fun getLast() = ExchangeTokenImpl(
                CoinTypes.Violas,
                "vLibra",
//                "61b578c0ebaad3852ea5e023fb0f59af61de1a5faf02b1211af0424ee5bbc410"
//                LibraToVlibraInfo.tokenAddress
//                0
            3
            )

            override fun getRate(): BigDecimal {
                return BigDecimal(1)
            }

            override fun getReceiveFirstAddress(): String {
                return "0a82179351b8ecb6c5e68ab7b08622de"
//                return "29223f25fe4b74d75ca87527aed560b2826f5da9382e2fb83f9ab740ac40b8f7"
//                return libraToVLibraInfo.receiveAddress
            }

            override fun getReceiveLastAddress(): String {
                return "ee1e24e8fc664894709c947b74823b2f"
//                return "fd0426fa9a3ba4fae760d0f614591c61bb53232a3b1138d5078efa11ef07c49c"
//                return vLibraToLibraInfo.receiveAddress
            }
        }
    }

    private suspend fun loadBTC2vBTC(): ExchangePair? {
        val BTCToVbtcInfoDeferred =
            GlobalScope.exceptionAsync { mMappingExchangeService.getMappingInfo(MappingType.BTCToVbtc) }
        val VbtcToBTCInfoDeferred =
            GlobalScope.exceptionAsync { mMappingExchangeService.getMappingInfo(MappingType.VbtcToBTC) }

        val BTCToVbtcInfo = BTCToVbtcInfoDeferred.await().data
        val VbtcToBTCInfo = VbtcToBTCInfoDeferred.await().data

        if (BTCToVbtcInfo == null || VbtcToBTCInfo == null) {
            return null
        }

        return object :
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
//                "2236322cf1e35198302919c2c1b1e4bf5be07359c8995c6a13ec53c17579c768"
//                BTCToVbtcInfo.tokenAddress
//                1
                2
            )

            override fun getRate(): BigDecimal {
//                return BigDecimal(1)
                return BigDecimal(BTCToVbtcInfo.exchangeRate.toString())
            }

            override fun getReceiveFirstAddress(): String {
                return "2N2YasTUdLbXsafHHmyoKUYcRRicRPgUyNB"
//                return "2MxBZG7295wfsXaUj69quf8vucFzwG35UWh"
//                return BTCToVbtcInfo.receiveAddress
            }

            override fun getReceiveLastAddress(): String {
                return "cae5f8464c564aabb684ecbcc19153e9"
//                return "fd0426fa9a3ba4fae760d0f614591c61bb53232a3b1138d5078efa11ef07c49c"
//                return VbtcToBTCInfo.receiveAddress
            }
        }

    }
}

