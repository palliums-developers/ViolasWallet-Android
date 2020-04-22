package org.palliums.libracore.http

import android.content.Context
import android.util.Log
import org.palliums.libracore.BuildConfig
import org.palliums.libracore.crypto.KeyPair
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
        publicKey: KeyPair.PublicKey,
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
}