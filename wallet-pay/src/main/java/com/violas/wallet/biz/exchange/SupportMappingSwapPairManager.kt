package com.violas.wallet.biz.exchange

import com.google.gson.annotations.SerializedName
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.Vm
import com.violas.wallet.ui.main.market.bean.CoinAssetsMark
import com.violas.wallet.ui.main.market.bean.LibraTokenAssetsMark
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class SupportMappingSwapPairManager {

    private val mMappingSwapPair = ArrayList<MockMappingInterface>()

    /**
     * 获取
     */
    fun getMappingSwapPair(force: Boolean = false): List<MockMappingInterface> {
        if (force || mMappingSwapPair.isEmpty()) {
            synchronized(this) {
                if (mMappingSwapPair.isEmpty()) {
                    mMappingSwapPair.clear()
                    mMappingSwapPair.addAll(mockNetworkData())
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
                        mappingPair.toCoin.assets.module,
                        mappingPair.toCoin.assets.address,
                        mappingPair.toCoin.assets.name
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

    private fun mockNetworkData(): List<MockMappingInterface> {
        val list = mutableListOf<MockMappingInterface>()
        list.add(
            MockMappingInterface(
                "libra",
                "l2vusd",
                "00000000000000000000000000000001",
                ToCoin(
                    Assets(
                        "0000000000000000000000000a550c18",
                        "VLSUSD",
                        "VLSUSD"
                    ),
                    "violas"
                )
            )
        )
        list.add(
            MockMappingInterface(
                "libra",
                "l2vgbp",
                "00000000000000000000000000000001",
                ToCoin(
                    Assets(
                        "0000000000000000000000000a550c18",
                        "VLSGBP",
                        "VLSGBP"
                    ),
                    "violas"
                )
            )
        )
        list.add(
            MockMappingInterface(
                "violas",
                "v2lusd",
                "00000000000000000000000000000001",
                ToCoin(
                    Assets(
                        "0000000000000000000000000a550c18",
                        "Coin1",
                        "Coin1"
                    ),
                    "libra"
                )
            )
        )
        list.add(
            MockMappingInterface(
                "violas",
                "v2lgbp",
                "00000000000000000000000000000001",
                ToCoin(
                    Assets(
                        "0000000000000000000000000a550c18",
                        "Coin2",
                        "Coin2"
                    ),
                    "libra"
                )
            )
        )
        list.add(
            MockMappingInterface(
                "violas",
                "v2b",
                "00000000000000000000000000000001",
                ToCoin(
                    null,
                    "btc"
                )
            )
        )
        return list
    }

    data class MockMappingInterface(
        @SerializedName("input_coin_type")
        val inputCoinType: String,
        @SerializedName("lable")
        val lable: String,
        @SerializedName("receiver_address")
        val receiverAddress: String,
        @SerializedName("to_coin")
        val toCoin: ToCoin
    )

    data class ToCoin(
        @SerializedName("assets")
        val assets: Assets?,
        @SerializedName("coin_type")
        val coinType: String
    )

    data class Assets(
        @SerializedName("address")
        val address: String,
        @SerializedName("module")
        val module: String,
        @SerializedName("name")
        val name: String
    )
}

data class MappingInfo(
    val label: String,
    val receiverAddress: String
)
