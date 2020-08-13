package com.violas.wallet.biz.mapping.processor

import com.palliums.net.await
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.TransferUnknownException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.processor.ViolasOutputScript
import com.violas.wallet.biz.mapping.PayeeAccountCoinNotActiveException
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex

/**
 * Created by elephant on 2020/8/13 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinToMappingCoinProcessor(
    private val contractAddress: String
) : MappingProcessor {

    private val violasRpcService by lazy {
        DataRepository.getViolasChainRpcService()
    }

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        return str2CoinType(coinPair.fromCoin.chainName) ==
                (if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin)
                && str2CoinType(coinPair.toCoin.chainName) == CoinTypes.Violas
    }

    override suspend fun mapping(
        payerAccount: AccountDO,
        payerPrivateKey: ByteArray,
        payeeAddress: String,
        coinPair: MappingCoinPairDTO,
        amount: Long
    ): String {
        // 检查收款地址激活状态
        val payeeAccountState = violasRpcService.getAccountState(payeeAddress)
            ?: throw AccountPayeeNotFindException()

        // 检查收款地址 Token 注册状态
        var isPublishToken = false
        payeeAccountState.balances?.forEach {
            if (it.currency.equals(coinPair.toCoin.assets.module, true)) {
                isPublishToken = true
            }
        }
        if (!isPublishToken) {
            throw PayeeAccountCoinNotActiveException(
                CoinTypes.Violas,
                payeeAddress,
                coinPair.toCoin.assets
            )
        }

        val transactionManager: TransactionManager =
            TransactionManager(arrayListOf(payerAccount.address))
        val checkBalance =
            transactionManager.checkBalance(amount / 100000000.0, 3)
        if (!checkBalance) {
            throw LackOfBalanceException()
        }

        return transactionManager.obtainTransaction(
            payerPrivateKey,
            payerAccount.publicKey.hexStringToByteArray(),
            checkBalance,
            coinPair.receiverAddress,
            payerAccount.address,
            ViolasOutputScript().requestExchange(
                coinPair.mappingType,
                payeeAddress.hexStringToByteArray(),
                contractAddress.hexStringToByteArray(),
                amount
            )
        ).flatMap {
            try {
                BitcoinChainApi.get().pushTx(it.signBytes.toHex())
            } catch (e: Exception) {
                e.printStackTrace()
                throw TransferUnknownException()
            }
        }.await()
    }
}