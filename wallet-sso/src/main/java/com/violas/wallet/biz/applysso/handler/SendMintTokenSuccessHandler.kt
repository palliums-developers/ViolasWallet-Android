package com.violas.wallet.biz.applysso.handler

class SendMintTokenSuccessHandler(
    private val accountAddress: String,
    private val ssoWalletAddress: String,
    private val ssoApplicationId: String
) : ApplyHandle() {

    override suspend fun handler() {
        getServiceProvider()!!.getGovernorService()
            .changeSSOApplicationToMinted(
                ssoApplicationId,
                ssoWalletAddress
            )

        getServiceProvider()!!.getApplySsoRecordDao()
            .remove(
                accountAddress,
                ssoApplicationId
            )
    }
}