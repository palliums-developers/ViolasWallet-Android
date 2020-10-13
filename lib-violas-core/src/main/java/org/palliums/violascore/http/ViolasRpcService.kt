package org.palliums.violascore.http

import android.content.Context
import android.util.Log
import org.palliums.violascore.BuildConfig
import org.palliums.violascore.crypto.*
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by elephant on 2020/3/31 12:51.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class ViolasRpcService(private val mViolasRpcRepository: ViolasRpcRepository) {
    data class TransactionResult(
        val sender: String,
        val sequenceNumber: Long
    )

    data class GenerateTransactionResult(
        val signTxn: String,
        val sender: String,
        val sequenceNumber: Long
    )

    /**
     * 发起一笔交易，建议用法
     * 适用于有私钥的情况下使用该方法
     *
     * @param payload 交易内容，通常情况下一般使用 @{link TransactionPayload.Script}
     * @param account 公钥，单签使用 {@link Ed25519KeyPair}，多签使用{@link MultiEd25519KeyPair}
     * @param sequenceNumber 账户 sequenceNumber
     * @param maxGasAmount 该交易可使用的最大 gas 数量
     * @param gasUnitPrice gas 的价格
     * @param delayed 交易超时时间：当前时间的延迟，单位秒。例如当前时间延迟 1000 秒。
     */
    @Throws(ViolasException::class)
    suspend fun generateTransaction(
        payload: TransactionPayload,
        account: Account,
        sequenceNumber: Long = -1,
        gasCurrencyCode: String = lbrStructTagType(),
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 600,
        chainId: Int
    ): GenerateTransactionResult {
        var sequenceNumber = sequenceNumber
        val keyPair = account.keyPair
        val senderAddress = account.getAddress()
        val accountState = getAccountState(senderAddress.toHex())
            ?: throw ViolasException.AccountNoActivation()

        if (accountState.authenticationKey != account.getAuthenticationKey().toHex()) {
            throw ViolasException.AccountNoControl()
        }

        if (sequenceNumber == -1L) {
            sequenceNumber = accountState.sequenceNumber
        }
        val rawTransaction = RawTransaction.optionTransaction(
            senderAddress.toHex(),
            payload,
            sequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            delayed,
            chainId = chainId
        )
        val signTransactionHex = generateTransaction(
            rawTransaction.toByteArray().toHex(),
            keyPair.getPublicKey(),
            keyPair.signMessage(rawTransaction.toHashByteArray())
        )
        return GenerateTransactionResult(
            signTransactionHex,
            senderAddress.toHex(),
            sequenceNumber
        )
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
    @Throws(ViolasException::class)
    suspend fun generateTransaction(
        rawTransactionHex: String,
        publicKey: KeyPair.PublicKey,
        signature: Signature
    ): String {
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

        return hexSignedTransaction
    }

    /**
     * 发起一笔交易，建议用法
     * 适用于有私钥的情况下使用该方法
     *
     * @param payload 交易内容，通常情况下一般使用 @{link TransactionPayload.Script}
     * @param account 公钥，单签使用 {@link Ed25519KeyPair}，多签使用{@link MultiEd25519KeyPair}
     * @param sequenceNumber 账户 sequenceNumber
     * @param maxGasAmount 该交易可使用的最大 gas 数量
     * @param gasUnitPrice gas 的价格
     * @param delayed 交易超时时间：当前时间的延迟，单位秒。例如当前时间延迟 1000 秒。
     */
    @Throws(ViolasException::class)
    suspend fun sendTransaction(
        payload: TransactionPayload,
        account: Account,
        sequenceNumber: Long = -1,
        gasCurrencyCode: String = lbrStructTagType(),
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 600,
        chainId: Int
    ): TransactionResult {
        val generateTransaction = generateTransaction(
            payload,
            account,
            sequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            delayed,
            chainId
        )
        mViolasRpcRepository.submitTransaction(generateTransaction.signTxn)
        return TransactionResult(generateTransaction.sender, generateTransaction.sequenceNumber)
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
    @Throws(ViolasException::class)
    suspend fun sendTransaction(
        rawTransactionHex: String,
        publicKey: KeyPair.PublicKey,
        signature: Signature
    ) {
        val hexSignedTransaction = generateTransaction(rawTransactionHex, publicKey, signature)
        mViolasRpcRepository.submitTransaction(hexSignedTransaction)
    }

    suspend fun sendViolasToken(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        typeTag: TypeTag = lbrStructTag(),
        gasCurrencyCode: String = lbrStructTagType(),
        chainId: Int
    ) {
        val transactionPayload =
            TransactionPayload.optionTransactionPayload(
                context, address, amount, typeTag = typeTag
            )
        sendTransaction(
            transactionPayload,
            account,
            gasCurrencyCode = gasCurrencyCode,
            chainId = chainId
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
        response?.balances?.forEach {
            if (it.currency.equals("lbr", true)
                || it.currency.equals("libra", true)
            ) {
                return it.amount
            }
        }
        return 0
    }

    suspend fun getCurrencies() = mViolasRpcRepository.getCurrencies()

    suspend fun getTransaction(
        address: String,
        sequenceNumber: Long,
        bool: Boolean = true
    ) = mViolasRpcRepository.getTransaction(address, sequenceNumber, bool)

    suspend fun submitTransaction(hex: String) = mViolasRpcRepository.submitTransaction(hex)

    suspend fun getAccountState(
        address: String
    ) = mViolasRpcRepository.getAccountState(address)

    suspend fun addCurrency(
        context: Context,
        account: Account,
        address: String,
        module: String,
        name: String,
        chainId: Int
    ): TransactionResult {
        val transactionPayload =
            TransactionPayload.optionAddCurrencyPayload(
                context,
                TypeTag.newStructTag(
                    StructTag(
//                        AccountAddress.DEFAULT, module, name,
                        AccountAddress(address.hexToBytes()), module, name,
                        arrayListOf()
                    )
                )
            )
        return sendTransaction(
            transactionPayload,
            account,
            gasCurrencyCode = lbrStructTagType(),
            chainId = chainId
        )
    }
}