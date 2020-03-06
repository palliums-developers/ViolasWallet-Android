package com.violas.wallet.biz.applysso.handler

import com.palliums.utils.coroutineExceptionHandler
import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import com.violas.wallet.repository.database.entity.ApplySSORecordDo
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch

class SendWalletLayersHandle(
    private val layerWallet: Long,
    private val walletAddress: String
) : ApplyHandle() {
    override fun handler(): Boolean {
        return runBlocking {
            try {
                getServiceProvider()!!.getGovernorService()
                    .updateSubAccountCount(walletAddress, layerWallet)
                getServiceProvider()!!.getApplySsoRecordDao()
                    .insert(
                        ApplySSORecordDo(
                            childNumber = layerWallet,
                            walletAddress = walletAddress,
                            tokenAddress = "",
                            ssoWalletAddress = "",
                            status = SSOApplyTokenHandler.Initial
                        )
                    )
                return@runBlocking true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@runBlocking false
        }
    }
}