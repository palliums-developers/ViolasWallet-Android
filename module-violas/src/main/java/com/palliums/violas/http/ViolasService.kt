package com.palliums.violas.http

import android.content.Context
import android.util.Log
import com.palliums.violas.error.ViolasException
import org.palliums.violascore.BuildConfig
import org.palliums.violascore.crypto.*
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode

class ViolasService(private val mViolasRepository: ViolasRepository) {
    data class GenerateTransactionResult(
        val signTxn: String,
        val sender: String,
        val sequenceNumber: Long
    )

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
    @Throws(ViolasException::class)
    suspend fun sendTransaction(
        payload: TransactionPayload,
        account: Account,
        sequenceNumber: Long = -1L,
        gasCurrencyCode: String = lbrStructTagType(),
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 1000
    ): TransactionResult {
        val (signedTxn, sender, newSequenceNumber) =
            generateTransaction(
                payload,
                account,
                sequenceNumber,
                gasCurrencyCode,
                maxGasAmount,
                gasUnitPrice,
                delayed
            )
        sendTransaction(signedTxn)
        return TransactionResult(sender, newSequenceNumber)
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
        val generateTransaction =
            generateTransaction(rawTransactionHex, publicKey, signature)
        sendTransaction(generateTransaction)
    }

    @Throws(ViolasException::class)
    suspend fun sendTransaction(signTxn: String) {
        mViolasRepository.pushTx(signTxn)
    }

    suspend fun generateTransaction(
        payload: TransactionPayload,
        account: Account,
        sequenceNumber: Long = -1L,
        gasCurrencyCode: String = lbrStructTagType(),
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 1000
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
            delayed
        )
        return GenerateTransactionResult(
            generateTransaction(
                rawTransaction.toByteArray().toHex(),
                keyPair.getPublicKey(),
                keyPair.signMessage(rawTransaction.toHashByteArray())
            ), senderAddress.toHex(), sequenceNumber
        )
    }

    suspend fun generateRawTransaction(
        payload: TransactionPayload,
        senderAddress: String,
        sequenceNumber: Long = -1L,
        gasCurrencyCode: String = lbrStructTagType(),
        maxGasAmount: Long = 1_000_000,
        gasUnitPrice: Long = 0,
        delayed: Long = 1000
    ): RawTransaction {
        var sequenceNumber = sequenceNumber

//        if (accountState.authenticationKey != account.getAuthenticationKey().toHex()) {
//            throw ViolasException.AccountNoControl()
//        }

        if (sequenceNumber == -1L) {
            val accountState = getAccountState(senderAddress)
                ?: throw ViolasException.AccountNoActivation()

            sequenceNumber = accountState.sequenceNumber
        }

        return RawTransaction.optionTransaction(
            senderAddress,
            payload,
            sequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            delayed
        )
    }

    @Throws(ViolasException::class)
    fun generateTransaction(
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

    suspend fun sendViolasToken(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        typeTag: TypeTag = lbrStructTag(),
        gasCurrencyCode: String = lbrStructTagType()
    ) {
        val transactionPayload =
            TransactionPayload.optionTransactionPayload(
                context, address, amount, typeTag = typeTag
            )
        sendTransaction(transactionPayload, account, gasCurrencyCode = gasCurrencyCode)
    }

    suspend fun getBalance(address: String): BigDecimal {
        val microLibras = getBalanceInMicroLibra(address)
        return BigDecimal(microLibras.toString())
            .divide(
                BigDecimal("1000000"),
                6,
                RoundingMode.HALF_UP
            ) ?: BigDecimal("0")
    }

    suspend fun getBalanceInMicroLibra(address: String): Long {
        val response = getAccountState(address)
//        return response?.balance?.amount ?: 0
        return response?.balance ?: 0
    }

    suspend fun getAccountState(
        address: String
    ) = mViolasRepository.getAccountState(address).data

    suspend fun activateAccount(
        address: String,
        authKeyPrefix: String
    ) = mViolasRepository.activateAccount(address, authKeyPrefix)

    suspend fun getCurrencies() = mViolasRepository.getCurrencies().data?.currencies

    suspend fun getBTCChainFiatBalance(address: String) =
        mViolasRepository.getBTCChainFiatBalance(address).data

    suspend fun getLibraChainFiatBalance(address: String) =
        mViolasRepository.getLibraChainFiatBalance(address).data

    suspend fun getViolasChainFiatBalance(address: String) =
        mViolasRepository.getViolasChainFiatBalance(address).data
}