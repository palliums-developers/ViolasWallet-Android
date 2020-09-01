package com.violas.wallet.repository.http.bank

import com.palliums.net.await

class BankRepository(private val api: BankApi) {

    /**
     * 获取账户银行信息
     */
    suspend fun getAccountInfo(
        address: String
    ) =
        api.getAccountInfo(address).await().data
            ?: AccountInfoDTO("0", "0", "0", "0")

    /**
     * 获取存款产品列表
     */
    suspend fun getDepositProducts() =
        api.getDepositProducts().await().data ?: emptyList()

    /**
     * 获取借贷产品列表
     */
    suspend fun getBorrowingProducts() =
        api.getBorrowingProducts().await().data ?: emptyList()

    /**
     * 获取存款产品信息
     * @param id 业务 ID
     * @param address
     */
    suspend fun getDepositProductDetails(
        id: String,
        address: String
    ) =
        api.getDepositProductDetails(id, address).await().data


    /**
     * 获取存款信息列表
     */
    suspend fun getDepositInfos(
        address: String,
        limit: Int,
        offset: Int
    ) =
        api.getDepositInfos(address, limit, offset).await().data ?: emptyList()

    /**
     * 获取存款订单列表
     */
    suspend fun getDepositOrderList(
        address: String,
        currency: String,
        status: Int,
        offset: Int,
        limit: Int
    ): List<DepositOrderDTO>? {
        return api.getDepositOrderList(address, currency, status, offset, limit).await().data
    }

    /**
     * 获取借贷产品信息
     * @param id
     * @param address
     */
    suspend fun getBorrowProductDetails(
        id: String,
        address: String
    ) =
        api.getBorrowProductDetails(id, address).await().data

    /**
     * 获取借贷信息
     */
    suspend fun getBorrowingInfos(
        address: String,
        limit: Int,
        offset: Int
    ) =
        api.getBorrowingInfos(address, limit, offset).await().data ?: emptyList()

    /**
     * 获取借贷订单列表
     */
    suspend fun getBorrowOrderList(
        address: String,
        currency: String,
        status: Int,
        offset: Int,
        limit: Int
    ): List<BorrowOrderDTO>? {
        return api.getBorrowOrderList(address, currency, status, offset, limit).await().data
    }

    /**
     * 获取借贷订单详情
     */
    suspend fun getBorrowDetail(
        address: String,
        id: String,
        q: Int,
        offset: Int,
        limit: Int
    ): BorrowOrderDetailDTO? {
        return api.getBorrowDetail(address, id, q, offset, limit).await().data
    }
}