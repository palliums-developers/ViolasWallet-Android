package com.violas.wallet.walletconnect.walletConnectMessageHandler

import android.os.Parcel
import android.os.Parcelable
import com.quincysx.crypto.CoinTypes

/**
 * 传递给转账确认页面的数据类型
 */
enum class TransactionDataType(val value: Int) {
    Transfer(0), None(1), PUBLISH(2),BITCOIN_TRANSFER(3);

    companion object {
        fun decode(value: Int): TransactionDataType {
            return when (value) {
                Transfer.value -> {
                    Transfer
                }
                None.value -> {
                    None
                }
                PUBLISH.value -> {
                    PUBLISH
                }
                BITCOIN_TRANSFER.value->{
                    BITCOIN_TRANSFER
                }
                else -> {
                    None
                }
            }
        }
    }
}

data class TransferBitcoinDataType(
    val form: String,
    val to: String,
    val amount: Long,
    val changeForm: String,
    // base64
    val data: String
)

data class TransferDataType(
    val form: String,
    val to: String,
    val amount: Long,
    val coinName: String,
    // base64
    val data: String
)

data class PublishDataType(
    val form: String,
    val coinName: String
)

/**
 * 传递给转账确认页面的数据类型
 */
data class TransactionSwapVo(
    val requestID: Long,
    val hexTx: String,
    val isSigned: Boolean = true,
    val accountId: Long = -1,
    val coinType: CoinTypes,
    val viewType: Int,
    val viewData: String
) : Parcelable{
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readLong(),
        CoinTypes.parseCoinType(parcel.readInt()),
        parcel.readInt(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(requestID)
        parcel.writeString(hexTx)
        parcel.writeByte(if (isSigned) 1 else 0)
        parcel.writeLong(accountId)
        parcel.writeInt(coinType.coinType())
        parcel.writeInt(viewType)
        parcel.writeString(viewData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionSwapVo> {
        override fun createFromParcel(parcel: Parcel): TransactionSwapVo {
            return TransactionSwapVo(
                parcel
            )
        }

        override fun newArray(size: Int): Array<TransactionSwapVo?> {
            return arrayOfNulls(size)
        }
    }
}