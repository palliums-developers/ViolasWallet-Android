package com.violas.wallet.repository.http

import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionType

/**
 * Created by elephant on 2019-11-08 11:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
interface TransactionRecordService {

    /**
     * 获取交易记录
     * @param walletAddress         钱包地址
     * @param tokenAddress          token地址
     * @param tokenName             token名称
     * @param transactionType       交易类型
     * @param pageSize              分页大小，默认为10
     * @param pageNumber            页码，从1开始
     * @param pageKey               页面键，来源上一次[onSuccess]返回的第二个数据，开始为null
     * @param onSuccess             成功回调
     */
    suspend fun getTransactionRecords(
        walletAddress: String,
        tokenAddress: String?,
        tokenName: String?,
        @TransactionType
        transactionType: Int,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    )
}