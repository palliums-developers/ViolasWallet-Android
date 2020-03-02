package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import com.violas.wallet.repository.database.entity.ApplySSORecordDo
import kotlinx.coroutines.runBlocking

class SendWalletLayersHandle(
    private val layerWallet: Long,
    private val walletAddress: String
) : ApplyHandle() {
    override fun handler(): Boolean {
        runBlocking(com.palliums.utils.coroutineExceptionHandler()) {
            getServiceProvider()!!.getGovernorService()
                .updateSubAccountCount(walletAddress, layerWallet)
            getServiceProvider()!!.getApplySsoRecordDao()
                .insert(
                    ApplySSORecordDo(
                        childNumber = layerWallet,
                        walletAddress = walletAddress,
                        tokenAddress = "",
                        status = SSOApplyTokenHandler.Initial
                    )
                )
            return@runBlocking true
        }
        return false
    }


}