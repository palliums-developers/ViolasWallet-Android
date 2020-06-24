package com.violas.wallet.repository.http.libra.violas

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsTokenVo

/**
 * Created by elephant on 2020/4/22 19:02.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraViolasService(
    private val repository: LibraViolasRepository
) : TransactionRecordService {

    private val libraTokens by lazy {
        WalletAppViewModel.getViewModelInstance().mAssetsListLiveData.value
            ?.filter { it is AssetsTokenVo && it.getCoinNumber() == CoinTypes.Libra.coinType() }
            ?.associate { (it as AssetsTokenVo).module to it.getAssetsName() }
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
            val transactionState = if (dto.status == 4001)
                TransactionState.SUCCESS
            else
                TransactionState.FAILURE

            // 解析交易类型
            val realTransactionType = if (dto.type == 0) {
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
                coinType = CoinTypes.Libra,
                transactionType = realTransactionType,
                transactionState = transactionState,
                fromAddress = dto.sender,
                toAddress = dto.receiver,
                time = dto.expiration_time,
                amount = dto.amount,
                tokenId = tokenId,
                tokenDisplayName = tokenDisplayName,
                gas = dto.gas,
                gasTokenId = dto.gasCurrency,
                gasTokenDisplayName = libraTokens[dto.gasCurrency] ?: tokenDisplayName,
                transactionId = dto.version.toString(),
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

    suspend fun getCurrencies() = repository.getCurrencies().data?.currencies
}