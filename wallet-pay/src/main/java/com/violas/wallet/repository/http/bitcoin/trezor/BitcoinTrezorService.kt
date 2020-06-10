package com.violas.wallet.repository.http.bitcoin.trezor

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.http.TransactionRecordService
import com.violas.wallet.ui.transactionRecord.TransactionRecordVO
import com.violas.wallet.ui.transactionRecord.TransactionState
import com.violas.wallet.ui.transactionRecord.TransactionType

/**
 * Created by elephant on 2020/6/5 18:16.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Trezor service
 */
class BitcoinTrezorService(
    private val repository: BitcoinTrezorRepository
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

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, dto ->

            // 解析交易状态
            val transactionState = if (dto.confirmations >= 6)
                TransactionState.SUCCESS
            else
                TransactionState.PENDING

            // 解析交易类型，暂时只分收款和付款
            var transactionType = TransactionType.COLLECTION
            dto.vin.forEach { inputInfo ->
                inputInfo.addresses.forEach { inputAddress ->
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
                for (input in dto.vin) {
                    for (address in input.addresses) {
                        if (address.isNotBlank() && address != walletAddress) {
                            fromAddress = address
                            break
                        }
                    }
                }
            } else {
                fromAddress = walletAddress
                for (output in dto.vout) {
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
                dto.vout.forEach { outputInfo ->
                    var me = false
                    outputInfo.addresses.forEach { outputAddress ->
                        if (outputAddress == walletAddress) {
                            me = true
                        }
                    }

                    if (me) {
                        showAmount = outputInfo.value.toLong()
                    }
                }
            } else {
                var inputAmount = 0L
                dto.vin.forEach { inputInfo ->
                    var me = false
                    inputInfo.addresses.forEach { inputAddress ->
                        if (inputAddress == walletAddress) {
                            me = true
                        }
                    }

                    if (me) {
                        inputAmount += inputInfo.value.toLong()
                    }
                }

                var outputAmount = 0L
                dto.vout.forEach { outputInfo ->
                    var me = false
                    outputInfo.addresses.forEach { outputAddress ->
                        if (outputAddress == walletAddress) {
                            me = true
                        }
                    }

                    if (me) {
                        outputAmount += outputInfo.value.toLong()
                    }
                }

                showAmount = inputAmount - outputAmount - dto.fees.toLong()
            }
            if (showAmount < 0) {
                showAmount = 0
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin,
                transactionType = transactionType,
                transactionState = transactionState,
                time = dto.blockTime,
                fromAddress = fromAddress,
                toAddress = toAddress,
                amount = showAmount.toString(),
                gas = dto.fees,
                transactionId = dto.txid,
                url = BaseBrowserUrl.getBitcoinBrowserUrl(dto.txid),
                tokenName = null
            )
        }
        onSuccess.invoke(list, null)
    }
}