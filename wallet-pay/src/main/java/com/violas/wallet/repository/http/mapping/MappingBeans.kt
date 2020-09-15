package com.violas.wallet.repository.http.mapping

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Created by elephant on 2020-02-14 11:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class MappingCoinPairDTO(
    @SerializedName("from_coin")
    val fromCoin: Coin,
    @SerializedName("to_coin")
    val toCoin: Coin,
    @SerializedName("lable")
    val mappingType: String,
    @SerializedName("receiver_address")
    val receiverAddress: String
) {
    data class Coin(
        @SerializedName("assert")
        val assets: Assets,
        @SerializedName("coin_type")
        val chainName: String
    )

    data class Assets(
        @SerializedName("name")
        val name: String,
        @SerializedName("module")
        val module: String,
        @SerializedName("address")
        val address: String,
        @SerializedName("show_name")
        val displayName: String,
        @SerializedName("icon")
        val logo: String
    )
}

@Keep
data class MappingRecordDTO(
    @SerializedName(value = "in_token")
    val inputCoinName: String,
    @SerializedName(value = "in_amount")
    val inputCoinAmount: String?,
    @SerializedName(value = "from_chain")
    val inputChainName: String?,
    @SerializedName(value = "in_show_name")
    val inputCoinDisplayName: String?,

    @SerializedName(value = "out_token")
    val outputCoinName: String,
    @SerializedName(value = "out_amount")
    val outputCoinAmount: String?,
    @SerializedName(value = "to_chain")
    val outputChainName: String?,
    @SerializedName(value = "out_show_name")
    val outputCoinDisplayName: String?,

    @SerializedName(value = "version")
    val version: String?,
    @SerializedName(value = "tran_id")
    val txId: String?,
    @SerializedName(value = "state")
    val state: String?,
    @SerializedName(value = "expiration_time")
    val time: Long
)