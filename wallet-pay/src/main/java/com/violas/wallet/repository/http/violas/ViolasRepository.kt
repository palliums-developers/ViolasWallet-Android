package com.violas.wallet.repository.http.violas

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.http.TransactionRepository
import com.violas.wallet.repository.http.checkResponse
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas repository
 */
class ViolasRepository(private val violasApi: ViolasApi) :
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
            violasApi.getTransactionRecord(address, pageSize, (pageNumber - 1) * pageSize)

        }.onSuccess {

            if (it.data.isNullOrEmpty()) {
                onSuccess.invoke(emptyList(), null)
                return@onSuccess
            }

            val list = it.data!!.mapIndexed { index, bean ->
                // 解析交易类型
                val transactionType = when {
                    bean.type == 1 ->
                        TransactionRecordVO.TRANSACTION_TYPE_OPEN_STABLE_COIN

                    bean.sender == address -> {
                        if (bean.receiver_module.isNotEmpty()) {
                            TransactionRecordVO.TRANSACTION_TYPE_STABLE_COIN_TRANSFER
                        } else {
                            TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                        }
                    }

                    else -> {
                        if (bean.sender_module.isNotEmpty()) {
                            TransactionRecordVO.TRANSACTION_TYPE_STABLE_COIN_RECEIPT
                        } else {
                            TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                        }
                    }
                }

                // 解析展示地址，收款付款均为对方地址
                val showAddress = when {
                    bean.type == 1 || bean.sender == address ->
                        bean.receiver

                    else ->
                        bean.sender
                }

                val coinName = if (TransactionRecordVO.isStableCoinOpt(transactionType)) {
                    // TODO 解析 coinName = bean.coin_name
                    "Xcoin"
                } else {
                    CoinTypes.VToken.coinName()
                }

                TransactionRecordVO(
                    id = (pageNumber - 1) * pageSize + index,
                    coinTypes = CoinTypes.VToken,
                    transactionType = transactionType,
                    time = bean.expiration_time * 1000,
                    amount = bean.amount,
                    address = showAddress,
                    coinName = coinName
                )
            }
            onSuccess.invoke(list, null)

        }.onFailure(onFailure)
    }
}