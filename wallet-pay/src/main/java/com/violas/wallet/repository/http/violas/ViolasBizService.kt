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

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas service
 */
class ViolasBizService(
    private val repository: ViolasRepository
) : TransactionRecordService {

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
        tokenAddress: String?,
        transactionType: Int,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        // TODO 接口需要升级，需要支持查询指定交易类型的交易记录
        val response =
            repository.getTransactionRecords(
                walletAddress,
                pageSize,
                (pageNumber - 1) * pageSize,
                tokenAddress
            )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, dto ->

            // TODO 解析交易状态
            val transactionState = TransactionState.SUCCESS

            // 解析交易类型
            val transactionType = if (dto.type == 9) {
                TransactionType.REGISTER
            } else if (dto.sender == walletAddress) {
                if (dto.receiver.isNullOrBlank()) {
                    TransactionType.REGISTER
                } else {
                    TransactionType.TRANSFER
                }
            } else {
                TransactionType.COLLECTION
            }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = if (transactionType == TransactionType.TRANSFER) {
                dto.receiver!!
            } else {
                dto.sender
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = CoinTypes.Violas,
                transactionType = transactionType,
                transactionState = transactionState,
                time = dto.expiration_time,
                fromAddress = showAddress,
                amount = dto.amount,
                gas = dto.gas,
                url = BaseBrowserUrl.getViolasBrowserUrl(dto.version.toString())
            )
        }
        onSuccess.invoke(list, null)
    }
}