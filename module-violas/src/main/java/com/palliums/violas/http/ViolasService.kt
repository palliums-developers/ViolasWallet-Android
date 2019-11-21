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
class ViolasService(private val violasApi: ViolasApi) {

    @Throws(NetworkException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        offset: Int
    ): ViolasApiResponse<List<TransactionRecordDTO>> {
        return checkResponse {
            violasApi.getTransactionRecord(address, pageSize, offset)
        }
    }

    fun getBalance(address: String, module: String = "") =
        if (module.isEmpty()) {
            violasApi.getBalance(address)
        } else {
            violasApi.getBalance(address, module)
        }

    fun getSequenceNumber(address: String) =
        violasApi.getSequenceNumber(address)

    fun pushTx(tx: String): Single<ViolasApiResponse<Any>> {
        val requestBody = Gson().toJson(SignedTxnDTO(tx))
            .toRequestBody("application/json".toMediaTypeOrNull())
        return violasApi.pushTx(requestBody)
    }

    fun getSupportCurrency() =
        violasApi.getSupportCurrency()

    fun checkRegisterToken(address: String, modu: String) =
        violasApi.checkRegisterToken(address, modu)

}