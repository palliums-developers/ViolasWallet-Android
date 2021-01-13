package com.violas.wallet.repository.http.violas

import com.palliums.violas.http.ViolasRepository
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas biz service
 */
class ViolasBizService(private val mViolasRepository: ViolasRepository) : TransactionService {

    override suspend fun getTransactionRecord(
        address: String,
        tokenId: String?,
        tokenName: String?,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        val response =
            mViolasRepository.getTransactionRecords(
                address,
                tokenId,
                pageSize,
                (pageNumber - 1) * pageSize,
                null
            )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, bean ->
            // 解析交易类型
            val transactionType =
                if (bean.type.equals("ADD_CURRENCY_TO_ACCOUNT", true)) {
                    TransactionRecordVO.TRANSACTION_TYPE_OPEN_TOKEN
                } else if (bean.sender != address && bean.receiver == address) {
                    TransactionRecordVO.TRANSACTION_TYPE_TOKEN_RECEIPT
                } else if (bean.sender == address && !bean.receiver.isNullOrBlank()) {
                    TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER
                } else {
                    TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER
                }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = when (bean.sender) {
                address -> bean.receiver ?: ""
                else -> bean.sender
            }

            // 解析币名称
            val coinName = if (TransactionRecordVO.isTokenOpt(transactionType)) {
                tokenName ?: bean.currency
            } else {
                null
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinTypes = CoinTypes.Violas,
                transactionType = transactionType,
                time = bean.expiration_time * 1000,
                amount = bean.amount,
                gas = bean.gas,
                address = showAddress,
                url = BaseBrowserUrl.getViolasBrowserUrl(bean.version.toString()),
                coinName = coinName
            )
        }
        onSuccess.invoke(list, null)
    }
}