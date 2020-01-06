package com.violas.wallet.repository.http.violas

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.palliums.violas.http.ModuleDTO
import com.palliums.violas.http.SupportCurrencyDTO
import com.palliums.violas.http.SupportTokenCache
import com.palliums.violas.http.ViolasRepository
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.repository.http.interceptor.BaseUrlInterceptor
import com.violas.wallet.ui.record.TransactionRecordVO
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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

    private val mTokenStorage by lazy { DataRepository.getTokenStorage() }

    fun checkTokenRegister(address: String, call: (list: List<String>) -> Unit) {
        val subscribe = mViolasRepository.getRegisterToken(address)
            .subscribe({
                if (it.data != null) {
                    call.invoke(it.data!!)
                } else {
                    call.invoke(arrayListOf())
                }
            }, {
                call.invoke(arrayListOf())
            })
    }

    suspend fun getRegisterToken(address: String): List<String>? {
        var registerToken: List<String>? = null
        val subscribe = mViolasRepository.getRegisterToken(address)
            .subscribe({
                if (it.data != null) {
                    registerToken = it.data!!
                }
            }, {
            })
        return registerToken
    }

    fun getSupportCurrency(call: (list: List<SupportCurrencyDTO>) -> Unit) {
        val subscribe = mViolasRepository.getSupportCurrency()
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.data == null) {
                    call.invoke(arrayListOf())
                } else {
                    call.invoke(it.data!!)
                }
            }, {
                call.invoke(arrayListOf())
            })
    }

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

    fun getBalanceInMicroLibras(address: String, call: (amount: Long) -> Unit) {
        val subscribe = mViolasRepository.getBalance(address)
            .subscribeOn(Schedulers.io())
            .subscribe({
                mMainHandler.post {
                    if (it.data == null) {
                        call.invoke(0)
                    } else {
                        call.invoke(it.data!!.balance)
                    }
                }
            }, {
                mMainHandler.post {
                    call.invoke(0)
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
                account.keyPair.sign(rawTransaction.toByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

    fun sendTransaction(
        rawTransaction: RawTransaction,
        publicKey: ByteArray,
        signed: ByteArray,
        call: (success: Boolean) -> Unit
    ) {
        val signedTransaction = SignedTransaction(
            rawTransaction,
            publicKey,
            signed
        )

        val subscribe =
            mViolasRepository.pushTx(signedTransaction.toByteArray().toHex())
                .subscribeOn(Schedulers.io())
                .subscribe({
                    if (it.errorCode == it.getSuccessCode()) {
                        call.invoke(true)
                    } else {
                        call.invoke(false)
                    }
                }, {
                    call.invoke(false)
                })
    }

    fun sendTransaction(signedtxn: String, call: (success: Boolean) -> Unit) {
        val subscribe = mViolasRepository.pushTx(signedtxn)
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.errorCode == it.getSuccessCode()) {
                    call.invoke(true)
                } else {
                    call.invoke(false)
                }
            }, {
                call.invoke(false)
            })
    }

    fun sendViolasToken(
        context: Context,
        tokenAddress: String,
        account: Account,
        address: String,
        amount: Long,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, { sequenceNumber ->

            val publishTokenPayload = TransactionPayload.optionTokenTransactionPayload(
                context, tokenAddress, address, amount
            )

            val rawTransaction = RawTransaction.optionTransaction(
                senderAddress,
                publishTokenPayload,
                sequenceNumber
            )

            sendTransaction(
                rawTransaction,
                account.keyPair.getPublicKey(),
                account.keyPair.sign(rawTransaction.toByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

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
                account.keyPair.sign(rawTransaction.toByteArray()),
                call
            )
        }, {
            call.invoke(false)
        })
    }

    fun getSequenceNumber(
        address: String,
        call: (sequenceNumber: Long) -> Unit,
        error: (Exception) -> Unit
    ) {
        val subscribe = mViolasRepository.getSequenceNumber(address)
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.data == null) {
                    call.invoke(0)
                } else {
                    call.invoke(it.data!!)
                }
            }, {
                call.invoke(0)
            })
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
                1 -> TransactionRecordVO.TRANSACTION_TYPE_OPEN_TOKEN

                0 -> {
                    if (bean.sender == address) {
                        TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                    } else {
                        TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                    }
                }

                else -> {
                    if (bean.sender == address) {
                        TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER
                    } else {
                        TransactionRecordVO.TRANSACTION_TYPE_TOKEN_RECEIPT
                    }
                }
            }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = when (bean.sender) {
                address -> bean.receiver
                else -> bean.sender
            }

            // 解析币名称
            val coinName = if (TransactionRecordVO.isTokenOpt(transactionType)) {
                tokenName ?: getTokenName(bean.sender_module)
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
                url = BaseUrlInterceptor.getViolasBrowserUrl(bean.version.toString()),
                coinName = coinName
            )
        }
        onSuccess.invoke(list, null)
    }

    private suspend fun getTokenName(tokenAddress: String): String? {
        return try {
            SupportTokenCache.getSupportTokens(mViolasRepository)[tokenAddress]?.name
        } catch (e: Exception) {
            null
        } ?: try {
            mTokenStorage.findByTokenAddress(tokenAddress)?.name
        } catch (e: Exception) {
            null
        }
    }
}