package com.violas.wallet.biz.applysso.handler

class SendWalletLayersHandle(
    layerWallet: Long,
    accountId: Long,
    walletAddress: String
) : ApplyHandle {
    override fun handler(): Boolean {
        return false
    }
}