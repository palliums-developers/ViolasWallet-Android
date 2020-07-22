package com.violas.wallet.biz.exchange

import com.palliums.utils.CustomIOScope
import com.palliums.violas.http.MappingPairInfoDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class SupportMappingSwapPairManager : CoroutineScope by CustomIOScope() {

    private val mMappingSwapPair = ArrayList<MappingPairInfoDTO>()
    val mViolasService by lazy {
        DataRepository.getViolasService()
    }

    /**
     * 获取映射服务支持的交易对
     * @exception Exception 网络请求失败会报错
     */
    @Throws(Exception::class)
    fun getMappingSwapPair(force: Boolean = false): List<MappingPairInfoDTO> {
        if (force || mMappingSwapPair.isEmpty()) {
            synchronized(this) {
                if (mMappingSwapPair.isEmpty()) {
                    mMappingSwapPair.clear()
                    val countDownLatch = CountDownLatch(1)
                    launch {
                        mViolasService.getMarketMappingPairInfo().data?.let {
                            mMappingSwapPair.addAll(it)
                        }
                        countDownLatch.countDown()
                    }
                    countDownLatch.await()
                    return mMappingSwapPair
                }
            }
        }
        return mMappingSwapPair
    }

    fun getMappingTokensInfo(coinTypes: CoinTypes): HashMap<String, MappingInfo> {
        val result = HashMap<String, MappingInfo>()
        mMappingSwapPair
            .filter {
                str2CoinType(it.inputCoinType) == coinTypes.coinType()
            }
            .map { mappingPair ->
                val assetsMark = if (mappingPair.toCoin.assets == null) {
                    CoinAssetsMark(coinTypes)
                } else {
                    LibraTokenAssetsMark(
                        coinTypes,
                        mappingPair.toCoin.assets?.module ?: "",
                        mappingPair.toCoin.assets?.address ?: "",
                        mappingPair.toCoin.assets?.name ?: ""
                    )
                }
                result[assetsMark.mark()] =
                    MappingInfo(mappingPair.lable, mappingPair.receiverAddress)
            }
        return result
    }

    private fun str2CoinType(str: String): Int? {
        return when (str.toLowerCase(Locale.ROOT)) {
            "btc" -> {
                if (Vm.TestNet) {
                    CoinTypes.BitcoinTest.coinType()
                } else {
                    CoinTypes.Bitcoin.coinType()
                }
            }
            "libra" -> {
                CoinTypes.Libra.coinType()
            }
            "violas" -> {
                CoinTypes.Violas.coinType()
            }
            else -> null
        }
    }
}

data class MappingInfo(
    val label: String,
    val receiverAddress: String
)
