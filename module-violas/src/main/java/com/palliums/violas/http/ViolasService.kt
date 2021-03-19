package com.palliums.violas.http

import android.content.Context
import android.util.Log
import androidx.annotation.IntRange
import com.palliums.violas.error.ViolasException
import org.palliums.violascore.BuildConfig
import org.palliums.violascore.common.*
import org.palliums.violascore.crypto.*
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.*
import org.palliums.violascore.transaction.storage.StructTag
import org.palliums.violascore.transaction.storage.TypeTag
import org.palliums.violascore.wallet.Account
import java.math.BigDecimal
import java.math.RoundingMode

class ViolasService(private val mViolasRepository: ViolasRepository) {
    data class GenerateTransactionResult(
        val signedTxn: String,
        val payerAddress: String,
        val sequenceNumber: Long
    )

    data class TransactionResult(
        val payerAddress: String,
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
        payerAccount: Account,
        sequenceNumber: Long = SEQUENCE_NUMBER_UNKNOWN,
        gasCurrencyCode: String = CURRENCY_DEFAULT_CODE,
        @IntRange(from = MAX_GAS_AMOUNT_MIN, to = MAX_GAS_AMOUNT_MAX)
        maxGasAmount: Long = MAX_GAS_AMOUNT_DEFAULT,
        @IntRange(from = GAS_UNIT_PRICE_MIN, to = GAS_UNIT_PRICE_MAX)
        gasUnitPrice: Long = GAS_UNIT_PRICE_DEFAULT,
        @IntRange(from = 0)
        delayed: Long = EXPIRATION_DELAYED_DEFAULT,
        chainId: Int
    ): TransactionResult {
        val (signedTxn, payerAddress, newSequenceNumber) =
            generateTransaction(
                payload,
                payerAccount,
                sequenceNumber,
                gasCurrencyCode,
                maxGasAmount,
                gasUnitPrice,
                delayed,
                chainId
            )
        sendTransaction(signedTxn)
        return TransactionResult(payerAddress, newSequenceNumber)
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
        val hexSignedTransaction =
            generateTransaction(rawTransactionHex, publicKey, signature)
        sendTransaction(hexSignedTransaction)
    }

    @Throws(ViolasException::class)
    suspend fun sendTransaction(signTxn: String) {
        mViolasRepository.pushTx(signTxn)
    }

    suspend fun generateTransaction(
        payload: TransactionPayload,
        payerAccount: Account,
        sequenceNumber: Long = SEQUENCE_NUMBER_UNKNOWN,
        gasCurrencyCode: String = CURRENCY_DEFAULT_CODE,
        @IntRange(from = MAX_GAS_AMOUNT_MIN, to = MAX_GAS_AMOUNT_MAX)
        maxGasAmount: Long = MAX_GAS_AMOUNT_DEFAULT,
        @IntRange(from = GAS_UNIT_PRICE_MIN, to = GAS_UNIT_PRICE_MAX)
        gasUnitPrice: Long = GAS_UNIT_PRICE_DEFAULT,
        @IntRange(from = 0)
        delayed: Long = EXPIRATION_DELAYED_DEFAULT,
        chainId: Int
    ): GenerateTransactionResult {
        val payerAddress = payerAccount.getAddress().toHex()

        var actualSequenceNumber = sequenceNumber
        if (actualSequenceNumber == SEQUENCE_NUMBER_UNKNOWN) {
            val accountState = getAccountState(payerAddress)
                ?: throw org.palliums.violascore.http.ViolasException.AccountNoActivation()
            if (accountState.authenticationKey != payerAccount.getAuthenticationKey().toHex()) {
                throw org.palliums.violascore.http.ViolasException.AccountNoControl()
            }

            actualSequenceNumber = accountState.sequenceNumber
        }

        val rawTransaction = RawTransaction.optionTransaction(
            payerAddress,
            payload,
            actualSequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            delayed,
            chainId
        )
        return GenerateTransactionResult(
            generateTransaction(
                rawTransaction.toByteArray().toHex(),
                payerAccount.keyPair.getPublicKey(),
                payerAccount.keyPair.signMessage(rawTransaction.toHashByteArray())
            ),
            payerAddress,
            actualSequenceNumber
        )
    }

    suspend fun generateRawTransaction(
        payload: TransactionPayload,
        payerAddress: String,
        sequenceNumber: Long = EXPIRATION_DELAYED_DEFAULT,
        gasCurrencyCode: String = CURRENCY_DEFAULT_CODE,
        @IntRange(from = MAX_GAS_AMOUNT_MIN, to = MAX_GAS_AMOUNT_MAX)
        maxGasAmount: Long = MAX_GAS_AMOUNT_DEFAULT,
        @IntRange(from = GAS_UNIT_PRICE_MIN, to = GAS_UNIT_PRICE_MAX)
        gasUnitPrice: Long = GAS_UNIT_PRICE_DEFAULT,
        @IntRange(from = 0)
        delayed: Long = EXPIRATION_DELAYED_DEFAULT,
        chainId: Int
    ): RawTransaction {
        var actualSequenceNumber = sequenceNumber
        if (actualSequenceNumber == -1L) {
            val accountState = getAccountState(payerAddress)
                ?: throw ViolasException.AccountNoActivation()
//            if (accountState.authenticationKey != account.getAuthenticationKey().toHex()) {
//                throw ViolasException.AccountNoControl()
//            }

            actualSequenceNumber = accountState.sequenceNumber
        }

        return RawTransaction.optionTransaction(
            payerAddress,
            payload,
            actualSequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            delayed,
            chainId
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

    suspend fun sendCurrency(
        context: Context,
        payerAccount: Account,
        payeeAddress: String,
        transferAmount: Long,
        currencyAddress: String,
        currencyModule: String,
        currencyName: String,
        sequenceNumber: Long = SEQUENCE_NUMBER_UNKNOWN,
        gasCurrencyCode: String = CURRENCY_DEFAULT_CODE,
        @IntRange(from = MAX_GAS_AMOUNT_MIN, to = MAX_GAS_AMOUNT_MAX)
        maxGasAmount: Long = MAX_GAS_AMOUNT_DEFAULT,
        @IntRange(from = GAS_UNIT_PRICE_MIN, to = GAS_UNIT_PRICE_MAX)
        gasUnitPrice: Long = GAS_UNIT_PRICE_DEFAULT,
        @IntRange(from = 0)
        delayed: Long = EXPIRATION_DELAYED_DEFAULT,
        chainId: Int
    ): TransactionResult {
        val transactionPayload = TransactionPayload.optionTransactionPayload(
            context,
            payeeAddress,
            transferAmount,
            typeTag = TypeTag.newStructTag(
                StructTag(
                    AccountAddress(currencyAddress.hexToBytes()), currencyModule, currencyName,
                    arrayListOf()
                )
            )
        )

        return sendTransaction(
            transactionPayload,
            payerAccount,
            sequenceNumber = sequenceNumber,
            gasCurrencyCode = gasCurrencyCode,
            maxGasAmount = maxGasAmount,
            gasUnitPrice = gasUnitPrice,
            delayed = delayed,
            chainId = chainId
        )
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

    suspend fun activateWallet(
        address: String,
        authKeyPrefix: String
    ) = mViolasRepository.activateWallet(address, authKeyPrefix)

    suspend fun getCurrencies() = mViolasRepository.getCurrencies().data?.currencies

    suspend fun getViolasChainFiatRates(address: String) =
        mViolasRepository.getViolasChainFiatRates(address).data

    suspend fun getDiemChainFiatRates(address: String) =
        mViolasRepository.getDiemChainFiatRates(address).data

    suspend fun getBitcoinChainFiatRates(address: String) =
        mViolasRepository.getBitcoinChainFiatRates(address).data

}