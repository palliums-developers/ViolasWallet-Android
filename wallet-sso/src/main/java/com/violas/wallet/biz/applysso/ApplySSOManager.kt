package com.violas.wallet.biz.applysso

import com.violas.wallet.repository.DataRepository
import org.palliums.violascore.wallet.Account

class ApplySSOManager {

    /**
     * @param account 解密后的 Account 账户
     */
    fun apply(accountId: Long, account: Account, walletAddress: String) {
        val applySSOHandler = SSOApplyTokenHandler(accountId, account, walletAddress)
        applySSOHandler.exec()
    }

    fun mint(account: Account, tokenAddress: String, receiveAddress: String, receiveAmount: Long) {

    }
}