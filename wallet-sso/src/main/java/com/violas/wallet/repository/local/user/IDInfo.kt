package com.violas.wallet.repository.local.user

import com.google.gson.annotations.Expose

/**
 * Created by elephant on 2019-11-29 11:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份信息
 */
data class IDInfo(
    var idName: String,         // 身份姓名
    var idNumber: String,       // 身份证号码
    var idCardFrontUrl: String, // 身份证正面url
    var idCardBackUrl: String,  // 身份证背面url
    var idCountryCode: String,  // 身份所属国家代码

    @IDAuthenticationStatus
    @Expose(serialize = false, deserialize = false)
    var idAuthenticationStatus: Int = IDAuthenticationStatus.AUTHENTICATED
){

    fun isAuthenticatedID(): Boolean {
        return idAuthenticationStatus == IDAuthenticationStatus.AUTHENTICATED
    }
}