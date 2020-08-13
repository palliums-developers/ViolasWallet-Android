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
        val displayName: String
    )
}

@Keep
data class MappingRecordDTO(
    @SerializedName(value = "amount_from")
    val inputCoin: Coin,
    @SerializedName(value = "amount_to")
    val outputCoin: Coin,
    @SerializedName(value = "version_or_block_height")
    val transactionId: String,          // btc块的高度或者libra和violas的version
    @SerializedName(value = "confirmed_time")
    val confirmedTime: Long,
    @SerializedName(value = "status")
    val status: Int
) {
    data class Coin(
        @SerializedName("name")
        val name: String,
        @SerializedName("amount")
        val amount: String,
        @SerializedName("chain")
        val chainName: String,
        @SerializedName("show_name")
        val displayName: String
    )
}