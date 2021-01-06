package com.violas.wallet.repository.http.libra.violas

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by elephant on 2019-11-11 15:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas libra api
 * @see <a href="https://github.com/palliums-developers/violas-webservice">link</a>
 */
interface LibraViolasApi {

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address           地址
     * @param tokenId           token唯一标识，null时表示查询平台币的交易记录
     * @param pageSize          分页大小
     * @param offset            偏移量，从0开始
     * @param transactionType   交易类型，null：全部；0：转出；1：转入
     */
    @GET("/1.0/libra/transaction")
    fun getTransactionRecords(
        @Query("addr") address: String,
        @Query("currency") tokenId: String?,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int,
        @Query("flows") transactionType: Int?
    ): Observable<ListResponse<TransactionRecordDTO>>

    /**
     * 获取 Violas 钱包支持的代币列表
     */
    @GET("/1.0/libra/currency")
    fun getCurrency(): Observable<Response<CurrencysDTO>>

    @GET("/1.0/libra/mint")
    fun activateAccount(
        @Query("address") address: String,
        @Query("auth_key_perfix") authKeyPrefix: String
    ): Observable<Response<Any>>

}

data class TransactionRecordDTO(
    @SerializedName(value = "sender")
    val sender: String = "",
    @SerializedName(value = "receiver")
    val receiver: String? = null,
    @SerializedName(value = "amount")
    val amount: String = "",
    @SerializedName(value = "currency")
    val currency: String = "",
    @SerializedName(value = "gas")
    val gas: String = "",
    @SerializedName(value = "gas_currency")
    val gasCurrency: String = "",
    @SerializedName(value = "expiration_time")
    val expiration_time: Long = 0,
    @SerializedName(value = "sequence_number")
    val sequence_number: Long = 0,
    @SerializedName(value = "version")
    val version: Long = 0,
    @SerializedName(value = "type")
    val type: String = "",
    @SerializedName(value = "status")
    val status: String = ""
)

@Keep
data class CurrencysDTO(
    val currencies: List<CurrencyDTO>
)

@Keep
data class CurrencyDTO(
    val address: String,
    val module: String,
    val name: String,
    @SerializedName(value = "show_icon")
    val showLogo: String,
    @SerializedName(value = "show_name")
    val showName: String
)