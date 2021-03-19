package com.violas.wallet.biz.command

import com.violas.wallet.viewModel.WalletAppViewModel

class RefreshAssetsCommand(private val isFirst: Boolean = false) : ISingleCommand {

    override fun getIdentity() = "RefreshAssets"

    override fun exec() {
        WalletAppViewModel.getInstance().refreshAssets(isFirst)
    }
}