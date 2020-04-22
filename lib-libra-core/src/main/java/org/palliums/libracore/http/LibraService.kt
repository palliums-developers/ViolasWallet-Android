package org.palliums.libracore.http

import android.content.Context
import android.util.Log
import org.palliums.libracore.BuildConfig
import org.palliums.libracore.crypto.*
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
    data class TransactionResult(
        val sender: String,
        val sequenceNumber: Long
    )

    /**
     * 发起一笔转账，建议使用
     */
    suspend fun sendTransaction(
        payload: TransactionPayload,
        keyPair: KeyPair,
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 1000
    ): TransactionResult {
        val authenticationKey = when (val publicKey = keyPair.getPublicKey()) {
            is Ed25519PublicKey -> AuthenticationKey.ed25519(publicKey)
            is MultiEd25519PublicKey -> AuthenticationKey.multiEd25519(publicKey)
            else -> {
                TODO("更多类型")
            }
        }
        val senderAddress = authenticationKey.getShortAddress()
        val sequenceNumber = getSequenceNumber(senderAddress.toHex())
        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress.toHex(), payload, sequenceNumber, maxGasAmount, gasUnitPrice, delayed
        )

        val publicKey = keyPair.getPublicKey()
        val signature = keyPair.signMessage(rawTransaction.toHashByteArray())
        val transactionAuthenticator = when (keyPair) {
            is Ed25519KeyPair -> {
                TransactionSignAuthenticator(publicKey, signature)
            }
            is MultiEd25519KeyPair -> {
                TransactionSignAuthenticator(publicKey, signature)
            }
            else -> {
                TODO("更多类型")
            }
        }

        val signedTransaction = SignedTransaction(rawTransaction, transactionAuthenticator)
        val hexSignedTransaction = signedTransaction.toHex()
        if (BuildConfig.DEBUG) {
            Log.i(this.javaClass.name, "SignTransaction: $hexSignedTransaction")
        }

        mLibraRepository.submitTransaction(hexSignedTransaction)
        return TransactionResult(senderAddress.toHex(), sequenceNumber)
    }

    suspend fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long
    ) {
        val transactionPayload =
            TransactionPayload.optionTransactionPayload(
                context, address, amount
            )

        sendTransaction(transactionPayload, account.keyPair)
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
        publicKey: KeyPair.PublicKey,
        signedRawTransaction: Signature
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
}