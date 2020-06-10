package com.violas.wallet.repository.http.bitcoin.bitmain

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType

/**
 * Created by elephant on 2019-11-07 18:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆 service
 */
class BitmainService(
    private val repository: BitmainRepository
) : TransactionRecordService {

    override suspend fun getTransactionRecords(
        walletAddress: String,
        tokenAddress: String?,
        tokenName: String?,
        transactionType: Int,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        val response =
            repository.getTransactionRecords(walletAddress, pageSize, pageNumber)

        if (response.data == null || response.data!!.list.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.list!!.mapIndexed { index, dto ->

            // 解析交易状态
            val transactionState = if (dto.confirmations >= 6)
                TransactionState.SUCCESS
            else
                TransactionState.PENDING

            // 解析交易类型，暂时只分收款和付款
            var transactionType = TransactionType.COLLECTION
            dto.inputs.forEach { inputInfo ->
                inputInfo.prev_addresses.forEach { inputAddress ->
                    if (inputAddress == walletAddress) {
                        transactionType = TransactionType.TRANSFER
                    }
                }
            }

            // 解析地址
            var fromAddress = ""
            var toAddress = ""
            if (transactionType == TransactionType.COLLECTION) {
                toAddress = walletAddress
                for (input in dto.inputs) {
                    for (prevAddress in input.prev_addresses) {
                        if (prevAddress.isNotBlank() && prevAddress != walletAddress) {
                            fromAddress = prevAddress
                            break
                        }
                    }
                }
            } else {
                fromAddress = walletAddress
                for (output in dto.outputs) {
                    for (address in output.addresses) {
                        if (address.isNotBlank() && address != walletAddress) {
                            toAddress = address
                            break
                        }
                    }
                }
            }

            // 解析展示金额，收款:自己接收的金额, 付款:自己交付的金额 - 自己接收的金额（系统找零）- 手续费
            var showAmount = 0L
            if (transactionType == TransactionType.COLLECTION) {
                dto.outputs.forEach { outputInfo ->
                    var me = false
                    outputInfo.addresses.forEach { outputAddress ->
                        if (outputAddress == walletAddress) {
                            me = true
                        }
                    }

                    if (me) {
                        showAmount = outputInfo.value
                    }
                }
            } else {
                var inputAmount = 0L
                dto.inputs.forEach { inputInfo ->
                    var me = false
                    inputInfo.prev_addresses.forEach { inputAddress ->
                        if (inputAddress == walletAddress) {
                            me = true
                        }
                    }

                    if (me) {
                        inputAmount += inputInfo.prev_value
                    }
                }

                var outputAmount = 0L
                dto.outputs.forEach { outputInfo ->
                    var me = false
                    outputInfo.addresses.forEach { outputAddress ->
                        if (outputAddress == walletAddress) {
                            me = true
                        }
                    }

                    if (me) {
                        outputAmount += outputInfo.value
                    }
                }

                showAmount = inputAmount - outputAmount - dto.fee
            }
            if (showAmount < 0) {
                showAmount = 0
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin,
                transactionType = transactionType,
                transactionState = transactionState,
                time = dto.block_time,
                fromAddress = fromAddress,
                toAddress = toAddress,
                amount = showAmount.toString(),
                gas = dto.fee.toString(),
                transactionId = dto.hash,
                url = BaseBrowserUrl.getBitcoinBrowserUrl(dto.hash),
                tokenName = null
            )
        }
        onSuccess.invoke(list, null)
    }
}