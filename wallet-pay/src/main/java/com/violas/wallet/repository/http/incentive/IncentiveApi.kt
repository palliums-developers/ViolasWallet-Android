package com.violas.wallet.repository.http.incentive

import com.palliums.violas.http.Response
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Created by elephant on 12/11/20 4:28 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface IncentiveApi {

    /**
     * 获取领取激励奖励结果
     */
    @GET("/violas/1.0/incentive/check/verified")
    fun getReceiveIncentiveRewardsResults(
        @Query("address") address: String
    ): Observable<Response<ReceiveIncentiveRewardsResultsDTO>>

    /**
     * 领取激励奖励
     */
    @POST("/violas/1.0/incentive/mobile/verify")
    fun receiveIncentiveRewards(
        @Body body: RequestBody
    ): Observable<Response<Any>>
}