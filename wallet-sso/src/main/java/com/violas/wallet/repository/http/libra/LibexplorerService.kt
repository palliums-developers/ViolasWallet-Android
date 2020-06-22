package com.violas.wallet.repository.http.libra

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2019-11-08 18:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer service
 */
class LibexplorerService(private val mLibexplorerRepository: LibexplorerRepository) :
    TransactionService {

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
            mLibexplorerRepository.getTransactionRecord(address, pageSize, pageNumber)

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, bean ->

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
                gas = bean.gasUsed.toString(),
                address = showAddress,
                url = BaseBrowserUrl.getLibraBrowserUrl(bean.version)
            )
        }
        onSuccess.invoke(list, null)
    }
}