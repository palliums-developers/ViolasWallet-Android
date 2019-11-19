package com.violas.wallet.biz.libra

import android.content.Context
import io.grpc.ManagedChannelBuilder
import org.palliums.libracore.admissioncontrol.LibraAdmissionControl
import org.palliums.libracore.wallet.Account

object LibraTransferManager {
    private val mChannel by lazy {
        ManagedChannelBuilder.forAddress("ac.testnet.libra.org", 8000)
            .usePlaintext()
            .build()
    }

    private val mLibraAdmissionControl by lazy {
        LibraAdmissionControl(mChannel)
    }


    fun sendCoin(
        context: Context,
        account: Account,
        address: String,
        amount: Long,
        call: (success: Boolean) -> Unit
    ) {
        mLibraAdmissionControl.sendCoin(
            context, account, address, amount, call
        )
    }

}