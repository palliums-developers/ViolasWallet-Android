package com.violas.wallet.walletconnect.messageHandle

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.walletconnect.TransactionDataType
import com.violas.wallet.walletconnect.TransactionSwapVo
import com.violas.walletconnect.exceptions.InvalidJsonRpcParamsException
import com.violas.walletconnect.models.WCMethod
import com.violas.walletconnect.models.violas.WCViolasSendRawTransaction

class ViolasSendRawTransactionMessageHandler : IMessageHandler<JsonArray> {
    private val mViolasService by lazy { DataRepository.getViolasService() }

    private val mBuilder = GsonBuilder()
    private val mGson = mBuilder
        .serializeNulls()
        .create()

    override fun canHandle(method: WCMethod): Boolean {
        return method == WCMethod.VIOLAS_SEND_RAW_TRANSACTION
    }

    override fun decodeMessage(requestID: Long, param: JsonArray): TransactionSwapVo {
        val tx = mGson.fromJson<List<WCViolasSendRawTransaction>>(param).firstOrNull()
            ?: throw InvalidJsonRpcParamsException(requestID)
        return TransactionSwapVo(
            requestID,
            tx.tx,
            true,
            true,
            -1L,
            getViolasCoinType(),
            TransactionDataType.UNKNOWN.value,
            ""
        )
    }
}