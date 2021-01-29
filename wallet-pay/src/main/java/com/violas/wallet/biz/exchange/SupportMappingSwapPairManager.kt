package com.violas.wallet.biz.exchange

import com.palliums.utils.CustomIOScope
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.MappingPairInfoDTO
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import com.violas.wallet.utils.str2CoinNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

class SupportMappingSwapPairManager : CoroutineScope by CustomIOScope() {

    private val mMappingSwapPair = ArrayList<MappingPairInfoDTO>()
    private val mExchangeService by lazy {
        DataRepository.getExchangeService()
    }

    /**
     * 获取映射服务支持的交易对
     * @exception Exception 网络请求失败会报错
     */
    @Throws(Exception::class)
    fun getMappingSwapPair(force: Boolean = false): List<MappingPairInfoDTO> {
        // 交易市场不支持跨链兑换
        /*if (force || mMappingSwapPair.isEmpty()) {
            synchronized(this) {
                if (mMappingSwapPair.isEmpty()) {
                    mMappingSwapPair.clear()
                    val countDownLatch = CountDownLatch(1)
                    launch {
                        mExchangeService.getMarketMappingPairInfo()?.let {
                            mMappingSwapPair.addAll(it)
                        }
                        countDownLatch.countDown()
                    }
                    countDownLatch.await()
                    return mMappingSwapPair
                }
            }
        }*/
        return mMappingSwapPair
    }

    fun getMappingTokensInfo(coinTypes: CoinTypes): HashMap<String, MappingInfo> {
        val result = HashMap<String, MappingInfo>()
        mMappingSwapPair
            .filter {
                str2CoinNumber(it.inputCoinType) == coinTypes.coinType()
            }
            .map { mappingPair ->
                val assetsMark =
                    when (val toMappingCoinTypes = str2CoinNumber(mappingPair.toCoin.coinType)) {
                        CoinTypes.BitcoinTest.coinType(),
                        CoinTypes.Bitcoin.coinType() -> {
                            CoinAssetsMark(CoinTypes.parseCoinType(toMappingCoinTypes))
                        }
                        CoinTypes.Libra.coinType(),
                        CoinTypes.Violas.coinType() -> {
                            LibraTokenAssetsMark(
                                CoinTypes.parseCoinType(toMappingCoinTypes),
                                mappingPair.toCoin.assets?.module ?: "",
                                mappingPair.toCoin.assets?.address ?: "",
                                mappingPair.toCoin.assets?.name ?: ""
                            )
                        }
                        else -> {
                            null
                        }
                    }
                assetsMark?.let {
                    result[assetsMark.mark()] =
                        MappingInfo(mappingPair.lable, mappingPair.receiverAddress)
                }
            }
        return result
    }
}

data class MappingInfo(
    val label: String,
    val receiverAddress: String
)
