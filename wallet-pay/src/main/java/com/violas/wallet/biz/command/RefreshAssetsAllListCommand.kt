package com.violas.wallet.biz.command

import com.palliums.content.ContextProvider
import com.violas.wallet.viewModel.WalletAppViewModel

class RefreshAssetsAllListCommand(private val isFirst: Boolean = false) : ISingleCommand {
    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(ContextProvider.getContext())
    }

    override fun getIdentity() = "RefreshAssetsAllList"

    override fun exec() {
        mWalletAppViewModel.refreshAssetsList(isFirst)
    }
}