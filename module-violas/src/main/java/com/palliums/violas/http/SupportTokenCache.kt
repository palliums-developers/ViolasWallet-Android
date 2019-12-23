package com.palliums.violas.http

import com.palliums.net.RequestException

/**
 * Created by elephant on 2019-12-20 21:11.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 平台支持的Token缓存，缓存不存在时从网络中获取
 */
object SupportTokenCache {

    private val supportTokens by lazy {
        mutableMapOf<String, SupportCurrencyDTO>()
    }

    @Throws(RequestException::class)
    suspend fun getSupportTokens(violasRepository: ViolasRepository): Map<String, SupportCurrencyDTO> {
        synchronized(this) {
            if (supportTokens.isNotEmpty()) {
                return supportTokens
            }
        }

        val response = violasRepository.getSupportToken()

        synchronized(this) {
            response.data?.forEach {
                supportTokens[it.address] = it
            }

            return supportTokens
        }
    }

    fun release() {
        synchronized(this) {
            supportTokens.clear()
        }
    }
}