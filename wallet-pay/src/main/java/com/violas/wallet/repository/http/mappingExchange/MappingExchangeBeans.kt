package com.violas.wallet.repository.http.mappingExchange

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.violas.http.Response

/**
 * Created by elephant on 2020-02-14 11:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

data class MappingInfoDTO(
    @SerializedName("address")
    val receiveAddress: String,         // 接收地址
    @SerializedName("token_id")
    val tokenIdx: Long,           // 映射币地址
    val name: String,                   // 映射币或平台币的名称
    @SerializedName("rate")
    val exchangeRate: Double            // 兑换比率
)

data class MappingExchangeOrderDTO(
    val date: Long,
    val amount: String,
    val address: String,
    val coin: String?,
    val status: Int                     // 0：进行中；1：成功；2失败
)

@Keep
class MappingExchangeOrdersResponse : Response<MappingExchangeOrdersResponse.Data>() {

    @Keep
    data class Data(
        @SerializedName(value = "offset")
        var offset: Int = 0,

        @SerializedName(value = "infos")
        var list: List<MappingExchangeOrderDTO>? = null
    )
}

@Keep
data class CrossChainSwapRecord(
    @SerializedName(value = "coina")
    val coinA: String?,
    @SerializedName(value = "amounta")
    val amountA: String?,
    @SerializedName(value = "coinb")
    val coinB: String?,
    @SerializedName(value = "amountb")
    val amountB: String?,
    val version: Long,
    @SerializedName(value = "data")
    val date: Long,
    val status: Int
)