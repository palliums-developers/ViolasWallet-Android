package com.violas.wallet.repository.http.diem.violas

import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getDiemTxnDetailsUrl
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.DiemCurrencyAssetVo

/**
 * Created by elephant on 2020/4/22 19:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class DiemViolasService(
    private val repository: DiemViolasRepository
) : TransactionRecordService {

    private val diemTokens by lazy {
        WalletAppViewModel.getInstance().mAssetsLiveData.value
            ?.filter { it is DiemCurrencyAssetVo && it.getCoinNumber() == getDiemCoinType().coinNumber() }
            ?.associate { (it as DiemCurrencyAssetVo).currency.module to it.getAssetsName() }
            ?: emptyMap()
    }

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
            repository.getTransactionRecords(
                walletAddress,
                tokenId,
                pageSize,
                (pageNumber - 1) * pageSize,
                when (transactionType) {
                    TransactionType.TRANSFER -> 0
                    TransactionType.COLLECTION -> 1
                    else -> null
                }
            )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, dto ->

            // 解析交易状态
            val transactionState =
                if (dto.status.equals("Executed", true))
                    TransactionState.SUCCESS
                else
                    TransactionState.FAILURE

            // 解析交易类型
            val realTransactionType =
                if (dto.type.equals("ADD_CURRENCY_TO_ACCOUNT", true)
                    || dto.type.equals("0", true)
                ) {
                    TransactionType.ADD_CURRENCY
                } else if (dto.sender != walletAddress && dto.receiver == walletAddress) {
                    TransactionType.COLLECTION
                } else if (dto.sender == walletAddress && !dto.receiver.isNullOrBlank()) {
                    TransactionType.TRANSFER
                } else {
                    TransactionType.OTHER
                }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = getDiemCoinType(),
                transactionType = realTransactionType,
                transactionState = transactionState,
                fromAddress = dto.sender,
                toAddress = dto.receiver,
                time = dto.confirmedTime,
                amount = dto.amount,
                tokenId = tokenId,
                tokenDisplayName = tokenDisplayName,
                gas = dto.gas,
                gasTokenId = dto.gasCurrency,
                gasTokenDisplayName = diemTokens[dto.gasCurrency] ?: tokenDisplayName,
                transactionId = dto.version.toString(),
                url = getDiemTxnDetailsUrl(dto.version.toString())
            )
        }
        onSuccess.invoke(list, null)
    }

    suspend fun activateWallet(
        address: String,
        authKeyPrefix: String
    ) =
        repository.activateWallet(address, authKeyPrefix)

    suspend fun getCurrencies() = repository.getCurrencies().data?.currencies
}