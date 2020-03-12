package com.violas.wallet.biz.applysso.handler

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.TokenManager
import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import com.violas.wallet.repository.DataRepository
import org.palliums.violascore.wallet.Account
import java.util.concurrent.CountDownLatch

class PublishTokenHandler(
    private val accountAddress: String,
    private val layerWallet: Long,
    private val tokenAddress: String,
    private val mintAccount: Account
) : ApplyHandle() {

    override fun handler(): Boolean {
        val countDownLatch = CountDownLatch(1)
        var isSuccess = false

        isSuccess =
            DataRepository.getViolasService().checkTokenRegister(accountAddress, tokenAddress)

        if (!isSuccess) {
            DataRepository.getViolasService().publishToken(
                ContextProvider.getContext(),
                mintAccount,
                tokenAddress
            ) {
                isSuccess = it
                countDownLatch.countDown()
            }
        }
        if (isSuccess) {
            getServiceProvider()!!.getApplySsoRecordDao()
                .updateRecordStatus(
                    accountAddress,
                    layerWallet,
                    SSOApplyTokenHandler.MintPublishSuccess
                )
        }
        countDownLatch.await()
        return isSuccess
    }
}