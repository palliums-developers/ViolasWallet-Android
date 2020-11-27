package com.violas.wallet.repository.http.incentive

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Created by elephant on 11/27/20 10:25 AM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class InviteMiningEarningDTO(
    @SerializedName("address")
    val inviteeAddress: String = "",    // 被邀请账号
    @SerializedName("date")
    val invitationTime: Long = 0,       // 邀请时间
    @SerializedName("amount")
    val miningEarnings: Long = 0,       // 挖矿收益
    @SerializedName("status")
    val status: Int = 0,                // 状态
)

@Keep
data class PoolMiningEarningDTO(
    @SerializedName("date")
    val extractionTime: Long = 0,       // 提取时间
    @SerializedName("amount")
    val extractionAmount: Long = 0,     // 提取数量
    @SerializedName("status")
    val status: Int = 0,                // 状态
)

@Keep
data class BankMiningEarningDTO(
    @SerializedName("date")
    val extractionTime: Long = 0,       // 提取时间
    @SerializedName("amount")
    val extractionAmount: Long = 0,     // 提取数量
    @SerializedName("status")
    val status: Int = 0,                // 状态
)