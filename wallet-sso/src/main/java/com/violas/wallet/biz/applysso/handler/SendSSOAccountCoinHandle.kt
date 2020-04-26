package com.violas.wallet.biz.applysso.handler

import com.palliums.content.ContextProvider
import com.violas.wallet.biz.applysso.SSOApplyTokenHandler
import com.violas.wallet.repository.database.entity.ApplySSORecordDo
import org.palliums.violascore.wallet.Account

class SendSSOAccountCoinHandle(
    private val walletAddress: String,
    private val account: Account,
    private val ssoWalletAddress: String,
    private val ssoApplicationId: String,
    private val newTokenIdx: Long,
    private val amount: Long
) : ApplyHandle() {

    override suspend fun handler() {
        /*getServiceProvider()!!.getTokenManager().mViolasService
            .sendCoin(
                ContextProvider.getContext(),
                account,
                ssoWalletAddress,
                amount
            )*/

        getServiceProvider()!!.getApplySsoRecordDao()
            .insert(
                ApplySSORecordDo(
                    walletAddress = walletAddress,
                    applicationId = ssoApplicationId,
                    tokenIdx = newTokenIdx,
                    ssoWalletAddress = ssoWalletAddress,
                    status = SSOApplyTokenHandler.ReadyApproval
                )
            )
    }
}