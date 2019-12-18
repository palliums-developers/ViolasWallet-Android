package com.palliums.violas.http

import com.google.gson.Gson
import com.palliums.net.RequestException
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

    @Throws(RequestException::class)
    suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        offset: Int,
        tokenAddress: String?
    ): ListResponse<TransactionRecordDTO> {
        return checkResponse {
            mViolasApi.getTransactionRecord(address, pageSize, offset, tokenAddress)
        }
    }

    fun getBalance(
        address: String,
        tokenAddressList: List<String>? = null
    ): Single<Response<BalanceDTO>> {
        val tokenAddressArr: String? = tokenAddressList?.joinToString(",")
        return mViolasApi.getBalance(address, tokenAddressArr)
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

    fun getRegisterToken(address: String) =
        mViolasApi.getRegisterToken(address)

}