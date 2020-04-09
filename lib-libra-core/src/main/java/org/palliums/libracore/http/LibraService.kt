package org.palliums.libracore.http

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.palliums.libracore.BuildConfig
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.*
import org.palliums.libracore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/3/31 12:51.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class LibraService(private val mLibraRepository: LibraRepository) {

    suspend fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long
    ) {
        val senderAddress = account.getAddress().toHex()
        val sequenceNumber = getSequenceNumber(senderAddress)

        val transactionPayload =
            TransactionPayload.optionTransactionPayload(
                context, address, amount
            )
        val rawTransaction =
            RawTransaction.optionTransaction(
                senderAddress, transactionPayload, sequenceNumber
            )

        submitTransaction(
            rawTransaction,
            account.keyPair.getPublicKey(),
            account.keyPair.signMessage(rawTransaction.toHashByteArray())
        )
    }

    suspend fun getBalance(address: String): String {
        val microLibras = getBalanceInMicroLibra(address)
        return BigDecimal(microLibras.toString())
            .divide(
                BigDecimal("1000000"),
                6,
                RoundingMode.HALF_UP
            )
            .stripTrailingZeros()
            .toPlainString()
    }

    suspend fun getBalanceInMicroLibra(address: String): Long {
        val response = getAccountState(address)
        return response.data?.balance ?: 0
    }

    suspend fun getSequenceNumber(address: String): Long {
        val response = getAccountState(address)
        return response.data?.sequenceNumber ?: 0
    }

    suspend fun submitTransaction(
        rawTransaction: RawTransaction,
        publicKey: ByteArray,
        signedRawTransaction: ByteArray
    ) {
        val signedTransaction = SignedTransaction(
            rawTransaction,
            TransactionSignAuthenticator(publicKey, signedRawTransaction)
        )

        val hexSignedTransaction = signedTransaction.toByteArray().toHex()
        if (BuildConfig.DEBUG) {
            Log.i(this.javaClass.name, "SignTransaction: $hexSignedTransaction")
        }

        mLibraRepository.submitTransaction(hexSignedTransaction)
    }

    suspend fun getAccountState(
        address: String
    ) =
        mLibraRepository.getAccountState(address)


    /************************ 以下方法是为了最小改动现有功能代码而增加，不建议再使用 ************************/
    fun sendCoinWithCallback(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        callback: (exception: Exception?) -> Unit
    ) {
        runBlocking {
            try {
                sendCoin(context, account, address, amount)

                callback.invoke(null)
            } catch (e: Exception) {
                callback.invoke(e)
            }
        }
    }

    fun getBalanceWithCallback(
        address: String,
        callback: (amount: String, exception: Exception?) -> Unit
    ) {
        runBlocking {
            try {
                val amount = getBalance(address)

                callback.invoke(amount, null)
            } catch (e: Exception) {
                callback.invoke("", e)
            }
        }
    }

    fun getBalanceInMicroLibraWithCallback(
        address: String,
        callback: (amount: Long, exception: Exception?) -> Unit
    ) {
        runBlocking {
            try {
                val amount = getBalanceInMicroLibra(address)

                callback.invoke(amount, null)
            } catch (e: Exception) {
                callback.invoke(0, e)
            }
        }
    }

    fun getSequenceNumberWithCallback(
        address: String,
        callback: (sequenceNumber: Long, exception: Exception?) -> Unit
    ) {
        runBlocking {
            try {
                val sequenceNumber = getSequenceNumber(address)

                callback.invoke(sequenceNumber, null)
            } catch (e: Exception) {
                callback.invoke(0, e)
            }
        }
    }

    fun submitTransactionWithCallback(
        rawTransaction: RawTransaction,
        publicKey: ByteArray,
        signedRawTransaction: ByteArray,
        callback: (exception: Exception?) -> Unit
    ) {
        runBlocking {
            try {
                submitTransaction(rawTransaction, publicKey, signedRawTransaction)

                callback.invoke(null)
            } catch (e: Exception) {
                callback.invoke(e)
            }
        }
    }

    fun getAccountStateWithCallback(
        address: String,
        callback: (accountState: AccountStateDTO?, exception: Exception?) -> Unit
    ) {
        runBlocking {
            try {
                val response = getAccountState(address)

                callback.invoke(response.data, null)
            } catch (e: Exception) {
                callback.invoke(null, e)
            }
        }
    }
}