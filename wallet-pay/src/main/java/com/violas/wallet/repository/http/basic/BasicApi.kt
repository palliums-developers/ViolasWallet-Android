package com.violas.wallet.repository.http.basic

import com.palliums.violas.http.Response
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by elephant on 12/11/20 4:15 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface BasicApi {

    @POST("/1.0/violas/verify_code")
    fun sendVerifyCode(
        @Body body: RequestBody
    ): Observable<Response<Any>>
}