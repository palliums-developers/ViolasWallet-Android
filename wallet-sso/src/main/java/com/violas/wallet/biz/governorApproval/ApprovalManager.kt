package com.violas.wallet.biz.governorApproval

import org.palliums.violascore.wallet.Account

class ApprovalManager {

    /**
     * 转平台币给发行商
     *
     * @param account 解密后的 Account 账户
     * @param walletAddress 州长钱包地址
     * @param ssoApplicationId SSO发币申请id
     * @param ssoWalletAddress 铸币接收账户地址
     *
     * @exception 暂时未定义
     */
    suspend fun transferCoinToIssuer(
        account: Account? = null,
        walletAddress: String,
        ssoApplicationId: String,
        ssoWalletAddress: String
    ) {
        val handler = TransferCoinToIssuerHandler(
            account,
            walletAddress,
            ssoApplicationId,
            ssoWalletAddress
        )
        handler.exec()
    }

    /**
     * 铸稳定币给发行商
     *
     * @param account 解密后的 Account 账户
     * @param ssoWalletAddress 铸币接收账户地址
     * @param ssoApplicationId SSO发币申请id
     * @param ssoApplyAmount 铸币数量
     * @param newTokenIdx 新发型的稳定币索引
     *
     * * @exception 暂时未定义
     */
    suspend fun mintTokenToIssuer(
        account: Account,
        ssoApplicationId: String,
        ssoWalletAddress: String,
        ssoApplyAmount: Long,
        newTokenIdx: Long
    ) {
        val handler = MintTokenToIssuerHandler(
            account,
            ssoApplicationId,
            ssoWalletAddress,
            ssoApplyAmount,
            newTokenIdx
        )
        handler.exec()
    }
}