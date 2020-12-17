package com.violas.wallet.repository.http.incentive

import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 12/11/20 4:42 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class IncentiveRepository(private val incentiveApi: IncentiveApi) {

    /**
     * 获取领取激励奖励状态
     */
    suspend fun getReceiveIncentiveRewardsState(
        address: String
    ) =
        incentiveApi.getReceiveIncentiveRewardsResults(address)
            .await(dataNullableOnSuccess = false).data!!.state

    /**
     * 领取激励奖励
     */
    suspend fun receiveIncentiveRewards(
        address: String,
        phoneNumber: String,
        areaCode: String,
        verificationCode: String,
        inviterAddress: String
    ) =
        """{
    "wallet_address":"$address",
    "mobile_number":"$phoneNumber",
    "local_number":"${if (areaCode.startsWith("+")) areaCode else "+$areaCode"}",
    "verify_code":"$verificationCode",
    "inviter_address":"$inviterAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { incentiveApi.receiveIncentiveRewards(it).await() }

    /**
     * 获取邀请好友收益明细
     */
    suspend fun getInviteFriendsEarnings(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        incentiveApi.getInviteFriendsEarnings(address, pageSize, offset)
            .await().data ?: emptyList()

    /**
     * 获取资金池挖矿收益明细
     */
    suspend fun getPoolMiningEarnings(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        incentiveApi.getPoolMiningEarnings(address, pageSize, offset)
            .await().data ?: emptyList()

    /**
     * 获取数字银行挖矿收益明细
     */
    suspend fun getBankMiningEarnings(
        address: String,
        pageSize: Int,
        offset: Int
    ) =
        incentiveApi.getBankMiningEarnings(address, pageSize, offset)
            .await().data ?: emptyList()
}