package com.violas.wallet.biz.applysso

import org.palliums.violascore.wallet.Account

class ApplySSOManager {

    /**
     * 同意发币申请
     * @param account 解密后的 Account 账户
     * @param mnemonic 当前账户的助记词
     * @param applySSOWalletAddress 发起申请铸币的账户地址
     * @param ssoApplicationId SSO发币申请id
     *
     * @exception 暂时未定义
     */
    suspend fun apply(
        account: Account,
        mnemonic: List<String>,
        applySSOWalletAddress: String,
        ssoApplicationId: String
    ): Boolean {
        val applySSOHandler = SSOApplyTokenHandler(
            account,
            mnemonic,
            applySSOWalletAddress,
            ssoApplicationId
        )
        return applySSOHandler.exec()
    }

    /**
     * 铸币
     * @param account 解密后的 Account 账户
     * @param mnemonic 当前账户的助记词
     * @param tokenAddress 申请铸币的账户
     * @param receiveAddress 铸币接收账户地址
     * @param receiveAmount 铸币数量
     * @param walletLayersNumber 铸币账户使用的钱包层数
     * @param ssoApplicationId SSO发币申请id
     *
     * * @exception 暂时未定义
     */
    suspend fun mint(
        account: Account,
        mnemonic: List<String>,
        tokenAddress: String,
        receiveAddress: String,
        receiveAmount: Long,
        walletLayersNumber: Long,
        ssoApplicationId: String
    ): Boolean {
        val ssoMintTokenHandler = SSOMintTokenHandler(
            account,
            mnemonic,
            receiveAddress,
            receiveAmount,
            tokenAddress,
            walletLayersNumber,
            ssoApplicationId
        )
        return ssoMintTokenHandler.exec()
    }
}