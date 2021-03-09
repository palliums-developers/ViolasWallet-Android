package com.violas.wallet.biz.mapping

import com.violas.wallet.biz.mapping.processor.MappingProcessor
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import java.util.*

/**
 * Created by elephant on 2020/8/13 16:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
// 不支持的映射币种对
class UnsupportedMappingCoinPairException(
    val coinPair: MappingCoinPairDTO
) : RuntimeException()

// 收款账户币种未激活
class PayeeAccountCoinNotActiveException(
    val accountDO: AccountDO,
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
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        amount: Long,
        coinPair: MappingCoinPairDTO
    ): String {
        processors.forEach {
            if (it.hasMappable(coinPair)) {
                return it.mapping(
                    checkPayeeAccount,
                    payeeAddress,
                    payeeAccountDO,
                    payerAccountDO,
                    password,
                    amount,
                    coinPair
                )
            }
        }

        throw UnsupportedMappingCoinPairException(coinPair)
    }
}