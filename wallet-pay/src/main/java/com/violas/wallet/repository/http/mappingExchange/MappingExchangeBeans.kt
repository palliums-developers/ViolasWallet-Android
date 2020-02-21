package com.violas.wallet.repository.http.mappingExchange

import com.google.gson.annotations.SerializedName

/**
 * Created by elephant on 2020-02-14 11:46.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

data class MappingInfoDTO(
    @SerializedName("address")
    val receiveAddress: String,         // 接收地址
    @SerializedName("module")
    val tokenAddress: String,           // 映射币地址
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