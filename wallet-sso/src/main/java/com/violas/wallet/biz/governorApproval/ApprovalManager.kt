package com.violas.wallet.biz.governorApproval

import org.palliums.violascore.wallet.Account

class ApprovalManager {

    /**
     * 同意发币申请
     *
     * @param account 解密后的 Account 账户
     * @param walletAddress 州长钱包地址
     * @param ssoApplicationId SSO发币申请id
     * @param ssoWalletAddress 铸币接收账户地址
     *
     * @exception 暂时未定义
     */
    suspend fun approve(
        account: Account? = null,
        walletAddress: String,
        ssoApplicationId: String,
        ssoWalletAddress: String
    ) {
        val applySSOHandler = ApproveSSOIssueTokenHandler(
            account,
            walletAddress,
            ssoApplicationId,
            ssoWalletAddress
        )
        applySSOHandler.exec()
    }

    /**
     * 铸币
     * @param account 解密后的 Account 账户
     * @param ssoWalletAddress 铸币接收账户地址
     * @param ssoApplicationId SSO发币申请id
     * @param ssoApplyAmount 铸币数量
     * @param newTokenIdx 新发型的稳定币索引
     *
     * * @exception 暂时未定义
     */
    suspend fun mint(
        account: Account,
        ssoApplicationId: String,
        ssoWalletAddress: String,
        ssoApplyAmount: Long,
        newTokenIdx: Long
    ) {
        val ssoMintTokenHandler = MintTokenToSSOHandler(
            account,
            ssoApplicationId,
            ssoWalletAddress,
            ssoApplyAmount,
            newTokenIdx
        )
        ssoMintTokenHandler.exec()
    }
}