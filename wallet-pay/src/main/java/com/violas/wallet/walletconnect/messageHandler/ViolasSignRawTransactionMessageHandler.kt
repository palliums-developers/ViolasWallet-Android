package com.violas.wallet.walletconnect.messageHandler

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.violasTransferDataHandler.ViolasTransferDecodeEngine
import com.violas.wallet.walletconnect.TransactionSwapVo
import com.violas.walletconnect.exceptions.InvalidJsonRpcParamsException
import com.violas.walletconnect.extensions.hexStringToByteArray
import com.violas.walletconnect.jsonrpc.JsonRpcError
import com.violas.walletconnect.models.WCMethod
import com.violas.walletconnect.models.violas.WCViolasSignRawTransaction
import org.palliums.violascore.serialization.LCSInputStream
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.RawTransaction

class ViolasSignRawTransactionMessageHandler : IMessageHandler<JsonArray> {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }
    private val mBuilder = GsonBuilder()
    private val mGson = mBuilder
        .serializeNulls()
        .create()

    override fun canHandle(method: WCMethod): Boolean {
        return method == WCMethod.VIOLAS_SIGN_RAW_TRANSACTION
    }

    override fun decodeMessage(requestID: Long, param: JsonArray): TransactionSwapVo {
        val tx = mGson.fromJson<List<WCViolasSignRawTransaction>>(param).firstOrNull()
            ?: throw InvalidJsonRpcParamsException(requestID)

        val account = mAccountStorage.findByCoinTypeAndCoinAddress(
            CoinTypes.Violas.coinType(),
            tx.address
        )

        if (account == null) {
            throw InvalidParameterErrorMessage(requestID, "Account does not exist.")
        }

        val rawTransaction =
            RawTransaction.decode(LCSInputStream(tx.message.hexStringToByteArray()))

        val decode = try {
            ViolasTransferDecodeEngine(rawTransaction).decode()
        } catch (e: ProcessedRuntimeException) {
            throw WalletConnectErrorMessage(
                requestID,
                JsonRpcError.invalidParams("Invalid Parameter:${e.message}")
            )
        }

        return TransactionSwapVo(
            requestID,
            rawTransaction.toByteArray().toHex(),
            false,
            false,
            account.id,
            CoinTypes.Libra,
            decode.first.value,
            decode.second
        )
    }
}