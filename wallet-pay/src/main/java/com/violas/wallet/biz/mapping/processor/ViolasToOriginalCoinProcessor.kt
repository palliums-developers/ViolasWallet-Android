package com.violas.wallet.biz.mapping.processor

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType

/**
 * Created by elephant on 2020/8/13 17:13.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ViolasToOriginalCoinProcessor : MappingProcessor {

    override fun hasMappable(coinPair: MappingCoinPairDTO): Boolean {
        val toCoinType = str2CoinType(coinPair.toCoin.chainName)
        return str2CoinType(coinPair.fromCoin.chainName) == CoinTypes.Violas
                && (toCoinType == (if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin)
                || toCoinType == CoinTypes.Libra)
    }

    override suspend fun mapping(
        payerAccount: AccountDO,
        payerPrivateKey: ByteArray,
        payeeAddress: String,
        coinPair: MappingCoinPairDTO,
        amount: Long
    ): String {
        TODO("Not yet implemented")
    }
}