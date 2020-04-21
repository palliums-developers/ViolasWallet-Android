package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler

class SendApplySSOHandle(
    private val accountAddress: String,
    private val ssoWalletAddress: String,
    private val ssoApplicationId: String,
    private val newTokenIdx: Long
) : ApplyHandle() {

    override suspend fun handler() {
        getServiceProvider()!!.getGovernorService()
            .approvalSSOApplication(
                ssoApplicationId,
                ssoWalletAddress,
                newTokenIdx,
                true
            )

        getServiceProvider()!!.getApplySsoRecordDao()
            .updateRecordStatus(
                accountAddress,
                ssoApplicationId,
                SSOApplyTokenHandler.Approval
            )
    }
}