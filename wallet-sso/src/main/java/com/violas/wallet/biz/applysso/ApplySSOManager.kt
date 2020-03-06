package com.violas.wallet.biz.applysso

import org.palliums.violascore.wallet.Account

class ApplySSOManager {

    /**
     * 同意发币申请
     * @param account 解密后的 Account 账户
     * @param applySSOWalletAddress 发起申请铸币的账户地址
     * @param mnemonic 当前账户的助记词
     *
     * @exception 暂时未定义
     */
    suspend fun apply(
        account: Account,
        applySSOWalletAddress: String,
        mnemonic: List<String>
    ): Boolean {
        val applySSOHandler =
            SSOApplyTokenHandler( account, mnemonic, applySSOWalletAddress)
        return applySSOHandler.exec()
    }

    /**
     * 铸币
     * @param account 解密后的 Account 账户
     * @param tokenAddress 申请铸币的账户
     * @param receiveAddress 铸币接收账户地址
     * @param receiveAmount 铸币数量
     * @param mnemonic 当前账户的助记词
     *
     * * @exception 暂时未定义
     */
    suspend fun mint(
        account: Account,
        tokenAddress: String,
        receiveAddress: String,
        receiveAmount: Long,
        mnemonic: List<String>,
        walletLayersNumber: Long
    ): Boolean {
        val ssoMintTokenHandler = SSOMintTokenHandler(
            account,
            mnemonic,
            receiveAddress,
            receiveAmount,
            tokenAddress,
            walletLayersNumber
        )
        return ssoMintTokenHandler.exec()
    }
}