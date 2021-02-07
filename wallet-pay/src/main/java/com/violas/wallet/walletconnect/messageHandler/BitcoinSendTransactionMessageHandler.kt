package com.violas.wallet.walletconnect.messageHandler

import android.util.Log
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.violas.wallet.common.getBitcoinCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.TransactionSwapVo
import com.violas.walletconnect.exceptions.InvalidJsonRpcParamsException
import com.violas.walletconnect.models.WCMethod
import com.violas.walletconnect.models.violasprivate.WCBitcoinSendTransaction

data class TransferBitcoinDataType(
    val form: String,
    val to: String,
    val amount: Long,
    val changeForm: String,
    val data: String
)

class BitcoinSendTransactionMessageHandler :
    IMessageHandler<JsonArray> {
    private val mAccountStorage by lazy { DataRepository.getAccountStorage() }

    private val mBuilder = GsonBuilder()
    private val mGson = mBuilder
        .serializeNulls()
        .create()

    override fun canHandle(method: WCMethod): Boolean {
        return method == WCMethod.BITCOIN_SEND_TRANSACTION
    }

    override fun decodeMessage(id: Long, param: JsonArray): TransactionSwapVo {
        val tx = mGson.fromJson<List<WCBitcoinSendTransaction>>(param).firstOrNull()
            ?: throw InvalidJsonRpcParamsException(id)
        val account = mAccountStorage.findByCoinTypeAndCoinAddress(
            getBitcoinCoinType().coinNumber(),
            tx.from
        ) ?: throw InvalidParameterErrorMessage(id, "Account does not exist.")

        val amount = tx.amount
        val from = tx.from
        val changeAddress = tx.changeAddress
        val payeeAddress = tx.payeeAddress
        val script = tx.script

        Log.e("WalletConnect", Gson().toJson(tx))

        val data = script

        val transferBitcoinDataType = TransferBitcoinDataType(
            from, payeeAddress, amount, changeAddress, data ?: ""
        )

        return TransactionSwapVo(
            id,
            "",
            true,
            false,
            account.id,
            getBitcoinCoinType(),
            TransactionDataType.BITCOIN_TRANSFER.value,
            mGson.toJson(transferBitcoinDataType)
        )
    }
}