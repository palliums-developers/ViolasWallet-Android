package com.smallraw.core.http.violas

import com.google.gson.Gson
import com.smallraw.core.http.BaseRequest
import com.smallraw.core.http.libra.SignedTxnRequest
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class ViolasRepository(private val violasApi: ViolasApi) {
    fun getBalance(address: String, modu: String = "") =
        if (modu.isEmpty()) {
            violasApi.getBalance(address)
        } else {
            violasApi.getBalance(address, modu)
        }


    fun getSequenceNumber(address: String) =
        violasApi.getSequenceNumber(address)

    fun pushTx(tx: String): Single<BaseRequest<Any>> {
        val requestBody =
            Gson().toJson(SignedTxnRequest(tx))
                .toRequestBody("application/json".toMediaTypeOrNull())
        return violasApi.pushTx(requestBody)
    }

    fun loadTransaction(
        address: String,
        limit: Int,
        offset: Int
    ):
            Single<BaseRequest<List<TransactionResponse>>> {
        return violasApi.loadTransaction(address, limit, offset)
    }

    fun getSupportCurrency() = violasApi.getSupportCurrency()

    fun checkRegisterToken(
        address: String
    ) = violasApi.checkRegisterToken(address)
}
