package com.violas.wallet.biz.mapping.processor

import com.palliums.content.ContextProvider
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchange.AccountPayeeNotFindException
import com.violas.wallet.biz.mapping.PayeeAccountCoinNotActiveException
import com.violas.wallet.common.SimpleSecurity
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType
import com.violas.walletconnect.extensions.hexStringToByteArray
import org.json.JSONObject
import org.palliums.libracore.http.LibraRpcService
import org.palliums.violascore.crypto.KeyPair
import org.palliums.violascore.transaction.AccountAddress
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.optionTransactionPayload
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTagStructTag
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2020/8/13 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ViolasToOriginalCoinProcessor(
    private val libraRpcService: LibraRpcService
) : MappingProcessor {

    private val violasService by lazy { DataRepository.getViolasService() }

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        val toCoinType = str2CoinType(coinPair.toCoin.chainName)
        return str2CoinType(coinPair.fromCoin.chainName) == CoinTypes.Violas
                && (toCoinType == (if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin)
                || toCoinType == CoinTypes.Libra)
    }

    override suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAccountDO: AccountDO,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        if (checkPayeeAccount && str2CoinType(coinPair.toCoin.chainName) == CoinTypes.Libra) {
            // 检查收款账户激活状态
            val payeeAccountState =
                libraRpcService.getAccountState(payeeAccountDO.address)
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
        subMappingDate.put("flag", "violas")
        subMappingDate.put("type", coinPair.mappingType)
        subMappingDate.put(
            "to_address",
            if (str2CoinType(coinPair.toCoin.chainName)
                == if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin
            )
                payeeAccountDO.address
            else
                "00000000000000000000000000000000${payeeAccountDO.address}"
        )
        subMappingDate.put("state", "start")
        subMappingDate.put("times", 1)

        val optionMappingTransactionPayload =
            TransactionPayload.optionTransactionPayload(
                ContextProvider.getContext(),
                coinPair.receiverAddress,
                amount,
                metaData = subMappingDate.toString().toByteArray(),
                typeTag = typeTagFrom
            )

        return violasService.sendTransaction(
            optionMappingTransactionPayload,
            Account(KeyPair.fromSecretKey(payerPrivateKey)),
            gasCurrencyCode = typeTagFrom.value.module,
            chainId = Vm.ViolasChainId
        ).sequenceNumber.toString()
    }
}