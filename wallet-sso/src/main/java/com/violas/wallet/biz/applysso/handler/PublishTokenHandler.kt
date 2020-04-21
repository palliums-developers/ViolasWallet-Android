package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import org.palliums.violascore.wallet.Account

class PublishTokenHandler(
    private val walletAddress: String,
    private val account: Account,
    private val ssoApplicationId: String
) : ApplyHandle() {

    override suspend fun handler() {
        val tokenManager = getServiceProvider()!!.getTokenManager()
        if (!tokenManager.isPublish(walletAddress)) {
            tokenManager.publishToken(account)
        }

        getServiceProvider()!!.getApplySsoRecordDao()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                SSOApplyTokenHandler.PublishSuccess
            )
    }
}