package com.violas.wallet.repository.http.bitcoin.trezor

import com.palliums.utils.correctDateLength
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.common.getBitcoinConfirmations
import com.violas.wallet.common.getBitcoinTxnDetailsUrl
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
            val transactionState = if (dto.confirmations >= getBitcoinConfirmations())
                TransactionState.SUCCESS
            else
                TransactionState.PENDING

            // 解析交易类型，暂时只分收款和付款
            var transactionType = TransactionType.COLLECTION

            var totalInputAmount = 0L
            val inputAddressesExcludeSelf = arrayListOf<String>()
            dto.vin.forEach { inputInfo ->
                totalInputAmount += inputInfo.value.toLong()
                if (inputInfo.isAddress) {
                    inputInfo.addresses.forEach { address ->
                        if (address == walletAddress) {
                            transactionType = TransactionType.TRANSFER
                        } else {
                            inputAddressesExcludeSelf.add(address)
                        }
                    }
                }
            }

            var outputAmountToSelf = 0L
            val outputAddressesExcludeSelf = arrayListOf<String>()
            dto.vout.forEach { outputInfo ->
                if (outputInfo.isAddress) {
                    var self = false
                    outputInfo.addresses.forEach { address ->
                        if (address == walletAddress) {
                            self = true
                        } else {
                            outputAddressesExcludeSelf.add(address)
                        }
                    }

                    if (self) {
                        outputAmountToSelf += outputInfo.value.toLong()
                    }
                }
            }

            // 解析地址
            val fromAddress =
                if (transactionType == TransactionType.COLLECTION
                    && inputAddressesExcludeSelf.isNotEmpty()
                ) {
                    inputAddressesExcludeSelf[0]
                } else {
                    walletAddress
                }
            val toAddress =
                if (transactionType == TransactionType.TRANSFER
                    && outputAddressesExcludeSelf.isNotEmpty()
                ) {
                    outputAddressesExcludeSelf[0]
                } else {
                    walletAddress
                }

            // 解析展示金额，收款:自己接收的金额, 付款:自己交付的总金额 - 自己接收的金额（系统找零）- 矿工费
            var showAmount = if (transactionType == TransactionType.COLLECTION) {
                outputAmountToSelf
            } else {
                totalInputAmount - outputAmountToSelf - dto.fees.toLong()
            }
            if (showAmount < 0) {
                showAmount = 0
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinType = getBitcoinCoinType(),
                transactionType = transactionType,
                transactionState = transactionState,
                time = correctDateLength(dto.blockTime),
                fromAddress = fromAddress,
                toAddress = toAddress,
                amount = showAmount.toString(),
                tokenId = tokenId,
                tokenDisplayName = tokenDisplayName,
                gas = dto.fees,
                gasTokenId = tokenId,
                gasTokenDisplayName = tokenDisplayName,
                transactionId = dto.txid,
                url = getBitcoinTxnDetailsUrl(dto.txid)
            )
        }
        onSuccess.invoke(list, null)
    }
}