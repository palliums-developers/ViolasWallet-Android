package com.violas.wallet.repository.http.dex

import com.palliums.net.RequestException
import com.violas.wallet.repository.DataRepository

/**
 * Created by elephant on 2019-12-10 11:17.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 交易Token缓存，缓存不存在时从网络中获取
 */
object DexTokenCache {

    private val dexTokens by lazy {
        mutableMapOf<String, DexTokenPriceDTO>()
    }

    @Throws(RequestException::class)
    suspend fun getDexTokens(dexRepository: DexRepository? = null): Map<String, DexTokenPriceDTO> {
        synchronized(this) {
            if (dexTokens.isNotEmpty()) {
                return dexTokens
            }
        }

        val dexService = dexRepository ?: DataRepository.getDexService()
        val tokens = dexService.getTokenPrices()

        synchronized(this) {
            tokens.forEach {
                dexTokens[it.address] = it
            }

            return dexTokens
        }
    }
}