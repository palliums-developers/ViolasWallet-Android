package com.violas.wallet.walletconnect.walletConnectMessageHandler

import android.util.Log
import com.google.gson.Gson
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.libraTransferDataHandler.LibraTransferDecodeEngine
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.violasprivate.WCLibraSendTransaction
import org.palliums.libracore.serialization.hexToBytes
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.AccountAddress
import org.palliums.libracore.transaction.TransactionArgument
import org.palliums.libracore.transaction.TransactionPayload
import org.palliums.libracore.transaction.lbrStructTagType
import org.palliums.libracore.transaction.storage.StructTag
import org.palliums.libracore.transaction.storage.TypeTag

class LibraSendTransactionMessageHandler(private val iWalletConnectMessage: IWalletConnectMessage) :
    MessageHandler(iWalletConnectMessage) {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mLibraService by lazy { DataRepository.getLibraService() }

    override suspend fun handler(
        requestID: Long,
        tx: Any
    ): TransactionSwapVo? {
        tx as WCLibraSendTransaction
        val account = mAccountStorage.findByCoinTypeAndCoinAddress(
            CoinTypes.Libra.coinType(),
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
        val chainId = tx.chainId

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
                tx.payload.tyArgs.map {
                    TypeTag.newStructTag(
                        StructTag(
                            AccountAddress(it.address.hexToBytes()),
                            it.module,
                            it.name,
                            arrayListOf()
                        )
                    )
                }
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

        val generateRawTransaction = mLibraService.generateRawTransaction(
            TransactionPayload(payload),
            tx.from,
            sequenceNumber,
            gasCurrencyCode,
            maxGasAmount,
            gasUnitPrice,
            expirationTime - System.currentTimeMillis(),
            chainId
        )

        val decode = try {
            LibraTransferDecodeEngine(generateRawTransaction).decode()
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
            CoinTypes.Libra,
            decode.first.value,
            decode.second
        )
    }
}