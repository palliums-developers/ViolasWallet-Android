package com.violas.wallet.walletconnect.walletConnectMessageHandler

import android.util.Log
import com.google.gson.Gson
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.transferDataHandler.TransferDecodeEngine
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.violas.WCViolasSendTransaction
import org.palliums.violascore.serialization.LCSInputStream
import org.palliums.violascore.serialization.hexToBytes
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.TransactionArgument
import org.palliums.violascore.transaction.TransactionPayload
import org.palliums.violascore.transaction.lbrStructTagType
import org.palliums.violascore.transaction.storage.TypeTag

class ViolasSendTransactionMessageHandler(private val iWalletConnectMessage: IWalletConnectMessage) :
    MessageHandler(iWalletConnectMessage) {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mViolasService by lazy { DataRepository.getViolasService() }

    override suspend fun handler(
        requestID: Long,
        tx: Any
    ): TransactionSwapVo? {
        tx as WCViolasSendTransaction
        val account = mAccountStorage.findByCoinTypeAndCoinAddress(
            CoinTypes.Violas.coinType(),
            tx.from
        )

        if (account == null) {
            sendInvalidParameterErrorMessage(requestID, "Account does not exist.")
            return null
        }

        val gasUnitPrice = tx.gasUnitPrice ?: 0
        val maxGasAmount = tx.maxGasAmount ?: 1_000_000
        val expirationTime = tx.expirationTime ?: System.currentTimeMillis() + 1000
        val gasCurrencyCode = tx.gasCurrencyCode ?: lbrStructTagType()
        val sequenceNumber = tx.sequenceNumber ?: -1

        val payload = TransactionPayload.Script(
            try {
                tx.payload.code.hexToBytes()
            } catch (e: Exception) {
                sendInvalidParameterErrorMessage(
                    requestID,
                    "Payload code parameter error in transaction."
                )
                throw ProcessedRuntimeException()
            },
            try {
                tx.payload.tyArgs.map { TypeTag.decode(LCSInputStream(it.hexToBytes())) }
            } catch (e: Exception) {
                sendInvalidParameterErrorMessage(
                    requestID,
                    "Payload tyArgs parameter error in transaction."
                )
                throw ProcessedRuntimeException()
            },
            tx.payload.args.map {
                when (it.type.toLowerCase()) {
                    "address" -> {
                        try {
                            TransactionArgument.newAddress(it.value)
                        } catch (e: Exception) {
                            sendInvalidParameterErrorMessage(
                                requestID,
                                "Payload args Address parameter type error in transaction."
                            )
                            throw ProcessedRuntimeException()
                        }
                    }
                    "bool" -> {
                        try {
                            TransactionArgument.newBool(it.value.toBoolean())
                        } catch (e: Exception) {
                            sendInvalidParameterErrorMessage(
                                requestID, "Payload args Bool parameter type error in transaction."
                            )
                            throw ProcessedRuntimeException()
                        }
                    }
                    "u8" -> {
                        try {
                            TransactionArgument.newU8(it.value.toInt())
                        } catch (e: Exception) {
                            sendInvalidParameterErrorMessage(
                                requestID,
                                "Payload args U8 parameter type error in transaction. is positive integer."
                            )
                            throw ProcessedRuntimeException()
                        }
                    }
                    "u64" -> {
                        try {
                            TransactionArgument.newU64(it.value.toLong())
                        } catch (e: Exception) {
                            sendInvalidParameterErrorMessage(
                                requestID,
                                "Payload args U64 parameter type error in transaction. is positive integer."
                            )
                            throw ProcessedRuntimeException()
                        }
                    }
                    "u128" -> {
                        try {
                            TransactionArgument.newU128(it.value.toBigInteger())
                        } catch (e: Exception) {
                            sendInvalidParameterErrorMessage(
                                requestID,
                                "Payload args U128 parameter type error in transaction. is positive integer."
                            )
                            throw ProcessedRuntimeException()
                        }
                    }
                    "vector" -> {
                        try {
                            TransactionArgument.newByteArray(it.value.hexToBytes())
                        } catch (e: Exception) {
                            sendInvalidParameterErrorMessage(
                                requestID, "Payload args Bytes parameter type error in transaction."
                            )
                            throw ProcessedRuntimeException()
                        }
                    }
                    else -> {
                        sendInvalidParameterErrorMessage(
                            requestID, "Payload args Unknown type in transaction."
                        )
                        throw ProcessedRuntimeException()
                    }
                }
            }
        )

        Log.e("WalletConnect", Gson().toJson(payload))

        val generateRawTransaction = mViolasService.generateRawTransaction(
            TransactionPayload(payload),
            tx.from,
            sequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            expirationTime - System.currentTimeMillis()
        )

        val decode = try {
            TransferDecodeEngine(generateRawTransaction).decode()
        } catch (e: ProcessedRuntimeException) {
            iWalletConnectMessage.sendErrorMessage(
                requestID,
                JsonRpcError.invalidParams("Invalid Parameter:${e.message}")
            )
            throw ProcessedRuntimeException()
        }

        return TransactionSwapVo(
            requestID,
            generateRawTransaction.toByteArray().toHex(),
            false,
            account.id,
            decode.first.value,
            decode.second
        )
    }
}