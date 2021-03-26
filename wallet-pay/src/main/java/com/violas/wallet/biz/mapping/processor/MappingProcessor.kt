package com.violas.wallet.biz.mapping.processor

import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO

/**
 * Created by elephant on 2020/8/13 16:35.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface MappingProcessor {

    fun hasMappable(coinPair: MappingCoinPairDTO): Boolean

    suspend fun mapping(
        checkPayeeAccount: Boolean,
        payeeAddress: String?,
        payeeAccountDO: AccountDO?,
        payerAccountDO: AccountDO,
        password: ByteArray,
        mappingAmount: Long,
        coinPair: MappingCoinPairDTO
    ): String
}