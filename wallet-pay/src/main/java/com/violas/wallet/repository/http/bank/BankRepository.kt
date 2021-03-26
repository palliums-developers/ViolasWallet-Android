package com.violas.wallet.repository.http.bank

import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.palliums.net.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class BankRepository(private val api: BankApi) {

    /**
     * 获取账户信息
     */
    suspend fun getAccountInfo(
        address: String
    ) =
        api.getAccountInfo(address).await().data

    /**
     * 获取存款产品列表
     */
    suspend fun getDepositProducts() =
        api.getDepositProducts().await().data ?: emptyList()

    /**
     * 获取存款产品详情
     * @param id 业务 ID
     * @param address
     */
    suspend fun getDepositProductDetails(
        id: String,
        address: String
    ) =
        api.getDepositProductDetails(id, address).await().data

    /**
     * 分页获取存款信息
     */
    suspend fun getDepositInfos(
        address: String,
        limit: Int,
        offset: Int
    ) =
        api.getDepositInfos(address, limit, offset).await().data ?: emptyList()

    /**
     * 获取存款详情
     * @param id 业务 ID
     * @param address
     */
    suspend fun getDepositDetails(
        id: String,
        address: String
    ) =
        api.getDepositDetails(id, address).await(dataNullableOnSuccess = false).data!!

    /**
     * 分页获取存款记录
     */
    suspend fun getDepositRecords(
        address: String,
        currency: String?,
        state: Int?,
        limit: Int,
        offset: Int
    ) =
        api.getDepositRecords(address, currency, state, limit, offset).await().data
            ?: emptyList()

    /**
     * 获取借贷产品列表
     */
    suspend fun getBorrowingProducts() =
        api.getBorrowingProducts().await().data ?: emptyList()

    /**
     * 获取借贷产品详情
     */
    suspend fun getBorrowProductDetails(
        id: String,
        address: String
    ) =
        api.getBorrowProductDetails(id, address).await().data

    /**
     * 分页获取借贷信息
     */
    suspend fun getBorrowingInfos(
        address: String,
        limit: Int,
        offset: Int
    ) =
        api.getBorrowingInfos(address, limit, offset).await().data ?: emptyList()

    /**
     * 分页获取借贷记录
     */
    suspend fun getBorrowingRecords(
        address: String,
        currency: String?,
        state: Int?,
        limit: Int,
        offset: Int
    ) =
        api.getBorrowingRecords(address, currency, state, limit, offset).await().data
            ?: emptyList()

    /**
     * 分页获取借贷产品的借款记录
     */
    suspend fun getCoinBorrowingRecords(
        address: String,
        id: String,
        limit: Int,
        offset: Int
    ) =
        api.getCoinBorrowingRecords(address, id, limit, offset).await().data

    /**
     * 分页获取借贷产品的还款记录
     */
    suspend fun getCoinRepaymentRecords(
        address: String,
        id: String,
        limit: Int,
        offset: Int
    ) =
        api.getCoinRepaymentRecords(address, id, limit, offset).await().data

    /**
     * 分页获取借贷产品的清算记录
     */
    suspend fun getCoinLiquidationRecords(
        address: String,
        id: String,
        limit: Int,
        offset: Int
    ) =
        api.getCoinLiquidationRecords(address, id, limit, offset).await().data

    /**
     * 获取借贷详情
     */
    suspend fun getBorrowingDetails(
        address: String,
        id: String
    ) = api.getBorrowingDetails(address, id).await().data

    @Keep
    data class SubmitTransaction(
        val address: String,
        @SerializedName(value = "product_id")
        val productId: String,
        val value: Long,
        val sigtxn: String
    )

    /**
     * 存款交易上链
     */
    suspend fun submitDepositTransaction(
        address: String,
        productId: String,
        value: Long,
        sigtxn: String
    ) {
        val requestBody = Gson()
            .toJson(SubmitTransaction(address, productId, value, sigtxn))
            .toRequestBody("application/json".toMediaTypeOrNull())
        api.submitDepositTransaction(requestBody).await().data
    }

    /**
     * 取款交易上链
     */
    suspend fun submitRedeemTransaction(
        address: String,
        productId: String,
        value: Long,
        sigtxn: String
    ) {
        val requestBody = Gson()
            .toJson(SubmitTransaction(address, productId, value, sigtxn))
            .toRequestBody("application/json".toMediaTypeOrNull())
        api.submitRedeemTransaction(requestBody).await().data
    }

    /**
     * 借款交易上链
     */
    suspend fun submitBorrowTransaction(
        address: String,
        productId: String,
        value: Long,
        sigtxn: String
    ) {
        val requestBody = Gson()
            .toJson(SubmitTransaction(address, productId, value, sigtxn))
            .toRequestBody("application/json".toMediaTypeOrNull())
        api.submitBorrowTransaction(requestBody).await().data
    }

    /**
     * 还款交易上链
     */
    suspend fun submitRepayBorrowTransaction(
        address: String,
        productId: String,
        value: Long,
        sigtxn: String
    ) {
        val requestBody = Gson()
            .toJson(SubmitTransaction(address, productId, value, sigtxn))
            .toRequestBody("application/json".toMediaTypeOrNull())
        api.submitRepayBorrowTransaction(requestBody).await().data
    }
}