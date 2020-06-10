package com.violas.wallet.repository.http.libra.violas

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType

/**
 * Created by elephant on 2020/4/22 19:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraViolasService(
    private val repository: LibraViolasRepository
) : TransactionRecordService {

    override suspend fun getTransactionRecords(
        walletAddress: String,
        tokenAddress: String?,
        transactionType: Int,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        // TODO 接口需要升级，需要支持查询指定交易类型的交易记录
        val response =
            repository.getTransactionRecords(
                walletAddress,
                pageSize,
                (pageNumber - 1) * pageSize
            )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, dto ->

            // TODO 解析交易状态
            val transactionState = TransactionState.SUCCESS

            // 解析交易类型，暂时只分收款和付款
            val transactionType = if (dto.sender == walletAddress) {
                if (dto.receiver.isNullOrBlank()) {
                    TransactionType.REGISTER
                } else {
                    TransactionType.TRANSFER
                }
            } else {
                TransactionType.COLLECTION
            }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = if (transactionType == TransactionType.TRANSFER) {
                dto.receiver!!
            } else {
                dto.sender
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = CoinTypes.Libra,
                transactionType = transactionType,
                transactionState = transactionState,
                fromAddress = showAddress,
                time = dto.expiration_time,
                amount = dto.amount,
                gas = dto.gas,
                url = BaseBrowserUrl.getLibraBrowserUrl(dto.version.toString())
            )
        }
        onSuccess.invoke(list, null)
    }

    suspend fun activateAccount(
        address: String,
        authKeyPrefix: String
    ) =
        repository.activateAccount(address, authKeyPrefix)

}