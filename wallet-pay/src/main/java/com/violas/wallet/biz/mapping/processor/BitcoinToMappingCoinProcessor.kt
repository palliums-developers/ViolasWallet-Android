package com.violas.wallet.biz.mapping.processor

import com.palliums.content.ContextProvider
import com.palliums.net.await
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.exchange.processor.ViolasOutputScript
import com.violas.wallet.biz.mapping.PayeeAccountCoinNotActiveException
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.bitcoinChainApi.request.BitcoinChainApi
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.extensions.toHex
import org.palliums.violascore.http.ViolasRpcService

/**
 * Created by elephant on 2020/8/13 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BitcoinToMappingCoinProcessor(
    private val contractAddress: String,
    private val violasRpcService: ViolasRpcService
) : MappingProcessor {

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        return str2CoinType(coinPair.fromCoin.chainName) == getBitcoinCoinType()
                && str2CoinType(coinPair.toCoin.chainName) == getViolasCoinType()
    }

    override suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAccountDO: AccountDO,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        if (checkPayeeAccount) {
            // 检查收款账户激活状态
            val payeeAccountState =
                violasRpcService.getAccountState(payeeAccountDO.address)
                    ?: throw AccountPayeeNotFindException()

            // 检查收款账户 Token 注册状态
            var isPublishToken = false
            payeeAccountState.balances?.forEach {
                if (it.currency.equals(coinPair.toCoin.assets.module, true)) {
                    isPublishToken = true
                }
            }
            if (!isPublishToken) {
                throw PayeeAccountCoinNotActiveException(
                    payeeAccountDO,
                    coinPair.toCoin.assets
                )
            }
        }

        val payerPrivateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey)!!

        val transactionManager: TransactionManager =
            TransactionManager(arrayListOf(payerAccountDO.address))
        val checkBalance =
            transactionManager.checkBalance(amount / 100000000.0, 3)
        if (!checkBalance) {
            throw LackOfBalanceException()
        }

        return transactionManager.obtainTransaction(
            payerPrivateKey,
            payerAccountDO.publicKey.hexStringToByteArray(),
            checkBalance,
            coinPair.receiverAddress,
            payerAccountDO.address,
            ViolasOutputScript().requestMapping(
                coinPair.mappingType,
                payeeAccountDO.address.hexStringToByteArray(),
                contractAddress.hexStringToByteArray()
            )
        ).flatMap {
            BitcoinChainApi.get().pushTx(it.signBytes.toHex())
        }.await()
    }
}