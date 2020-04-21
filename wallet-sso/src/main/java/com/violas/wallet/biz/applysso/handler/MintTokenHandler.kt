package com.violas.wallet.biz.applysso.handler

import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import org.palliums.violascore.wallet.Account

class MintTokenHandler(
    private val walletAddress: String,
    private val account: Account,
    private val ssoWalletAddress: String,
    private val ssoApplyAmount: Long,
    private val ssoApplicationId: String,
    private val tokenIdx: Long
) : ApplyHandle() {

    override suspend fun handler() {
        getServiceProvider()!!.getTokenManager()
            .mintToken(
                account,
                tokenIdx,
                ssoWalletAddress,
                ssoApplyAmount
            )

        getServiceProvider()!!.getApplySsoRecordDao()
            .updateRecordStatus(
                walletAddress,
                ssoApplicationId,
                SSOApplyTokenHandler.MintSuccess
            )
    }
}