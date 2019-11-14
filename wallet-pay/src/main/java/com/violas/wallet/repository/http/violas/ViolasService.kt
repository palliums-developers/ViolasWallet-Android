package com.violas.wallet.repository.http.violas

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.smallraw.core.http.violas.Module
import com.smallraw.core.http.violas.SupportCurrencyResponse
import com.smallraw.core.http.violas.ViolasRepository
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.palliums.violascore.move.Move
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.utils.HexUtils
import org.palliums.violascore.wallet.Account
import java.util.*

class ViolasService(private val mViolasRepository: ViolasRepository) {
    private var mHandler = Handler(Looper.getMainLooper())

    fun getSupportCurrency(call: (list: List<SupportCurrencyResponse>) -> Unit) {
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
        call: (amount: Long, modes: List<Module>?) -> Unit
    ): Disposable {
        val joinToString = tokenAddress.joinToString(separator = ",")
        return mViolasRepository.getBalance(address, joinToString)
            .subscribeOn(Schedulers.io())
            .subscribe({
                mHandler.post {
                    if (it.data == null) {
                        call.invoke(0, arrayListOf())
                    } else {
                        call.invoke(it.data!!.balance, it.data!!.modules)
                    }
                }
            }, {
                mHandler.post {
                    call.invoke(0, arrayListOf())
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
            val moveEncode = Move.violasPublishTokenEncode(
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
                    if (it.code != 2000) {
                        call.invoke(false)
                    } else {
                        call.invoke(true)
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

            val moveEncode = Move.violasTransferTokenEncode(
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
}