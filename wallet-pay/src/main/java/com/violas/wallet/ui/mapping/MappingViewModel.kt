package com.violas.wallet.ui.mapping

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.mapping.MappingCoinPairDTO
import com.violas.wallet.utils.str2CoinType
import kotlinx.coroutines.coroutineScope

/**
 * Created by elephant on 2020/8/12 19:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MappingViewModel : BaseViewModel() {

    companion object {
        /**
         * 获取所有映射币种对信息
         */
        const val ACTION_GET_MAPPING_COIN_PAIRS = 0x01

        /**
         * 映射
         */
        const val ACTION_MAPPING = 0x02
    }

    // 当前选择的映射币种对
    private val currMappingCoinPairLiveData = MutableLiveData<MappingCoinPairDTO?>()

    // 可选择的映射币种对列表
    private val mappingCoinPairsLiveData = MutableLiveData<List<MappingCoinPairDTO>?>()

    private val mappingService by lazy { DataRepository.getMappingService() }

    val accountManager by lazy { AccountManager() }

    fun getCurrMappingCoinPairLiveData(): LiveData<MappingCoinPairDTO?> {
        return currMappingCoinPairLiveData
    }

    fun getMappingCoinPairsLiveData(): LiveData<List<MappingCoinPairDTO>?> {
        return mappingCoinPairsLiveData
    }

    suspend fun getAccount(): AccountDO? = coroutineScope {
        val coinPairs =
            currMappingCoinPairLiveData.value ?: return@coroutineScope null
        val coinNumber = str2CoinType(coinPairs.fromCoin.chainName).coinType()
        accountManager.getIdentityByCoinType(coinNumber)
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        when (action) {
            ACTION_GET_MAPPING_COIN_PAIRS -> {
                var coinPairs = mappingCoinPairsLiveData.value
                if (coinPairs.isNullOrEmpty()) {
                    coinPairs = mappingService.getMappingCoinPairs()
                }
                mappingCoinPairsLiveData.postValue(coinPairs)
            }

            ACTION_MAPPING -> {
                // TODO 映射逻辑
            }

            else -> {
                error("Unsupported action: $action")
            }
        }
    }
}