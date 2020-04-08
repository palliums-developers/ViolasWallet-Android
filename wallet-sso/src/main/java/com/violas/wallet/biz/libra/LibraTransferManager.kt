package com.violas.wallet.biz.libra

import android.content.Context
import com.violas.wallet.repository.DataRepository
import org.palliums.libracore.wallet.Account

object LibraTransferManager {

    fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        call: (success: Boolean) -> Unit
    ) {
        DataRepository.getLibraService().sendCoinWithCallback(
            context, account, address, amount
        ) {
            call.invoke(it == null)
        }
    }

}