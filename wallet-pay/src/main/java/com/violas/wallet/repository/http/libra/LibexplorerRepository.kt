package com.violas.wallet.repository.http.libra

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.http.TransactionRepository
import com.violas.wallet.repository.http.checkResponse
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2019-11-08 18:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer repository
 */
class LibexplorerRepository(private val libexplorerApi: LibexplorerApi) :
    TransactionRepository {

    override suspend fun getTransactionRecord(
        address: String,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        checkResponse {
            libexplorerApi.getTransactionRecord(address, pageSize, pageNumber)

        }.onSuccess {

            if (it.data.isNullOrEmpty()) {
                onSuccess.invoke(emptyList(), null)
                return@onSuccess
            }

            val list = it.data!!.map { bean ->

                // 解析交易类型，暂时只分收款和付款
                var transactionType = if (bean.from == address) {
                    TransactionRecordVO.TRANSACTION_TYPE_PAYMENT
                } else {
                    TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                }

                // 解析展示地址，收款付款均为对方地址
                var showAddress = if (TransactionRecordVO.isReceipt(transactionType)) {
                    bean.from
                } else {
                    bean.to
                }

                TransactionRecordVO(
                    id = bean.sequenceNumber,
                    coinTypes = CoinTypes.Libra,
                    transactionType = transactionType,
                    time = bean.expirationTime * 1000,
                    amount = bean.value,
                    address = showAddress,
                    url = "https://libexplorer.com/version/${bean.version}"
                )
            }

            onSuccess.invoke(list, null)

        }.onFailure(onFailure)
    }

}