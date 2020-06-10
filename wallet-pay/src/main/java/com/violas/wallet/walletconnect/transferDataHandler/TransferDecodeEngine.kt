package com.violas.wallet.walletconnect.transferDataHandler

import com.google.gson.Gson
import com.violas.wallet.walletconnect.WalletConnect
import org.palliums.violascore.transaction.RawTransaction

class TransferDecodeEngine(private val mRawTransaction: RawTransaction) {
    private val mDecode: ArrayList<TransferDecode> =
        arrayListOf(TransferP2PDecode(mRawTransaction))

    fun decode(): Pair<WalletConnect.TransactionDataType, String> {
        mDecode.forEach {
            if (it.isHandle()) {
                return Pair(it.getTransactionDataType(), Gson().toJson(it.handle()))
            }
        }
        return Pair(
            WalletConnect.TransactionDataType.None,
            Gson().toJson(mRawTransaction.payload?.payload)
        )
    }
}