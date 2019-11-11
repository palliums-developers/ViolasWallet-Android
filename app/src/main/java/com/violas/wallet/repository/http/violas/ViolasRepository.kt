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

            val list = it.data!!.map { bean ->

                TransactionRecordVO(
                    id = bean.sequence_number,
                    coinTypes = CoinTypes.Libra,
                    transactionType = TransactionRecordVO.TRANSACTION_TYPE_RECEIPT,
                    time = bean.expiration_time * 1000,
                    amount = bean.value,
                    address = address
                )
            }
            onSuccess.invoke(list, null)

        }.onFailure(onFailure)
    }
}