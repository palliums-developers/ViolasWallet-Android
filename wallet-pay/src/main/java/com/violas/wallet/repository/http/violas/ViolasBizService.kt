package com.violas.wallet.repository.http.violas

import com.palliums.violas.http.Response
import com.palliums.violas.http.ViolasRepository
import com.palliums.violas.http.WalletAccountDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsTokenVo

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas service
 */
class ViolasBizService(
    private val repository: ViolasRepository
) : TransactionRecordService {

    private val violasTokens by lazy {
        WalletAppViewModel.getViewModelInstance().mAssetsListLiveData.value
            ?.filter { it is AssetsTokenVo && it.getCoinNumber() == CoinTypes.Violas.coinType() }
            ?.associate { (it as AssetsTokenVo).module to it.getAssetsName() }
            ?: emptyMap()
    }

    suspend fun loginWeb(
        loginType: Int,
        sessionId: String,
        accounts: List<AccountDO>
    ): Response<Any> {
        val walletAccounts = accounts.map {
            WalletAccountDTO(
                coinType = when (it.coinNumber) {
                    CoinTypes.Violas.coinType() -> "violas"
                    CoinTypes.Libra.coinType() -> "libra"
                    else -> "bitcoin"
                },
                walletName = "",
                walletAddress = it.address,
                walletType = 0
            )
        }
        return repository.loginWeb(loginType, sessionId, walletAccounts)
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
                coinType = CoinTypes.Violas,
                transactionType = realTransactionType,
                transactionState = transactionState,
                time = dto.expiration_time,
                fromAddress = dto.sender,
                toAddress = dto.receiver,
                amount = dto.amount,
                tokenId = tokenId,
                tokenDisplayName = tokenDisplayName,
                gas = dto.gas,
                gasTokenId = dto.gasCurrency,
                gasTokenDisplayName = violasTokens[dto.gasCurrency] ?: tokenDisplayName,
                transactionId = dto.version.toString(),
                url = BaseBrowserUrl.getViolasBrowserUrl(dto.version.toString())
            )
        }
        onSuccess.invoke(list, null)
    }
}