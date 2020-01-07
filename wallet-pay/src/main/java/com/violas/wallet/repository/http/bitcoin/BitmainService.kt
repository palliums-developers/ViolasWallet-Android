package com.violas.wallet.repository.http.bitcoin

import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.common.Vm
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO

/**
 * Created by elephant on 2019-11-07 18:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 比特大陆 service
 */
class BitmainService(private val mBitmainRepository: BitmainRepository) :
    TransactionService {

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
            mBitmainRepository.getTransactionRecord(address, pageSize, pageNumber)

        if (response.data == null || response.data!!.list.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.list!!.mapIndexed { index, bean ->

            // 解析交易类型，暂时只分收款和付款
            var transactionType = TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
            bean.inputs.forEach { inputInfo ->
                inputInfo.prev_addresses.forEach { inputAddress ->
                    if (inputAddress == address) {
                        transactionType = TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                    }
                }
            }

            // 解析展示地址，收款付款均为对方第一个地址
            var showAddress = if (TransactionRecordVO.isReceipt(transactionType)) {
                if (bean.inputs.isNotEmpty()
                    && bean.inputs[0].prev_addresses.isNotEmpty()
                ) {
                    bean.inputs[0].prev_addresses[0]
                } else {
                    address
                }
            } else {
                if (bean.outputs.isNotEmpty()
                    && bean.outputs[0].addresses.isNotEmpty()
                ) {
                    bean.outputs[0].addresses[0]
                } else {
                    address
                }
            }

            // 解析展示金额，收款:自己接收的金额, 付款:自己交付的金额 - 自己接收的金额（系统找零）- 手续费
            var showAmount = 0L
            if (TransactionRecordVO.isReceipt(transactionType)) {
                bean.outputs.forEach { outputInfo ->
                    var me = false
                    outputInfo.addresses.forEach { outputAddress ->
                        if (outputAddress == address) {
                            me = true
                        }
                    }

                    if (me) {
                        showAmount = outputInfo.value
                    }
                }
            } else {
                var inputAmount = 0L
                bean.inputs.forEach { inputInfo ->
                    var me = false
                    inputInfo.prev_addresses.forEach { inputAddress ->
                        if (inputAddress == address) {
                            me = true
                        }
                    }

                    if (me) {
                        inputAmount += inputInfo.prev_value
                    }
                }

                var outputAmount = 0L
                bean.outputs.forEach { outputInfo ->
                    var me = false
                    outputInfo.addresses.forEach { outputAddress ->
                        if (outputAddress == address) {
                            me = true
                        }
                    }

                    if (me) {
                        outputAmount += outputInfo.value
                    }
                }

                showAmount = inputAmount - outputAmount - bean.fee
            }
            if (showAmount < 0) {
                showAmount = 0
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinTypes = if (Vm.TestNet) CoinTypes.BitcoinTest else CoinTypes.Bitcoin,
                transactionType = transactionType,
                time = bean.block_time * 1000L,
                amount = showAmount.toString(),
                gas = bean.fee.toString(),
                address = showAddress,
                url = BaseBrowserUrl.getBitcoinBrowserUrl(bean.hash)
            )
        }
        onSuccess.invoke(list, null)
    }
}