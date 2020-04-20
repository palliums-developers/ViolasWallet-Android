package com.violas.wallet.repository.http.violas

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.palliums.net.checkResponse
import com.palliums.violas.error.TransactionException
import com.palliums.violas.http.ModuleDTO
import com.palliums.violas.http.ViolasRepository
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.common.BaseBrowserUrl
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun getBalance(
        address: String,
        tokenAddressList: List<String>,
        call: (accountBalance: Long, tokens: List<ModuleDTO>?, result: Boolean) -> Unit
    ): Disposable {
        return mViolasRepository.getBalance(address, tokenAddressList)
            .subscribeOn(Schedulers.io())
            .subscribe({
                mMainHandler.post {
                    if (it.data == null) {
                        call.invoke(0, arrayListOf(), true)
                    } else {
                        call.invoke(it.data!!.balance, it.data!!.modules, true)
                    }
                }
            }, {
                mMainHandler.post {
                    call.invoke(0, arrayListOf(), false)
                }
            })
    }

    fun checkTokenRegister(address: String, tokenAddress: String): Boolean {
        var isRegister: Boolean = false
        mViolasRepository.getRegisterToken(address)
            .subscribe({
                if (it.data != null && it.data!!.contains(tokenAddress)) {
                    isRegister = true
                    return@subscribe
                }
            }, {
            })
        return isRegister
    }

    fun getBalanceInMicroLibras(address: String, call: (amount: Long, success: Boolean) -> Unit) {
        val subscribe = mViolasRepository.getBalance(address)
            .subscribeOn(Schedulers.io())
            .subscribe({
                mMainHandler.post {
                    if (it.data == null) {
                        call.invoke(0, true)
                    } else {
                        call.invoke(it.data!!.balance, true)
                    }
                }
            }, {
                mMainHandler.post {
                    call.invoke(0, false)
                }
            })
    }

    fun publishToken(
        context: Context,
        account: Account,
        tokenAddress: String,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, { sequenceNumber ->

            val publishTokenPayload = TransactionPayload.optionPublishTokenPayload(
                context, tokenAddress
            )

            val rawTransaction = RawTransaction.optionTransaction(
                senderAddress,
                publishTokenPayload,
                sequenceNumber
            )

            sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

    /**
     * 链上注册稳定比交易
     *
     * @param tokenAddress 稳定币的 model address
     * @param account 发送交易的账户
     */
    fun tokenRegister(
        context: Context,
        tokenAddress: String,
        account: Account,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, { sequenceNumber ->

            val publishTokenPayload = TransactionPayload.optionTokenRegisterPayload(
                context, tokenAddress
            )

            val rawTransaction = RawTransaction.optionTransaction(
                senderAddress,
                publishTokenPayload,
                sequenceNumber
            )

            sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

    /**
     * 生成铸币交易 Payload
     *
     * @param tokenAddress 稳定币的 model address
     * @param account 发送交易的账户
     * @param receiveAddress 稳定币接收地址
     * @param receiveAmount 铸币数量(最小单位)
     */
    fun tokenMint(
        context: Context,
        tokenAddress: String,
        account: Account,
        receiveAddress: String,
        receiveAmount: Long,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, { sequenceNumber ->

            val publishTokenPayload = TransactionPayload.optionTokenMintPayload(
                context, tokenAddress, receiveAddress, receiveAmount
            )

            val rawTransaction = RawTransaction.optionTransaction(
                senderAddress,
                publishTokenPayload,
                sequenceNumber
            )

            sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

    @Deprecated("准备删除")
    private fun getSequenceNumber(
        address: String,
        call: (sequenceNumber: Long) -> Unit,
        error: (Throwable) -> Unit
    ) {
        /*val subscribe = mViolasRepository.getSequenceNumber(address)
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.data == null) {
                    call.invoke(0)
                } else {
                    call.invoke(it.data!!)
                }
            }, {
                error.invoke(it)
            })*/
    }

    @Deprecated("准备删除")
    private fun sendTransaction(
        rawTransaction: RawTransaction,
        publicKey: ByteArray,
        signed: ByteArray,
        call: (success: Boolean) -> Unit
    ) {
        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(
                publicKey,
                signed
            )
        )

        GlobalScope.launch {
            try {
                checkResponse { mViolasRepository.pushTx(signedTransaction.toByteArray().toHex()) }
                call.invoke(true)
            } catch (e: Exception) {
                call.invoke(false)
            }
        }
    }

    @Deprecated("准备删除")
    fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, { sequenceNumber ->

            val publishTokenPayload = TransactionPayload.optionTransactionPayload(
                context, address, amount
            )

            val rawTransaction = RawTransaction.optionTransaction(
                senderAddress,
                publishTokenPayload,
                sequenceNumber
            )

            sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.signMessage(rawTransaction.toHashByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

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

    suspend fun sendTransaction(signedTransaction: SignedTransaction) {
        try {
            val pushTx = mViolasRepository.pushTx(signedTransaction.toByteArray().toHex())
            TransactionException.checkViolasTransactionException(pushTx)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            throw TransactionException()
        }
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