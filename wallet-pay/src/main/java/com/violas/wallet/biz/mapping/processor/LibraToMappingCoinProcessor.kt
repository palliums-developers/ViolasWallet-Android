package com.violas.wallet.biz.mapping.processor

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.mapping.PayeeAccountCoinNotActiveException
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.getDiemChainId
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.libracore.crypto.KeyPair
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.optionTransactionPayload
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTagStructTag
import org.palliums.libracore.wallet.Account
import org.palliums.violascore.http.ViolasRpcService

/**
 * Created by elephant on 2020/8/13 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraToMappingCoinProcessor(
    private val violasRpcService: ViolasRpcService
) : MappingProcessor {

    private val libraService by lazy { DataRepository.getDiemRpcService() }

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        return str2CoinType(coinPair.fromCoin.chainName) == getDiemCoinType()
                && str2CoinType(coinPair.toCoin.chainName) == getViolasCoinType()
    }

    override suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        if (checkPayeeAccount) {
            // 检查收款账户激活状态
            val payeeAccountState = violasRpcService.getAccountState(payeeAccountDO!!.address)
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

        val typeTagFrom = TypeTagStructTag(
            StructTag(
                AccountAddress(coinPair.fromCoin.assets.address.hexStringToByteArray()),
                coinPair.fromCoin.assets.module,
                coinPair.fromCoin.assets.name,
                arrayListOf()
            )
        )

        val subMappingDate = JSONObject()
        subMappingDate.put("flag", "libra")
        subMappingDate.put("type", coinPair.mappingType)
        subMappingDate.put(
            "to_address",
            "00000000000000000000000000000000${payeeAccountDO!!.address}"
        )
        subMappingDate.put("state", "start")
        subMappingDate.put("times", 0)

        val optionMappingTransactionPayload =
            TransactionPayload.optionTransactionPayload(
                ContextProvider.getContext(),
                coinPair.receiverAddress,
                amount,
                metaData = subMappingDate.toString().toByteArray(),
                typeTag = typeTagFrom
            )

        return libraService.sendTransaction(
            optionMappingTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = getDiemChainId()
        ).sequenceNumber.toString()
    }
}