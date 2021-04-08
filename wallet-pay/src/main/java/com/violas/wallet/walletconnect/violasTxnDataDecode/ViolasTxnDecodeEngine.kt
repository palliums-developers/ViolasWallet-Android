package com.violas.wallet.walletconnect.violasTxnDataDecode

import com.google.gson.*
import com.violas.wallet.walletconnect.TransactionDataType
import org.palliums.violascore.serialization.toHex
import org.palliums.violascore.transaction.RawTransaction
import java.lang.reflect.Type


class ViolasTxnDecodeEngine(
    private val mRawTransaction: RawTransaction
) {
    private val mDecode: ArrayList<ViolasTxnDecoder> =
        arrayListOf(
            ViolasPeerToPeerWithMetadataDecoder(mRawTransaction),
            ViolasAddCurrencyToAccountDecoder(mRawTransaction),
            ViolasExchangeSwapDecoder(mRawTransaction),
            ViolasExchangeAddLiquidityDecoder(mRawTransaction),
            ViolasExchangeRemoveLiquidityDecoder(mRawTransaction),
            ViolasExchangeWithdrawRewardDecoder(mRawTransaction),
            ViolasBankDepositDecoder(mRawTransaction),
            ViolasBankRedeemDecoder(mRawTransaction),
            ViolasBankBorrowDecoder(mRawTransaction),
            ViolasBankRepayBorrowDecoder(mRawTransaction),
            ViolasBankWithdrawRewardDecoder(mRawTransaction)
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