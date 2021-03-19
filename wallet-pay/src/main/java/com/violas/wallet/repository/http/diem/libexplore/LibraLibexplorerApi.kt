package com.violas.wallet.repository.http.diem.libexplore

import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_KEY_URL_NAME
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor.Companion.HEADER_VALUE_LIBEXPLORER
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_KEY_CHAIN_NAME
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_VALUE_DIEM_CHAIN
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Created by elephant on 2019-11-08 11:52.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer api
 * @see <a href="https://libexplorer.com/apis">link</a>
 */
interface LibraLibexplorerApi {

    /**
     * 获取指定地址的交易记录，分页查询
     * @param address 地址
     * @param pageSize 分页大小
     * @param pageNumber 页码，从1开始
     */
    @Headers(
        value = [
            "${HEADER_KEY_URL_NAME}:${HEADER_VALUE_LIBEXPLORER}",
            "${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_DIEM_CHAIN}"
        ]
    )
    @GET("?module=account&action=txlist&sort=desc")
    fun getTransactionRecords(
        @Query("address") address: String,
        @Query("offset") pageSize: Int,
        @Query("page") pageNumber: Int
    ): Observable<ListResponse<TransactionRecordDTO>>

}