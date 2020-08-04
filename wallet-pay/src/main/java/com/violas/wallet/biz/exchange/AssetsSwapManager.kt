package com.violas.wallet.biz.exchange

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.palliums.violas.smartcontract.ViolasMultiTokenContract
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.exchange.processor.BTCToMappingAssetsProcessor
import com.violas.wallet.biz.exchange.processor.LibraToMappingAssetsProcessor
import com.violas.wallet.biz.exchange.processor.ViolasToAssetsMappingProcessor
import com.violas.wallet.biz.exchange.processor.ViolasTokenToViolasTokenProcessor
import com.violas.wallet.common.Vm
import com.violas.wallet.ui.main.market.MarketViewModel
import com.violas.wallet.ui.main.market.bean.*
import com.violas.wallet.utils.str2CoinType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.palliums.violascore.http.ViolasException
import kotlin.collections.HashMap

class AssetsSwapManager(
    private val supportMappingSwapPairManager: SupportMappingSwapPairManager
) {
    /**
     * 稳定币交易所，uniswap 交易所支持的所有币种
     */
    var mSupportTokensLiveData: MutableLiveData<List<ITokenVo>?> = MutableLiveData()

    /**
     * 映射兑换支持的币种
     * 内容应该根据 mSupportTokens 变化
     */
    var mMappingSupportSwapPairMapLiveData: MutableLiveData<HashMap<String, MutBitmap>?> =
        MutableLiveData()

    private val mAssetsSwapEngine = AssetsSwapEngine()

    val contract = ViolasMultiTokenContract(Vm.TestNet)

    @WorkerThread
    suspend fun calculateTokenMapInfo(
        supportTokens: List<ITokenVo>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                mSupportTokensLiveData.postValue(supportTokens)

                val supportTokensPair = getMappingMarketSupportTokens(supportTokens)
                mMappingSupportSwapPairMapLiveData.postValue(supportTokensPair)

                mAssetsSwapEngine.clearProcessor()
                mAssetsSwapEngine.addProcessor(ViolasTokenToViolasTokenProcessor())
                mAssetsSwapEngine.addProcessor(
                    ViolasToAssetsMappingProcessor(
                        supportMappingSwapPairManager.getMappingTokensInfo(CoinTypes.Violas)
                    )
                )
                mAssetsSwapEngine.addProcessor(
                    LibraToMappingAssetsProcessor(
                        supportMappingSwapPairManager.getMappingTokensInfo(CoinTypes.Libra)
                    )
                )
                mAssetsSwapEngine.addProcessor(
                    BTCToMappingAssetsProcessor(
                        contract.getContractAddress(),
                        supportMappingSwapPairManager.getMappingTokensInfo(
                            if (Vm.TestNet) {
                                CoinTypes.BitcoinTest
                            } else {
                                CoinTypes.Bitcoin
                            }
                        )
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
            return@withContext true
        }
    }

    /**
     * 交易接收的币种
     */
    fun getSwapPayeeTokenList(fromToken: ITokenVo): List<ITokenVo> {
        return filterPayeeTokens(fromToken)
    }

    @Throws(ViolasException::class)
    suspend fun swap(
        pwd: ByteArray,
        tokenFrom: ITokenVo,
        tokenTo: ITokenVo,
        amountIn: Long,
        amountOutMin: Long,
        path: ByteArray,
        data: ByteArray
    ): String {
        return mAssetsSwapEngine.swap(
            pwd,
            tokenFrom,
            tokenTo,
            null,
            amountIn,
            amountOutMin,
            path,
            data
        )
    }

    /**
     * 获取映射兑换交易 和 币币 交易支持的币种 bitmap
     */
    @Throws(Exception::class)
    private fun getMappingMarketSupportTokens(supportTokens: List<ITokenVo>): HashMap<String, MutBitmap> {
        val result = HashMap<String, MutBitmap>()

        val mappingMarketSupportTokens = getMappingCoinTokenPair()

        val supportTokenMap = HashMap<String, Int>(supportTokens.size)
        val supportTokenCoinMap = HashMap<Int, List<ITokenVo>>()
        supportTokens.forEachIndexed { index, iTokenVo ->
            supportTokenMap[IAssetsMark.convert(iTokenVo).mark()] = index

            val tokens: MutableList<ITokenVo> =
                if (supportTokenCoinMap.containsKey(iTokenVo.coinNumber)) {
                    supportTokenCoinMap[iTokenVo.coinNumber] as MutableList<ITokenVo>
                } else {
                    val tokens = mutableListOf<ITokenVo>()
                    supportTokenCoinMap[iTokenVo.coinNumber] = tokens
                    tokens
                }
            tokens.add(iTokenVo)
        }

        supportTokens.forEach { assets ->
            val bitmap = MutBitmap()

            if (assets.coinNumber == CoinTypes.Violas.coinType()) {
                // 将相同链的币种放入集合
                supportTokenCoinMap[assets.coinNumber]?.forEach { iTokenVo ->
                    val assetsMark = IAssetsMark.convert(iTokenVo)
                    val hasNotOneAssets = assetsMark.mark() != IAssetsMark.convert(assets).mark()
                    if (hasNotOneAssets) {
                        supportTokenMap[assetsMark.mark()]?.let { index ->
                            bitmap.setBit(index)
                        }
                    }
                }
            }

            // 将符合条件的映射币放入集合
            mappingMarketSupportTokens[assets.coinNumber]?.forEach { assetsMark ->
                supportTokenMap[assetsMark.mark()]?.let { index ->
                    bitmap.setBit(index)
                }
            }
            result[IAssetsMark.convert(assets).mark()] = bitmap
        }
        return result
    }

    private fun filterPayeeTokens(
        fromToken: ITokenVo,
        supportTokens: List<ITokenVo>? = mSupportTokensLiveData.value,
        supportTokensPair: Map<String, MutBitmap>? = mMappingSupportSwapPairMapLiveData.value
    ): List<ITokenVo> {
        val result = mutableListOf<ITokenVo>()

        val assetsMark = IAssetsMark.convert(fromToken)
        supportTokensPair?.get(assetsMark.mark())?.forEach {
            supportTokens?.get(it)?.let { token -> result.add(token) }
        }

        return result
    }

    /**
     * 获取并解析处理映射币交易对
     * @exception Exception 网络请求失败会报错
     */
    @Throws(Exception::class)
    private fun getMappingCoinTokenPair(): java.util.HashMap<Int, List<IAssetsMark>> {
        val resultMap = java.util.HashMap<Int, List<IAssetsMark>>()
        supportMappingSwapPairManager.getMappingSwapPair().forEach { mappingPair ->
            val tokens: MutableList<IAssetsMark>? =
                str2CoinType(mappingPair.inputCoinType)?.let { coinType ->
                    if (resultMap.containsKey(coinType)) {
                        resultMap[coinType] as MutableList<IAssetsMark>
                    } else {
                        val tokens = mutableListOf<IAssetsMark>()
                        resultMap[coinType] = tokens
                        tokens
                    }
                }

            val assetsMark = str2CoinType(mappingPair.toCoin.coinType)?.let { coinType ->
                when (coinType) {
                    CoinTypes.BitcoinTest.coinType(),
                    CoinTypes.Bitcoin.coinType() -> {
                        CoinAssetsMark(CoinTypes.parseCoinType(coinType))
                    }
                    CoinTypes.Libra.coinType(),
                    CoinTypes.Violas.coinType() -> {
                        LibraTokenAssetsMark(
                            CoinTypes.parseCoinType(coinType),
                            mappingPair.toCoin.assets?.module ?: "",
                            mappingPair.toCoin.assets?.address ?: "",
                            mappingPair.toCoin.assets?.name ?: ""
                        )
                    }
                    else -> {
                        null
                    }
                }
            }
            if (assetsMark != null) {
                tokens?.add(assetsMark)
            }
        }
        return resultMap
    }


}