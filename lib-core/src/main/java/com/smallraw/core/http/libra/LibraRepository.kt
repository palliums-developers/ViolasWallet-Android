package com.smallraw.core.http.libra

import com.google.gson.Gson
import com.smallraw.core.http.BaseRequest
import io.reactivex.Observable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Path

class LibraRepository(private val libraApi: LibraApi) {
    fun getBalance(@Path("address") address: String) =
        libraApi.getBalance(address)

    fun getSequenceNumber(@Path("address") address: String) =
        libraApi.getSequenceNumber(address)

    fun pushTx(tx: String): Observable<BaseRequest<Any>> {
        val requestBody =
            Gson().toJson(SignedTxnRequest(tx))
                .toRequestBody("application/json".toMediaTypeOrNull())
        return libraApi.pushTx(requestBody)
    }

    fun loadTransaction(
        @Path("address") address: String,
        @Path("limit") limit: Int,
        @Path("offset") offset: Int
    ):
            Observable<BaseRequest<List<TransactionResponse>>> {
        return libraApi.loadTransaction(address, limit, offset)
    }
}
