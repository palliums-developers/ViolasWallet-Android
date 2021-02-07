package com.violas.wallet.walletconnect

import android.os.Parcel
import android.os.Parcelable
import com.quincysx.crypto.CoinType

/**
 * 传递给转账确认页面的数据类型
 */
enum class TransactionDataType(val value: Int) {
    Transfer(0),
    None(1),
    PUBLISH(2),
    BITCOIN_TRANSFER(3),
    VIOLAS_EXCHANGE_SWAP(4),
    VIOLAS_EXCHANGE_ADD_LIQUIDITY(5),
    VIOLAS_EXCHANGE_REMOVE_LIQUIDITY(6),
    VIOLAS_BANK_DEPOSIT(7),
    VIOLAS_BANK_REDEEM(8),
    VIOLAS_BANK_BORROW(9),
    VIOLAS_BANK_REPAY_BORROW(10),
    VIOLAS_EXCHANGE_WITHDRAW_REWARD(11),
    VIOLAS_BANK_WITHDRAW_REWARD(12);

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
                BITCOIN_TRANSFER.value -> {
                    BITCOIN_TRANSFER
                }
                VIOLAS_EXCHANGE_SWAP.value -> {
                    VIOLAS_EXCHANGE_SWAP
                }
                VIOLAS_EXCHANGE_ADD_LIQUIDITY.value -> {
                    VIOLAS_EXCHANGE_ADD_LIQUIDITY
                }
                VIOLAS_EXCHANGE_REMOVE_LIQUIDITY.value -> {
                    VIOLAS_EXCHANGE_REMOVE_LIQUIDITY
                }
                VIOLAS_BANK_DEPOSIT.value -> VIOLAS_BANK_DEPOSIT
                VIOLAS_BANK_REDEEM.value -> VIOLAS_BANK_REDEEM
                VIOLAS_BANK_BORROW.value -> VIOLAS_BANK_BORROW
                VIOLAS_BANK_REPAY_BORROW.value -> VIOLAS_BANK_REPAY_BORROW
                VIOLAS_EXCHANGE_WITHDRAW_REWARD.value -> VIOLAS_EXCHANGE_WITHDRAW_REWARD
                VIOLAS_BANK_WITHDRAW_REWARD.value -> VIOLAS_BANK_WITHDRAW_REWARD
                else -> {
                    None
                }
            }
        }
    }
}

data class BankDepositDatatype(
    val form: String,
    val amount: Long,
    val coinName: String
)

data class BankRedeemDatatype(
    val form: String,
    val amount: Long,
    val coinName: String
)

data class BankBorrowDatatype(
    val form: String,
    val amount: Long,
    val coinName: String
)

data class BankRepayBorrowDatatype(
    val form: String,
    val amount: Long,
    val coinName: String
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

data class ExchangeSwapDataType(
    val form: String,
    val payee: String,
    val inCoinName: String,
    val outCoinName: String,
    val amountIn: Long,
    val amountOutMin: Long,
    val path: List<Int>,
    // base64
    val data: String
)

data class ExchangeAddLiquidityDataType(
    val form: String,
    val inCoinName: String,
    val outCoinName: String,
    val amountIn: Long,
    val amountInMin: Long,
    val amountOut: Long,
    val amountOutMin: Long
)

data class ExchangeRemoveLiquidityDataType(
    val form: String,
    val inCoinName: String,
    val outCoinName: String,
    val liquidity: Long,
    val amountInMin: Long,
    val amountOutMin: Long
)

/**
 * 传递给转账确认页面的数据类型
 */
data class TransactionSwapVo(
    val requestID: Long,
    val hexTx: String,
    val isSend: Boolean = true,
    val isSigned: Boolean = true,
    val accountId: Long = -1,
    val coinType: CoinType,
    val viewType: Int,
    val viewData: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readLong(),
        CoinType.parseCoinNumber(parcel.readInt()),
        parcel.readInt(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(requestID)
        parcel.writeString(hexTx)
        parcel.writeByte(if (isSend) 1 else 0)
        parcel.writeByte(if (isSigned) 1 else 0)
        parcel.writeLong(accountId)
        parcel.writeInt(coinType.coinNumber())
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