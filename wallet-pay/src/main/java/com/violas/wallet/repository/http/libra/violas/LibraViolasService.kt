package com.violas.wallet.repository.http.libra.violas

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2020/4/22 19:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraViolasService(
    private val mRepository: LibraViolasRepository
) : TransactionService {

    override suspend fun getTransactionRecord(
        address: String,
        tokenAddress: String?,
        tokenName: String?,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        val response = mRepository.getTransactionRecord(
            address,
            pageSize,
            (pageNumber - 1) * pageSize
        )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, dto ->
            // 解析交易类型，暂时只分收款和付款
            val transactionType = if (dto.sender == address) {
                TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
            } else {
                TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
            }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = if (dto.sender == address) {
                dto.receiver ?: ""
            } else {
                dto.sender
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinTypes = CoinTypes.Libra,
                transactionType = transactionType,
                time = dto.expiration_time * 1000,
                amount = dto.amount,
                gas = dto.gas,
                address = showAddress,
                url = BaseBrowserUrl.getLibraBrowserUrl(dto.version.toString())
            )
        }
        onSuccess.invoke(list, null)
    }
}