package com.violas.wallet.repository.http.libra

import com.google.gson.annotations.SerializedName
import com.violas.wallet.repository.http.ApiResponse

/**
 * Created by elephant on 2019-11-11 11:56.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer bean
 */
open class LibexplorerResponse<T> : ApiResponse {

    @SerializedName(value = "status")
    var errorCode: String = ""

    @SerializedName(value = "message")
    var errorMsg: String? = null

    @SerializedName(value = "result")
    var data: T? = null

    override fun getSuccessCode(): Any {
        return "1"
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }
}

class LibexplorerTransactionRecordResponse :
    LibexplorerResponse<List<LibexplorerTransactionRecordResponse.Bean>>() {

    data class Bean(
        val to: String,
        val from: String,
        val value: String,
        val gasUsed: Long,
        val maxGasAmount: Long,
        val gasUnitPrice: Long,
        val expirationTime: Long,
        val sequenceNumber: Int,
        val status: String,
        val version: String
    )
}