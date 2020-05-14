package com.violas.wallet.repository.http.violas

import com.palliums.violas.http.Response
import com.palliums.violas.http.ViolasRepository
import com.palliums.violas.http.WalletAccountDTO
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas service
 */
class ViolasBizService(private val mViolasRepository: ViolasRepository) : TransactionService {

    suspend fun loginWeb(
        loginType: Int,
        sessionId: String,
        accounts: List<AccountDO>
    ): Response<Any> {
        val walletAccounts = accounts.map {
            WalletAccountDTO(
                walletType = it.walletType,
                coinType = when (it.coinNumber) {
                    CoinTypes.Violas.coinType() -> "violas"
                    CoinTypes.Libra.coinType() -> "libra"
                    else -> "bitcoin"
                },
                walletName = it.walletNickname,
                walletAddress = it.address
            )
        }
        return mViolasRepository.loginWeb(loginType, sessionId, walletAccounts)
    }

    override suspend fun getTransactionRecord(
        address: String,
        tokenAddress: String?,
        tokenName: String?,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        val response =
            mViolasRepository.getTransactionRecord(
                address,
                pageSize,
                (pageNumber - 1) * pageSize,
                tokenAddress
            )

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, bean ->
            // 解析交易类型
            val transactionType = when (bean.type) {
                9 -> TransactionRecordVO.TRANSACTION_TYPE_OPEN_TOKEN

                1, 2 -> {
                    if (bean.sender == address) {
                        TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                    } else {
                        TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                    }
                }

                7, 12, 13 -> {
                    if (bean.sender == address) {
                        TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER
                    } else {
                        TransactionRecordVO.TRANSACTION_TYPE_TOKEN_RECEIPT
                    }
                }

                else -> {
                    if ((!bean.module_name.isNullOrEmpty()
                                && !bean.module_name.equals(CoinTypes.Violas.coinName(), true))
                        || !tokenName.isNullOrEmpty()
                    ) {
                        if (bean.sender == address) {
                            TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER
                        } else {
                            TransactionRecordVO.TRANSACTION_TYPE_TOKEN_RECEIPT
                        }
                    } else {
                        if (bean.sender == address) {
                            TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                        } else {
                            TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                        }
                    }
                }
            }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = when (bean.sender) {
                address -> bean.receiver ?: ""
                else -> bean.sender
            }

            // 解析币名称
            val coinName = if (TransactionRecordVO.isTokenOpt(transactionType)) {
                tokenName ?: bean.module_name
            } else {
                null
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinTypes = CoinTypes.Violas,
                transactionType = transactionType,
                time = bean.expiration_time * 1000,
                amount = bean.amount,
                gas = bean.gas,
                address = showAddress,
                url = BaseBrowserUrl.getViolasBrowserUrl(bean.version.toString()),
                coinName = coinName
            )
        }
        onSuccess.invoke(list, null)
    }
}