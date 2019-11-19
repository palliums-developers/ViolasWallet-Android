package com.violas.wallet.repository.http.violas

import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse

/**
 * Created by elephant on 2019-11-11 15:41.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas bean
 */
open class ViolasResponse<T> : ApiResponse {

    @SerializedName(value = "code")
    var errorCode: Int = 0

    @SerializedName(value = "message")
    var errorMsg: String? = null

    @SerializedName(value = "data")
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 2000
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }
}

class ViolasTransactionRecordResponse :
    ViolasResponse<List<ViolasTransactionRecordResponse.Bean>>() {

    data class Bean(
        val amount: String,
        val receiver: String,
        val receiver_module: String,
        val sender: String,
        val sender_module: String,
        val expiration_time: Long,
        val sequence_number: Int,
        val type: Int,  // 1:publish transaction(开启稳定币); 2:p2p transaction(转账)
        val version: Int
    )
}