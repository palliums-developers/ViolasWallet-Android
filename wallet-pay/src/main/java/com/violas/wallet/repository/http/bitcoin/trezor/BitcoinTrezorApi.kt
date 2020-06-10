package com.violas.wallet.repository.http.bitcoin.trezor

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.net.ApiResponse
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_KEY_URLNAME
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_VALUE_TREZOR
import retrofit2.http.*

/**
 * Created by elephant on 2020/6/5 17:44.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Trezor api
 * @see <a href="https://github.com/trezor/blockbook/blob/master/docs/api.md">link</a>
 */
interface BitcoinTrezorApi {

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param pageNumber 页码，从1开始
     * @param details
     */
    @Headers(value = ["${HEADER_KEY_URLNAME}:${HEADER_VALUE_TREZOR}"])
    @GET("v2/address/{address}")
    suspend fun getTransactionRecords(
        @Path("address") address: String,
        @Query("pageSize") pageSize: Int,
        @Query("page") pageNumber: Int,
        @Query("details") details: String = "txs"
    ): TransactionRecordResponse
}

@Keep
open class Response<T> : ApiResponse {

    @SerializedName(value = "err_no")
    var errorCode: Int = 0

    @SerializedName(value = "err_msg")
    var errorMsg: String? = null

    @SerializedName(value = "data", alternate = ["transactions"])
    var data: T? = null

    override fun getSuccessCode(): Any {
        return 0
    }

    override fun getErrorMsg(): Any? {
        return errorMsg
    }

    override fun getErrorCode(): Any {
        return errorCode
    }

    override fun getResponseData(): Any? {
        return data
    }
}

@Keep
class TransactionRecordResponse : Response<List<TransactionRecordDTO>>() {

    var page: Int = Int.MIN_VALUE
    var totalPages: Int = Int.MIN_VALUE
    var itemsOnPage: Int = Int.MIN_VALUE
    var address: String? = null
    var balance: String? = null
    var totalReceived: String? = null
    var totalSent: String? = null
    var unconfirmedBalance: String? = null
    var unconfirmedTxs: Int = Int.MIN_VALUE
    var txs: Int = Int.MIN_VALUE
}

data class TransactionRecordDTO(
    val blockHash: String,
    val blockHeight: Long,
    val blockTime: Long,
    val confirmations: Long,
    val fees: String,
    val hex: String,
    val lockTime: Long,
    val txid: String,
    val value: String,
    val valueIn: String,
    val version: Long,
    val vin: List<InputDTO>,
    val vout: List<OutputDTO>
) {
    data class InputDTO(
        val addresses: List<String>,
        val hex: String,
        val isAddress: Boolean,
        val n: Long,
        val sequence: Long,
        val txid: String,
        val value: String,
        val vout: Long
    )

    data class OutputDTO(
        val addresses: List<String>,
        val hex: String,
        val isAddress: Boolean,
        val n: Long,
        val spent: Boolean,
        val value: String
    )
}


