package com.violas.wallet.repository.http.exchange

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

/**
 * Created by elephant on 2020-02-14 11:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class SwapTrialSTO(
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("fee")
    val fee: Long,
    @SerializedName("path")
    val path: List<Int>,
    @SerializedName("rate")
    val rate: Double
)

@Keep
data class MarketCurrenciesDTO(
    @SerializedName(value = "violas")
    val violasCurrencies: List<MarketCurrencyDTO>?,
    @SerializedName(value = "btc")
    val bitcoinCurrencies: List<MarketCurrencyDTO>? = null,
    @SerializedName(value = "libra")
    val libraCurrencies: List<MarketCurrencyDTO>? = null,
)

@Keep
data class MarketCurrencyDTO(
    val name: String,
    val module: String,
    val address: String,
    @SerializedName(value = "show_name")
    val displayName: String,
    @SerializedName(value = "icon")
    val logo: String,
    @SerializedName(value = "index")
    val marketIndex: Int
)

@Keep
data class UserPoolInfoDTO(
    @SerializedName(value = "total_token")
    val liquidityTotalAmount: String,
    @SerializedName(value = "balance")
    val liquidityList: List<PoolLiquidityDTO>?
)

@Keep
data class PoolLiquidityDTO(
    @SerializedName(value = "coin_a")
    val coinA: CoinDTO,
    @SerializedName(value = "coin_b")
    val coinB: CoinDTO,
    @SerializedName(value = "token")
    val amount: BigDecimal
) {
    @Keep
    data class CoinDTO(
        val name: String,
        val module: String,
        @SerializedName(value = "module_address")
        val address: String,
        @SerializedName(value = "index")
        val marketIndex: Int,
        @SerializedName(value = "show_name")
        val displayName: String,
        @SerializedName(value = "value")
        val amount: BigDecimal
    )
}

@Keep
data class AddPoolLiquidityEstimateResultDTO(
    @SerializedName(value = "amount")
    val tokenBAmount: BigDecimal,
    @SerializedName(value = "rate")
    val exchangeRate: BigDecimal
)

@Keep
data class RemovePoolLiquidityEstimateResultDTO(
    @SerializedName(value = "coin_a_name")
    val tokenAName: String,
    @SerializedName(value = "coin_a_value")
    val tokenAAmount: BigDecimal,
    @SerializedName(value = "coin_b_name")
    val tokenBName: String,
    @SerializedName(value = "coin_b_value")
    val tokenBAmount: BigDecimal
)

@Keep
data class PoolLiquidityReserveInfoDTO(
    @SerializedName(value = "liquidity_total_supply")
    val liquidityTotalAmount: BigDecimal,
    @SerializedName(value = "coina")
    val coinA: CoinDTO,
    @SerializedName(value = "coinb")
    val coinB: CoinDTO
) {
    @Keep
    data class CoinDTO(
        @SerializedName(value = "name")
        val module: String,
        @SerializedName(value = "index")
        val marketIndex: Int,
        @SerializedName(value = "value")
        val amount: BigDecimal
    )
}

@Keep
data class MapRelationDTO(
    @SerializedName("chain")
    val chain: String,
    @SerializedName("index")
    val index: Int,
    @SerializedName("map_name")
    val mapName: String,
    @SerializedName("module")
    val module: String,
    @SerializedName("module_address")
    val moduleAddress: String,
    @SerializedName("name")
    val name: String
)

data class MappingPairInfoDTO(
    @SerializedName("input_coin_type")
    val inputCoinType: String,
    @SerializedName("lable")
    val lable: String,
    @SerializedName("receiver_address")
    val receiverAddress: String,
    @SerializedName("to_coin")
    val toCoin: ToCoinDTO
)

data class ToCoinDTO(
    @SerializedName("assets")
    val assets: AssetsDTO?,
    @SerializedName("coin_type")
    val coinType: String
)

data class AssetsDTO(
    @SerializedName("address")
    val address: String,
    @SerializedName("module")
    val module: String,
    @SerializedName("name")
    val name: String
)

@Keep
@Parcelize
data class PoolRecordDTO(
    @SerializedName(value = "coina")
    val coinAName: String?,
    @SerializedName(value = "amounta")
    val coinAAmount: String?,

    @SerializedName(value = "coinb")
    val coinBName: String?,
    @SerializedName(value = "amountb")
    val coinBAmount: String?,

    @SerializedName(value = "gas_currency")
    val gasCoinName: String?,
    @SerializedName(value = "gas_used")
    val gasCoinAmount: String?,

    @SerializedName(value = "token")
    val liquidityAmount: String?,
    @SerializedName(value = "transaction_type")
    val type: String,
    @SerializedName(value = "status")
    val status: String?,

    @SerializedName(value = "date")
    val time: Long,
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long,
    @SerializedName(value = "version")
    val version: Long
) : Parcelable {

    companion object {
        const val TYPE_ADD_LIQUIDITY = "ADD_LIQUIDITY"
        const val TYPE_REMOVE_LIQUIDITY = "REMOVE_LIQUIDITY"
    }

    fun isAddLiquidity(): Boolean {
        return type.equals(TYPE_ADD_LIQUIDITY, true)
    }

    fun isSuccess(): Boolean {
        return status?.equals("Executed", true) == true
    }
}

@Keep
@Parcelize
data class SwapRecordDTO(
    @SerializedName(value = "from_chain")
    val inputChainName: String?,
    @SerializedName(value = "input_name")
    val inputCoinName: String?,
    @SerializedName(value = "input_amount")
    val inputCoinAmount: String?,
    @SerializedName(value = "input_shown_name")
    val inputCoinDisplayName: String?,

    @SerializedName(value = "to_chain")
    val outputChainName: String?,
    @SerializedName(value = "output_name")
    val outputCoinName: String?,
    @SerializedName(value = "output_amount")
    val outputCoinAmount: String?,
    @SerializedName(value = "output_shown_name")
    val outputCoinDisplayName: String?,

    @SerializedName(value = "gas_currency")
    val gasCoinName: String?,
    @SerializedName(value = "gas_used")
    val gasCoinAmount: String?,

    @SerializedName(value = "data")
    val time: Long,
    @SerializedName(value = "version")
    val version: Long,
    @SerializedName(value = "status")
    val status: Int
) : Parcelable

@Keep
@Parcelize
data class ViolasSwapRecordDTO(

    @SerializedName(value = "input_show_name")
    val inputDisplayName: String?,
    @SerializedName(value = "input_name")
    val inputCoinName: String?,
    @SerializedName(value = "input_amount")
    val inputCoinAmount: String?,

    @SerializedName(value = "output_show_name")
    val outputDisplayName: String?,
    @SerializedName(value = "output_name")
    val outputCoinName: String?,
    @SerializedName(value = "output_amount")
    val outputCoinAmount: String?,

    @SerializedName(value = "gas_currency")
    val gasCoinName: String?,
    @SerializedName(value = "gas_used")
    val gasCoinAmount: String?,

    @SerializedName(value = "date")
    val time: Long,
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long,
    @SerializedName(value = "version")
    val version: Long,
    @SerializedName(value = "status")
    val status: String?
) : Parcelable