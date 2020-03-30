package org.palliums.libracore.http

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by elephant on 2020/3/30 18:29.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface LibraApi {

    @POST()
    suspend fun getAccountState(@Body body: RequestBody): ListResponse<AccountStateDTO>

    @POST()
    suspend fun submitTransaction(@Body body: RequestBody): Response<Any>
}