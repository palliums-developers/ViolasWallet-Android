package com.violas.wallet.repository.http.violas

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.palliums.violas.http.ModuleDTO
import com.palliums.violas.http.SupportCurrencyDTO
import com.palliums.violas.http.ViolasRepository
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.database.entity.TokenDo
import com.violas.wallet.repository.http.TransactionService
import com.violas.wallet.ui.record.TransactionRecordVO
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.utils.HexUtils
import org.palliums.violascore.wallet.Account
import java.util.*

/**
 * Created by elephant on 2019-11-11 15:47.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: Violas service
 */
class ViolasService(private val mViolasRepository: ViolasRepository) : TransactionService {
    private var mHandler = Handler(Looper.getMainLooper())

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
        tokenAddress: List<String>,
        call: (amount: Long, modes: List<ModuleDTO>?, result: Boolean) -> Unit
    ): Disposable {
        val joinToString = tokenAddress.joinToString(separator = ",")
        return mViolasRepository.getBalance(address, joinToString)
            .subscribeOn(Schedulers.io())
            .subscribe({
                mHandler.post {
                    if (it.data == null) {
                        call.invoke(0, arrayListOf(), true)
                    } else {
                        call.invoke(it.data!!.balance, it.data!!.modules, true)
                    }
                }
            }, {
                mHandler.post {
                    call.invoke(0, arrayListOf(), false)
                }
            })
    }

    fun getBalanceInMicroLibras(address: String, call: (amount: Long) -> Unit) {
        val subscribe = mViolasRepository.getBalance(address)
            .subscribeOn(Schedulers.io())
            .subscribe({
                mHandler.post {
                    if (it.data == null) {
                        call.invoke(0)
                    } else {
                        call.invoke(it.data!!.balance)
                    }
                }
            }, {
                mHandler.post {
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
            val moveEncode = Move.violasTokenEncode(
                context.assets.open("move/token_publish.json"),
                tokenAddress.hexToBytes()
            )

            val program = TransactionPayload(
                TransactionPayload.Script(
                    moveEncode,
                    arrayListOf()
                )
            )

            val rawTransaction = RawTransaction(
                AccountAddress(HexUtils.fromHex(senderAddress)),
                sequenceNumber,
                program,
                140000,
                0,
                (Date().time / 1000) + 1000
            )

            val toByteString = rawTransaction.toByteArray()
            println("rawTransaction ${HexUtils.toHex(toByteString)}")
            println("code ${HexUtils.toHex(moveEncode)}")

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

    fun sendViolasToken(
        context: Context,
        tokenAddress: String,
        account: Account,
        address: String,
        amount: Long,
        call: (success: Boolean) -> Unit
    ) {
        val senderAddress = account.getAddress().toHex()
        getSequenceNumber(senderAddress, {

            val moveEncode = Move.violasTokenEncode(
                context.assets.open("move/token_transfer.json"),
                tokenAddress.hexToBytes()
            )
            val rawTransaction =
                generateSendCoinRawTransaction(
                    address,
                    senderAddress,
                    amount,
                    it,
                    moveEncode
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
        getSequenceNumber(senderAddress, {
            val rawTransaction =
                generateSendCoinRawTransaction(
                    address,
                    senderAddress,
                    amount,
                    it,
                    Move.decode(context.assets.open("move/peer_to_peer_transfer.json"))
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

    private fun generateSendCoinRawTransaction(
        address: String,
        senderAddress: String,
        amount: Long,
        sequenceNumber: Long,
        moveCode: ByteArray
    ): RawTransaction {

        val addressArgument = TransactionArgument.newAddress(address)
        val amountArgument = TransactionArgument.newU64(amount)

        val program = TransactionPayload(
            TransactionPayload.Script(
                moveCode,
                arrayListOf(addressArgument, amountArgument)
            )
        )

        val rawTransaction = RawTransaction(
            AccountAddress(HexUtils.fromHex(senderAddress)),
            sequenceNumber,
            program,
            140000,
            0,
            (Date().time / 1000) + 1000
        )

        val toByteString = rawTransaction.toByteArray()
        println("rawTransaction ${HexUtils.toHex(toByteString)}")

        return rawTransaction
    }

    override suspend fun getTransactionRecord(
        address: String,
        tokenDO: TokenDo?,
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<TransactionRecordVO>, Any?) -> Unit
    ) {
        val queryToken = tokenDO?.tokenAddress?.isEmpty() ?: false
        val response =
            mViolasRepository.getTransactionRecord(address, pageSize, (pageNumber - 1) * pageSize)

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        val list = response.data!!.mapIndexed { index, bean ->
            // 解析交易类型
            val transactionType = when {
                bean.type == 1 ->
                    TransactionRecordVO.TRANSACTION_TYPE_OPEN_TOKEN

                bean.sender == address -> {
                    if (queryToken) {
                        TransactionRecordVO.TRANSACTION_TYPE_TOKEN_TRANSFER
                    } else {
                        TransactionRecordVO.TRANSACTION_TYPE_TRANSFER
                    }
                }

                else -> {
                    if (queryToken) {
                        TransactionRecordVO.TRANSACTION_TYPE_TOKEN_RECEIPT
                    } else {
                        TransactionRecordVO.TRANSACTION_TYPE_RECEIPT
                    }
                }
            }

            // 解析展示地址，收款付款均为对方地址
            val showAddress = when {
                bean.type == 1 || bean.sender == address ->
                    bean.receiver

                else ->
                    bean.sender
            }

            val coinName = if (TransactionRecordVO.isTokenOpt(transactionType)) {
                // TODO 解析 if (queryToken) tokenDO!!.name else bean.module_name
                if (queryToken) tokenDO!!.name else "Xcoin"
            } else {
                CoinTypes.VToken.coinName()
            }

            TransactionRecordVO(
                id = (pageNumber - 1) * pageSize + index,
                coinTypes = CoinTypes.VToken,
                transactionType = transactionType,
                time = bean.expiration_time * 1000,
                amount = bean.amount,
                address = showAddress,
                coinName = coinName
            )
        }
        onSuccess.invoke(list, null)
    }
}