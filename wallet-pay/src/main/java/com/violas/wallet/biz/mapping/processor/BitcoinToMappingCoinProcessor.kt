package com.violas.wallet.biz.mapping.processor

import com.palliums.content.ContextProvider
import com.palliums.net.await
import com.violas.wallet.biz.LackOfBalanceException
import com.violas.wallet.biz.WrongPasswordException
import com.violas.wallet.biz.bean.DiemAppToken
import com.violas.wallet.biz.btc.TransactionManager
import com.violas.wallet.biz.exchange.processor.ViolasOutputScript
import com.violas.wallet.biz.transaction.ViolasTxnManager
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
    private val mContractAddress: String,
    private val mViolasRpcService: ViolasRpcService
) : MappingProcessor {

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        return str2CoinType(coinPair.fromCoin.chainName) == getBitcoinCoinType()
                && str2CoinType(coinPair.toCoin.chainName) == getViolasCoinType()
    }

    override suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        mappingAmount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        if (checkPayeeAccount) {
            // 检查Violas收款人账户
            ViolasTxnManager().getReceiverAccountState(
                payeeAddress ?: payeeAccountDO!!.address,
                DiemAppToken.convert(coinPair.toCoin.assets)
            ) {
                mViolasRpcService.getAccountState(it)
            }
        }

        val privateKey = SimpleSecurity.instance(ContextProvider.getContext())
            .decrypt(password, payerAccountDO.privateKey) ?: throw WrongPasswordException()

        val transactionManager = TransactionManager(arrayListOf(payerAccountDO.address))
        val checkBalance =
            transactionManager.checkBalance(mappingAmount / 100000000.0, 3)
        if (!checkBalance) {
            throw LackOfBalanceException()
        }

        return transactionManager.obtainTransaction(
            privateKey,
            payerAccountDO.publicKey.hexStringToByteArray(),
            checkBalance,
            coinPair.receiverAddress,
            payerAccountDO.address,
            ViolasOutputScript().requestMapping(
                coinPair.mappingType,
                payeeAccountDO!!.address.hexStringToByteArray(),
                mContractAddress.hexStringToByteArray()
            )
        ).flatMap {
            BitcoinChainApi.get().pushTx(it.signBytes.toHex())
        }.await()
    }
}