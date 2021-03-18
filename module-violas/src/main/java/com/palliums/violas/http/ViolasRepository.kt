package com.palliums.violas.http

import com.google.gson.Gson
import com.palliums.exceptions.RequestException
import com.palliums.net.await
import com.palliums.violas.error.ViolasException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class ViolasRepository(private val mViolasApi: ViolasApi) {

    /**
     * 获取平台币余额
     */
    @Throws(RequestException::class)
    suspend fun getBalance(
        walletAddress: String
    ) =
        mViolasApi.getBalance(walletAddress).await().data?.balance ?: 0

    @Throws(ViolasException::class, RequestException::class)
    suspend fun pushTx(
        tx: String
    ) =
        mViolasApi.pushTx(
            Gson().toJson(SignedTxnDTO(tx))
                .toRequestBody("application/json".toMediaTypeOrNull())
        ).await(
            customError = { ViolasException.checkViolasTransactionException(it) }
        )

    /**
     * 获取交易记录
     */
    @Throws(RequestException::class)
    suspend fun getTransactionRecords(
        address: String,
        tokenId: String?,
        pageSize: Int,
        offset: Int,
        transactionType: Int?
    ) =
        mViolasApi.getTransactionRecords(
            address, tokenId, pageSize, offset, transactionType
        ).await()

    /**
     * 获取账户信息
     */
    suspend fun getAccountState(
        address: String
    ) =
        mViolasApi.getAccountState(address).await()

    /**
     * 激活账户
     */
    suspend fun activateWallet(
        address: String,
        authKeyPrefix: String
    ) =
        mViolasApi.activateWallet(address, authKeyPrefix).await()

    /**
     * 登录网页端钱包
     */
    suspend fun loginWeb(
        loginType: Int,
        sessionId: String,
        walletAccounts: List<WalletAccountDTO>
    ) =
        mViolasApi.loginWeb(
            Gson().toJson(LoginWebDTO(loginType, sessionId, walletAccounts))
                .toRequestBody("application/json".toMediaTypeOrNull())
        ).await()

    @Throws(RequestException::class)
    suspend fun getCurrencies() =
        mViolasApi.getCurrency().await()

    @Throws(RequestException::class)
    suspend fun getBTCChainFiatBalance(
        address: String
    ) =
        mViolasApi.getBTCChainFiatBalance(address).await()

    @Throws(RequestException::class)
    suspend fun getLibraChainFiatBalance(
        address: String
    ) =
        mViolasApi.getLibraChainFiatBalance(address).await()

    @Throws(RequestException::class)
    suspend fun getViolasChainFiatBalance(
        address: String
    ) =
        mViolasApi.getViolasChainFiatBalance(address).await()

}