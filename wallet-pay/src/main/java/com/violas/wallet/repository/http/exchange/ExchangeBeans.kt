package com.violas.wallet.repository.http.exchange

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.quincysx.crypto.CoinTypes
import kotlinx.android.parcel.Parcelize

/**
 * Created by elephant on 2020-02-14 11:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

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
    @SerializedName(value = "token")
    val liquidityAmount: String?,
    @SerializedName(value = "gas_currency")
    val gasCoinName: String?,
    @SerializedName(value = "gas_used")
    val gasCoinAmount: String?,
    @SerializedName(value = "transaction_type")
    val type: String,
    @SerializedName(value = "date")
    val time: Long,
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long,
    val version: Long,
    val status: Int
) : Parcelable {

    companion object {
        const val TYPE_ADD_LIQUIDITY = "ADD_LIQUIDITY"
        const val TYPE_REMOVE_LIQUIDITY = "REMOVE_LIQUIDITY"
    }

    fun isAddLiquidity(): Boolean {
        return type.equals(TYPE_ADD_LIQUIDITY, true)
    }
}

@Keep
@Parcelize
data class SwapRecordDTO(
    @SerializedName(value = "input_name")
    val inputCoinName: String?,
    @SerializedName(value = "input_amount")
    val inputCoinAmount: String?,
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
    val version: Long,
    val status: Int,
    var inputCoinType: Int = CoinTypes.Violas.coinType(),
    var outputCoinType: Int = CoinTypes.Violas.coinType(),
    var customStatus: Status = Status.FAILED
) : Parcelable {

    @Keep
    enum class Status {
        SUCCEEDED, FAILED, PROCESSING, CANCELLED
    }
}

@Keep
data class CrossChainSwapRecordDTO(
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
    @SerializedName(value = "data")
    val time: Long,
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long,
    val version: Long,
    val status: Int
)