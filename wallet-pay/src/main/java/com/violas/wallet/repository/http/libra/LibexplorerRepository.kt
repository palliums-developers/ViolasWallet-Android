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

            val list = it.data!!.mapIndexed { index, bean ->

                // 解析交易类型，暂时只分收款和付款
                val transactionType = if (bean.from == address) {
                    TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                } else {
                    TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                }

                // 解析展示地址，收款付款均为对方地址
                val showAddress = if (bean.from == address) {
                    bean.to
                } else {
                    bean.from
                }

                TransactionRecordVO(
                    id = (pageNumber - 1) * pageSize + index,
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