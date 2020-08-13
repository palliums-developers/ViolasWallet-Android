package com.violas.wallet.biz.mapping

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.mapping.processor.MappingProcessor
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.ui.main.market.bean.ITokenVo
import java.lang.RuntimeException
import java.util.*

/**
 * Created by elephant on 2020/8/13 16:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
// 不支持的映射币种对
class UnsupportedMappingCoinPairException : RuntimeException()

class PayeeAccountCoinNotActiveException(
    val coinTypes: CoinTypes,
    val address: String,
    val assets: MappingCoinPairDTO.Assets
) : RuntimeException()

internal class MappingEngine {
    private val processors = LinkedList<MappingProcessor>()

    fun clearProcessors() {
        processors.clear()
    }

    fun addProcessor(processor: MappingProcessor) {
        if (!processors.contains(processor)) {
            processors.add(processor)
        }
    }

    suspend fun mapping(
        payerAccount: AccountDO,
        payerPrivateKey: ByteArray,
        payeeAddress: String,
        coinPair: MappingCoinPairDTO,
        amount: Long
    ): String {
        processors.forEach {
            if (it.hasMappable(coinPair)) {
                return it.mapping(payerAccount, payerPrivateKey, payeeAddress, coinPair, amount)
            }
        }

        throw UnsupportedMappingCoinPairException()
    }
}