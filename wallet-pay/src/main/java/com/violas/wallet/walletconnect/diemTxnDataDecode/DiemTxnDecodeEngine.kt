package com.violas.wallet.walletconnect.diemTxnDataDecode

import com.google.gson.*
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.libracore.serialization.toHex
import org.palliums.libracore.transaction.RawTransaction
import java.lang.reflect.Type


class DiemTxnDecodeEngine(
    private val mRawTransaction: RawTransaction
) {
    private val mDecoders: ArrayList<DiemTxnDecoder> =
        arrayListOf(
            DiemPeerToPeerWithMetadataDecoder(mRawTransaction),
            DiemAddCurrencyToAccountDecoder(mRawTransaction)
        )

    fun decode(): Pair<TransactionDataType, String> {
        mDecoders.forEach {
            if (it.isHandle()) {
                return Pair(it.getTransactionDataType(), Gson().toJson(it.handle()))
            }
        }
        val gson = GsonBuilder().registerTypeHierarchyAdapter(
            ByteArray::class.java,
            ByteArrayToHexStringTypeAdapter()
        ).create()
        return Pair(
            TransactionDataType.UNKNOWN,
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