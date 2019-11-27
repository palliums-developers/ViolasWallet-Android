package com.palliums.violas.http

import com.google.gson.Gson
import com.palliums.net.NetworkException
import com.palliums.net.checkResponse
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class ViolasRepository(private val mViolasApi: ViolasApi) {

    @Throws(NetworkException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        offset: Int
    ): ListResponse<TransactionRecordDTO> {
        return checkResponse {
            mViolasApi.getTransactionRecord(address, pageSize, offset)
        }
    }

    fun getBalance(address: String, module: String = "") =
        if (module.isEmpty()) {
            mViolasApi.getBalance(address)
        } else {
            mViolasApi.getBalance(address, module)
        }

    fun getSequenceNumber(address: String) =
        mViolasApi.getSequenceNumber(address)

    fun pushTx(tx: String): Single<Response<Any>> {
        val requestBody = Gson().toJson(SignedTxnDTO(tx))
            .toRequestBody("application/json".toMediaTypeOrNull())
        return mViolasApi.pushTx(requestBody)
    }

    fun getSupportCurrency() =
        mViolasApi.getSupportCurrency()

    fun checkRegisterToken(address: String) =
        mViolasApi.checkRegisterToken(address)

}