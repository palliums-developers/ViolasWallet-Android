package com.violas.wallet.biz.applysso

import org.palliums.violascore.wallet.Account

class ApplySSOManager {

    /**
     * @param account 解密后的 Account 账户
     */
    suspend fun apply(
        accountId: Long,
        account: Account,
        applySSOWalletAddress: String,
        mnemonic: List<String>
    ) {
        val applySSOHandler =
            SSOApplyTokenHandler(accountId, account, mnemonic, applySSOWalletAddress)
        applySSOHandler.exec()
    }

    fun mint(account: Account, tokenAddress: String, receiveAddress: String, receiveAmount: Long) {

    }
}