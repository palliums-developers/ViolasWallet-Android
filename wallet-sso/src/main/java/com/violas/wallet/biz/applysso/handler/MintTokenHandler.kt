package com.violas.wallet.biz.applysso.handler

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import com.violas.wallet.repository.DataRepository
import org.palliums.violascore.wallet.Account
import java.util.concurrent.CountDownLatch

class MintTokenHandler(
    private val accountId: Long,
    private val accountAddress: String,
    private val layerWallet: Long,
    private val tokenAddress: String,
    private val mintAccount: Account,
    private val receiveAddress: String,
    private val receiveAmount: Long
) : ApplyHandle() {

    override fun handler(): Boolean {
        val countDownLatch = CountDownLatch(1)
        var isSuccess = false
        DataRepository.getViolasService().tokenMint(
            ContextProvider.getContext(),
            tokenAddress,
            mintAccount,
            receiveAddress,
            receiveAmount
        ) {
            isSuccess = it
            countDownLatch.countDown()
        }
        if (isSuccess) {
            getServiceProvider()!!.getApplySsoRecordDao()
                .updateRecordStatus(
                    accountId,
                    accountAddress,
                    layerWallet,
                    SSOApplyTokenHandler.MintSuccess
                )
        }
        countDownLatch.await()
        return isSuccess
    }
}