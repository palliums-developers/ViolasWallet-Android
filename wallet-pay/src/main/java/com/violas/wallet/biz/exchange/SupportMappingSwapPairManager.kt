package com.violas.wallet.biz.exchange

import com.palliums.utils.CustomIOScope
import com.quincysx.crypto.CoinType
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.exchange.MappingPairInfoDTO
import com.violas.wallet.ui.main.market.bean.CoinAssetMark
import com.violas.wallet.ui.main.market.bean.DiemCurrencyAssetMark
import com.violas.wallet.utils.str2CoinNumber
import kotlinx.coroutines.CoroutineScope

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

    fun getMappingTokensInfo(coinType: CoinType): HashMap<String, MappingInfo> {
        val result = HashMap<String, MappingInfo>()
        mMappingSwapPair
            .filter {
                str2CoinNumber(it.inputCoinType) == coinType.coinNumber()
            }
            .map { mappingPair ->
                val assetsMark =
                    when (val toMappingCoinTypes = str2CoinNumber(mappingPair.toCoin.coinType)) {
                        getBitcoinCoinType().coinNumber() -> {
                            CoinAssetMark(CoinType.parseCoinNumber(toMappingCoinTypes))
                        }
                        getDiemCoinType().coinNumber(),
                        getViolasCoinType().coinNumber() -> {
                            DiemCurrencyAssetMark(
                                CoinType.parseCoinNumber(toMappingCoinTypes),
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
