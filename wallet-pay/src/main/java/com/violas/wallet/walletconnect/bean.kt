package com.violas.wallet.walletconnect

import android.os.Parcel
import android.os.Parcelable
import com.quincysx.crypto.CoinType

/**
 * 传递给转账确认页面的数据类型
 */
enum class TransactionDataType(val value: Int) {
    UNKNOWN(0),
    BITCOIN_TRANSFER(1),
    PEER_TO_PEER_WITH_METADATA(2),
    ADD_CURRENCY_TO_ACCOUNT(3),
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
                PEER_TO_PEER_WITH_METADATA.value -> {
                    PEER_TO_PEER_WITH_METADATA
                }
                ADD_CURRENCY_TO_ACCOUNT.value -> {
                    ADD_CURRENCY_TO_ACCOUNT
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
                VIOLAS_BANK_DEPOSIT.value -> {
                    VIOLAS_BANK_DEPOSIT
                }
                VIOLAS_BANK_REDEEM.value -> {
                    VIOLAS_BANK_REDEEM
                }
                VIOLAS_BANK_BORROW.value -> {
                    VIOLAS_BANK_BORROW
                }
                VIOLAS_BANK_REPAY_BORROW.value -> {
                    VIOLAS_BANK_REPAY_BORROW
                }
                VIOLAS_EXCHANGE_WITHDRAW_REWARD.value -> {
                    VIOLAS_EXCHANGE_WITHDRAW_REWARD
                }
                VIOLAS_BANK_WITHDRAW_REWARD.value -> {
                    VIOLAS_BANK_WITHDRAW_REWARD
                }
                else -> {
                    UNKNOWN
                }
            }
        }
    }
}

data class AddCurrencyToAccountData(
    val senderAddress: String,
    val currency: String
)

data class DiemTransferData(
    val payerAddress: String,
    val payeeAddress: String,
    val currency: String,
    val amount: Long,
    // base64
    val data: String
)

data class BitcoinTransferData(
    val payerAddress: String,
    val payeeAddress: String,
    val amount: Long,
    val changeForm: String,
    val data: String
)

data class ExchangeSwapData(
    val payerAddress: String,
    val payeeAddressOut: String,
    val currencyIn: String,
    val currencyOut: String,
    val amountIn: Long,
    val amountOutMin: Long,
    val path: List<Int>,
    // base64
    val data: String
)

data class ExchangeAddLiquidityData(
    val payerAddress: String,
    val currencyA: String,
    val currencyB: String,
    val amountADesired: Long,
    val amountBDesired: Long,
    val amountAMin: Long,
    val amountBMin: Long
)

data class ExchangeRemoveLiquidityData(
    val payerAddress: String,
    val currencyA: String,
    val currencyB: String,
    val liquidity: Long,
    val amountAMin: Long,
    val amountBMin: Long
)

data class BankDepositData(
    val senderAddress: String,
    val currency: String,
    val amount: Long
)

data class BankRedeemData(
    val senderAddress: String,
    val currency: String,
    val amount: Long
)

data class BankBorrowData(
    val senderAddress: String,
    val currency: String,
    val amount: Long
)

data class BankRepayBorrowData(
    val senderAddress: String,
    val currency: String,
    val amount: Long
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