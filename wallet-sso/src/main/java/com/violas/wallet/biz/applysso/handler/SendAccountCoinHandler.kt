package com.violas.wallet.biz.applysso.handler

import com.palliums.content.ContextProvider
import com.violas.wallet.repository.DataRepository
import org.palliums.violascore.wallet.Account
import java.util.concurrent.CountDownLatch

class SendAccountCoinHandler(
    private val account: Account,
    private val receiveAddress: String,
    private val amount: Long
) {
    fun send(): Boolean {
        val countDownLatch = CountDownLatch(1)
        var isSuccess = false
        DataRepository.getViolasService().sendCoin(
            ContextProvider.getContext(),
            account,
            receiveAddress,
            amount
        ) {
            isSuccess = it
            countDownLatch.countDown()
        }
        countDownLatch.await()
        return isSuccess
    }
}