package com.violas.wallet.biz.applysso.handler

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import com.violas.wallet.repository.DataRepository
import org.palliums.violascore.wallet.Account
import java.util.concurrent.CountDownLatch

class TokenAccountRegisterHandle(
    private val walletAddress: String,
    private val layerWallet: Long,
    private val account: Account,
    private val tokenAddress: String
) : ApplyHandle() {
    override fun handler(): Boolean {
        val countDownLatch = CountDownLatch(1)
        var isSuccess = false
        DataRepository.getViolasService().tokenRegister(
            ContextProvider.getContext(),
            tokenAddress,
            account
        ) {
            isSuccess = it
            countDownLatch.countDown()
        }
        countDownLatch.await()
        if (isSuccess) {
            getServiceProvider()?.getApplySsoRecordDao()?.updateRecordStatus(
                walletAddress,
                layerWallet,
                SSOApplyTokenHandler.Registered
            )
        }
        return isSuccess
    }
}