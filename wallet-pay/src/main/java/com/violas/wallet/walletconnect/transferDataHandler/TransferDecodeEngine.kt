package com.violas.wallet.walletconnect.transferDataHandler

import com.google.gson.*
import com.violas.wallet.walletconnect.WalletConnect
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.RawTransaction
import java.lang.reflect.Type


class TransferDecodeEngine(private val mRawTransaction: RawTransaction) {
    private val mDecode: ArrayList<TransferDecode> =
        arrayListOf(
            TransferP2PDecode(mRawTransaction),
            TransferP2PWithDataDecode(mRawTransaction),
            TransferPublishDecode(mRawTransaction)
        )

    fun decode(): Pair<WalletConnect.TransactionDataType, String> {
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
            WalletConnect.TransactionDataType.None,
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