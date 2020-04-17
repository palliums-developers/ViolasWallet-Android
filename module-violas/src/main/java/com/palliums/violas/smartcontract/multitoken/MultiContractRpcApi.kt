package com.palliums.violas.smartcontract.multitoken

import com.google.gson.annotations.SerializedName
import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.*

interface MultiContractRpcApi {
    /**
     * 获取余额
     * @param address 账号地址
     * @param tokenAddressArr 稳定币地址，多个稳定币地址以逗号分开，为空时只返回平台币的余额
     */
    @GET("/1.0/violas/balance")
    fun getBalance(
        @Query("addr") address: String,
        @Query("modu") tokenIdxs: String
    ): Call<Response<BalanceDTO>>

    @GET("/1.0/violas/balance")
    fun getBalance(
        @Query("addr") address: String
    ): Call<Response<BalanceDTO>>

    @GET("/1.0/violas/currency")
    fun getSupportCurrency(): Call<Response<SupportCurrencyListDTO>>//Single<ListResponse<SupportCurrencyDTO>>

    @GET("/1.0/violas/module")
    fun getRegisterToken(@Query("addr") address: String): Call<Response<PublishDTO>>
}

data class SupportCurrencyListDTO(
    var currencies: List<SupportCurrencyDTO>
)

data class SupportCurrencyDTO(
    @SerializedName(value = "id")
    var tokenIdentity: Long = 0,
    var name: String = ""
)

data class BalanceDTO(
    var address: String = "",
    var balance: Long = 0,
    var modules: List<ModuleDTO>? = null
)

data class ModuleDTO(
    var id: Long = 0,
    var balance: Long = 0,
    var name: String = ""
)

data class PublishDTO(
    @SerializedName(value = "is_published")
    var isPublished: Int = 0
)