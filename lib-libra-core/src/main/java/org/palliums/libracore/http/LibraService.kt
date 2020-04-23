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
     * 发起一笔交易，建议用法
     * 适用于有私钥的情况下使用该方法
     *
     * @param payload 交易内容，通常情况下一般使用 @{link TransactionPayload.Script}
     * @param keyPair 公钥，单签使用 {@link Ed25519KeyPair}，多签使用{@link MultiEd25519KeyPair}
     * @param maxGasAmount 该交易可使用的最大 gas 数量
     * @param gasUnitPrice gas 的价格
     * @param delayed 交易超时时间：当前时间的延迟，单位秒。例如当前时间延迟 1000 秒。
     */
    @Throws(LibraException::class)
    suspend fun sendTransaction(
        payload: TransactionPayload,
        account: Account,
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 1000
    ): TransactionResult {
        val keyPair = account.keyPair
        val senderAddress = account.getAddress()
        val accountState = getAccountState(senderAddress.toHex())
            ?: throw LibraException.AccountNoActivation()

        val sequenceNumber = accountState.sequenceNumber
        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress.toHex(), payload, sequenceNumber, maxGasAmount, gasUnitPrice, delayed
        )
        sendTransaction(
            rawTransaction.toByteArray().toHex(),
            keyPair.getPublicKey(),
            keyPair.signMessage(rawTransaction.toHashByteArray())
        )
        return TransactionResult(senderAddress.toHex(), sequenceNumber)
    }

    /**
     * 高端用法，不建议基础业务使用
     * 适用于没有私钥的情况下使用该方法
     * publicKey 与 signature 需要配套使用
     *
     * @param rawTransactionHex 未签名交易序列化后的 16 进制字符串
     * @param publicKey 公钥，单签使用 {@link Ed25519PublicKey}，多签使用{@link MultiEd25519PublicKey}
     * @param signature 签名，单签使用 {@link Ed25519Signature}，多签使用{@link MultiEd25519Signature}
     */
    @Throws(LibraException::class)
    suspend fun sendTransaction(
        rawTransactionHex: String,
        publicKey: KeyPair.PublicKey,
        signature: Signature
    ) {
        val transactionAuthenticator = when {
            publicKey is Ed25519PublicKey && signature is Ed25519Signature -> {
                TransactionSignAuthenticator(
                    publicKey,
                    signature
                )
            }
            publicKey is MultiEd25519PublicKey && signature is MultiEd25519Signature -> {
                TransactionMultiSignAuthenticator(
                    publicKey,
                    signature
                )
            }
            else -> {
                throw IllegalArgumentException("Wrong type of publicKey and signature")
            }
        }

        val signedTransaction = SignedTransactionHex(rawTransactionHex, transactionAuthenticator)
        val hexSignedTransaction = signedTransaction.toHex()
        if (BuildConfig.DEBUG) {
            Log.i(this.javaClass.name, "SignTransaction: $hexSignedTransaction")
        }

        mLibraRepository.submitTransaction(hexSignedTransaction)
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

        sendTransaction(transactionPayload, account)
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
        return response?.balance?.amount ?: 0
    }

    suspend fun getAccountState(
        address: String
    ) =
        mLibraRepository.getAccountState(address).data
}