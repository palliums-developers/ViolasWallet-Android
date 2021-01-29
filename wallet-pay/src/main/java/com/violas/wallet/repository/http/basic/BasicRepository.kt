package com.violas.wallet.repository.http.basic

import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 12/11/20 4:16 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class BasicRepository(private val basicApi: BasicApi) {

    /**
     * 获取手机验证码
     */
    suspend fun sendPhoneVerifyCode(
        address: String,
        phoneNumber: String,
        areaCode: String
    ) =
        """{
    "address":"$address",
    "receiver":"$phoneNumber",
    "phone_local_number":"${if (areaCode.startsWith("+")) areaCode else "+$areaCode"}"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { basicApi.sendVerifyCode(it).await() }


    /**
     * 获取邮箱验证码
     */
    suspend fun sendEmailVerifyCode(
        address: String,
        emailAddress: String
    ) =
        """{
    "address":"$address",
    "receiver":"$emailAddress"
}""".toRequestBody("application/json".toMediaTypeOrNull())
            .let { basicApi.sendVerifyCode(it).await() }

}