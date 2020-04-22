package com.violas.wallet.repository.http.violas

import android.content.Context
import com.palliums.violas.error.TransactionException
import com.palliums.violas.http.ViolasRepository
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.wallet.Account

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas service
 */
class ViolasService(private val mViolasRepository: ViolasRepository) : TransactionService {

    suspend fun getBalanceInMicroLibras(address: String) =
        mViolasRepository.getBalance(address)

    suspend fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long
    ) {
        val senderAddress = account.getAddress().toHex()
        val sequenceNumber = getSequenceNumber(address)
        val transactionPayload =
            TransactionPayload.optionTransactionPayload(context, address, amount)

        val rawTransaction =
            RawTransaction.optionTransaction(senderAddress, transactionPayload, sequenceNumber)
        sendTransaction(rawTransaction, account)
    }

    suspend fun sendTransaction(
        payload: TransactionPayload,
        account: Account
    ) {
        val senderAddress = account.getAddress().toHex()
        val sequenceNumber = getSequenceNumber(senderAddress)

        val rawTransaction =
            RawTransaction.optionTransaction(senderAddress, payload, sequenceNumber)
        sendTransaction(rawTransaction, account)
    }

    @Throws(TransactionException::class)
    suspend fun sendTransaction(
        rawTransaction: RawTransaction,
        account: Account
    ) {
        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray())
            )
        )

        sendTransaction(signedTransaction)
    }

    //todo 添加返回值 返回 sequence_number、sender
    suspend fun sendTransaction(signedTransaction: SignedTransaction) {
        mViolasRepository.pushTx(signedTransaction.toByteArray().toHex())
    }

    suspend fun getSequenceNumber(address: String) =
        mViolasRepository.getSequenceNumber(address)

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