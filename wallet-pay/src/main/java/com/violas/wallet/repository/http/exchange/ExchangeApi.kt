package com.violas.wallet.repository.http.exchange

import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_KEY_CHAIN_NAME
import com.violas.wallet.repository.http.interceptor.RequestHeaderInterceptor.Companion.HEADER_VALUE_VIOLAS_CHAIN
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Created by elephant on 2020-02-14 11:21.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface ExchangeApi {

    /**
     * 获取交易市场支持的币种
     */
    @GET("/1.0/market/exchange/currency")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getMarketSupportCurrencies(): Observable<ListResponse<MarketCurrencyDTO>>

    /**
     * 尝试计算兑换价值
     */
    @GET("/1.0/market/exchange/trial")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun exchangeSwapTrial(
        @Query("amount") amount: Long,
        @Query("currencyIn") currencyIn: String,
        @Query("currencyOut") currencyOut: String
    ): Observable<Response<SwapTrialSTO>>

    /**
     * 获取指定地址的交易市场资金池信息
     * @param address       地址
     */
    @GET("/1.0/market/pool/info")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getUserPoolInfo(
        @Query("address") address: String
    ): Observable<Response<UserPoolInfoDTO>>

    /**
     * 资金池转出试算
     * @param address           地址
     * @param tokenAName        token a名称
     * @param tokenBName        token b名称
     * @param liquidityAmount   流动性token数量
     */
    @GET("/1.0/market/pool/withdrawal/trial")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun removePoolLiquidityEstimate(
        @Query("address") address: String,
        @Query("coin_a") tokenAName: String,
        @Query("coin_b") tokenBName: String,
        @Query("amount") liquidityAmount: String
    ): Observable<Response<RemovePoolLiquidityEstimateResultDTO>>

    /**
     * 资金池转入估算
     * @param tokenAName        token a名称
     * @param tokenBName        token b名称
     * @param tokenAAmount      token a数量
     */
    @GET("/1.0/market/pool/deposit/trial")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun addPoolLiquidityEstimate(
        @Query("coin_a") tokenAName: String,
        @Query("coin_b") tokenBName: String,
        @Query("amount") tokenAAmount: String
    ): Observable<Response<AddPoolLiquidityEstimateResultDTO>>

    /**
     * 获取币种对储备信息
     * @param coinAModule        Coin A module
     * @param coinBModule        Coin B module
     */
    @GET("/1.0/market/pool/reserve/info")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getPoolLiquidityReserve(
        @Query("coin_a") coinAModule: String,
        @Query("coin_b") coinBModule: String
    ): Observable<Response<PoolLiquidityReserveInfoDTO>>

    /**
     * 获取支持的映射币交易对详情
     */
    @GET("/1.0/market/exchange/crosschain/address/info")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getMarketMappingPairInfo(): Observable<Response<List<MappingPairInfoDTO>>>

    /**
     * 获取其他链对应的 violas 映射币
     */
    @GET("/1.0/market/exchange/crosschain/map/relation")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getMarketPairRelation(): Observable<Response<List<MapRelationDTO>>>

    /**
     * 获取全部币种对储备信息
     */
    @GET("/1.0/market/pool/reserve/infos")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getMarketAllReservePair(): Observable<Response<List<PoolLiquidityReserveInfoDTO>>>

    /**
     * 获取交易市场资金池记录
     * @param walletAddress 地址
     * @param pageSize      分页大小
     * @param offset        偏移量，从0开始
     */
    @GET("/1.0/market/pool/transaction")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getPoolRecords(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<PoolRecordDTO>>

    /**
     * 获取交易市场Violas币币兑换和跨链兑换记录
     * @param walletAddresses   地址
     * @param pageSize          分页大小
     * @param offset            偏移量，从0开始
     */
    @GET("/1.0/market/crosschain/transaction")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getSwapRecords(
        @Query("addresses") walletAddresses: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<SwapRecordDTO>>

    /**
     * 获取交易市场Violas币币兑换记录
     */
    @GET("/1.0/market/exchange/transaction")
    @Headers(value = ["${HEADER_KEY_CHAIN_NAME}:${HEADER_VALUE_VIOLAS_CHAIN}"])
    fun getViolasSwapRecords(
        @Query("address") walletAddress: String,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int
    ): Observable<ListResponse<ViolasSwapRecordDTO>>

}