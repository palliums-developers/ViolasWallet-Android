package com.violas.wallet.biz.applysso

import org.palliums.violascore.wallet.Account

class ApplySSOManager {

    /**
     * 同意发币申请
     *
     * @param account 解密后的 Account 账户
     * @param ssoWalletAddress 铸币接收账户地址
     * @param ssoApplicationId SSO发币申请id
     * @param newTokenIdx 新发型的稳定币索引
     *
     * @exception 暂时未定义
     */
    suspend fun apply(
        account: Account,
        ssoWalletAddress: String,
        ssoApplicationId: String,
        newTokenIdx: Long
    ) {
        val applySSOHandler = SSOApplyTokenHandler(
            account,
            ssoWalletAddress,
            ssoApplicationId,
            newTokenIdx
        )
        applySSOHandler.exec()
    }

    /**
     * 铸币
     * @param account 解密后的 Account 账户
     * @param ssoWalletAddress 铸币接收账户地址
     * @param ssoApplyAmount 铸币数量
     * @param ssoApplicationId SSO发币申请id
     * @param newTokenIdx 新发型的稳定币索引
     *
     * * @exception 暂时未定义
     */
    suspend fun mint(
        account: Account,
        ssoWalletAddress: String,
        ssoApplyAmount: Long,
        ssoApplicationId: String,
        newTokenIdx: Long
    ) {
        val ssoMintTokenHandler = SSOMintTokenHandler(
            account,
            ssoWalletAddress,
            ssoApplyAmount,
            ssoApplicationId,
            newTokenIdx
        )
        ssoMintTokenHandler.exec()
    }
}