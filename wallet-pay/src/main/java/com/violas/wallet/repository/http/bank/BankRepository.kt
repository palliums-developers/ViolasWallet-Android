package com.violas.wallet.repository.http.bank

import com.palliums.net.await

class BankRepository(private val api: BankApi) {
    /**
     * 账户信息
     */
    suspend fun getInfo(
        address: String
    ): AccountInfoDTO {
        return api.getInfo(address).await()
    }

    /**
     * 获取存款产品信息
     * @param id 业务 ID
     * @param address
     */
    suspend fun getDepositInfo(
        id: String,
        address: String
    ): DepositInfo {
        return api.getDepositInfo(id, address).await()
    }

    /**
     * 获取存款订单信息
     */
    suspend fun getDepositOrderInfos(
        address: String,
        offset: Int,
        limit: Int
    ): List<DepositOrderInfoDTO>? {
        return api.getDepositOrderInfos(address, offset, limit).await().data
    }

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
     * @param walletAddresses
     * @param pageSize
     * @param offset
     */
    suspend fun getBorrowInfo(
        id: String,
        address: String
    ): BorrowInfoDTO {
        return api.getBorrowInfo(id, address).await()
    }

    /**
     * 获取借贷订单信息
     */
    suspend fun getBorrowOrderInfos(
        address: String,
        offset: Int,
        limit: Int
    ): List<BorrowOrderInfoDTO>? {
        return api.getBorrowOrderInfos(address, offset, limit).await().data
    }

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
    ): BorrowOrderDetailDTO {
        return api.getBorrowDetail(address, id, q, offset, limit).await()
    }
}