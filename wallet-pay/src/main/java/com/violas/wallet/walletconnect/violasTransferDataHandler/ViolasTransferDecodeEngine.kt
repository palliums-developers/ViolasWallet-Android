package com.violas.wallet.walletconnect.violasTransferDataHandler

import com.google.gson.*
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.RawTransaction
import java.lang.reflect.Type


class ViolasTransferDecodeEngine(
    private val mRawTransaction: RawTransaction
) {
    private val mDecode: ArrayList<TransferDecode> =
        arrayListOf(
            TransferP2PWithDataDecode(mRawTransaction),
            TransferViolasAddCurrencyToAccountDecode(mRawTransaction),
            TransferExchangeSwapDecode(mRawTransaction),
            TransferExchangeAddLiquidityDecode(mRawTransaction),
            TransferExchangeRemoveLiquidityDecode(mRawTransaction)
        )

    fun decode(): Pair<TransactionDataType, String> {
        mDecode.forEach {
            if (it.isHandle()) {
                return Pair(it.getTransactionDataType(), Gson().toJson(it.handle()))
            }
        }
        val gson = GsonBuilder().registerTypeHierarchyAdapter(
            ByteArray::class.java,
            ByteArrayToHexStringTypeAdapter()
        ).create()
        return Pair(
            TransactionDataType.None,
            gson.toJson(mRawTransaction.payload?.payload)
        )
    }

    private class ByteArrayToHexStringTypeAdapter : JsonSerializer<ByteArray?>,
        JsonDeserializer<ByteArray?> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): ByteArray {
            return json.asString.toByteArray()
        }

        override fun serialize(
            src: ByteArray?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return JsonPrimitive(src?.toHex())
        }
    }
}