package com.violas.wallet.repository.http.libra.libexplore

import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getDiemTxnDetailsUrl
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import java.util.*

/**
 * Created by elephant on 2019-11-08 18:04.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: LibExplorer service
 */
class LibraLibexplorerService(
    private val repository: LibraLibexplorerRepository
) : TransactionRecordService {

    override suspend fun getTransactionRecords(
        walletAddress: String,
        tokenId: String?,
        tokenDisplayName: String?,
        transactionType: Int,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        val response =
            repository.getTransactionRecords(walletAddress, pageSize, pageNumber)

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, dto ->

            // 解析交易状态
            val transactionState = when (dto.status.toUpperCase(Locale.ENGLISH)) {
                "PENDING" ->
                    TransactionState.PENDING

                "SUCCESS" ->
                    TransactionState.SUCCESS

                else ->
                    TransactionState.FAILURE
            }

            // 解析交易类型
            val realTransactionType = if (dto.from == walletAddress) {
                if (dto.to.isBlank()) {
                    TransactionType.ADD_CURRENCY
                } else {
                    TransactionType.TRANSFER
                }
            } else {
                TransactionType.COLLECTION
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = getDiemCoinType(),
                transactionType = realTransactionType,
                transactionState = transactionState,
                time = dto.expirationTime,
                fromAddress = dto.from,
                toAddress = dto.to,
                amount = dto.value,
                tokenId = tokenId,
                tokenDisplayName = tokenDisplayName,
                gas = dto.gasUsed.toString(),
                gasTokenId = tokenId,
                gasTokenDisplayName = tokenDisplayName,
                transactionId = dto.version,
                url = getDiemTxnDetailsUrl(dto.version)
            )
        }
        onSuccess.invoke(list, null)
    }
}