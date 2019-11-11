package com.smallraw.core.http.violas

import com.google.gson.Gson
import com.smallraw.core.http.BaseRequest
import com.smallraw.core.http.libra.SignedTxnRequest
import io.reactivex.Observable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Path

class ViolasRepository(private val violasApi: ViolasApi) {
    fun getBalance(@Path("address") address: String) =
        violasApi.getBalance(address)

    fun getSequenceNumber(@Path("address") address: String) =
        violasApi.getSequenceNumber(address)

    fun pushTx(tx: String): Observable<BaseRequest<Any>> {
        val requestBody =
            Gson().toJson(SignedTxnRequest(tx))
                .toRequestBody("application/json".toMediaTypeOrNull())
        return violasApi.pushTx(requestBody)
    }

    fun loadTransaction(
        @Path("address") address: String,
        @Path("limit") limit: Int,
        @Path("offset") offset: Int
    ):
            Observable<BaseRequest<List<TransactionResponse>>> {
        return violasApi.loadTransaction(address, limit, offset)
    }

    fun getSupportCurrency() = violasApi.getSupportCurrency()
}
