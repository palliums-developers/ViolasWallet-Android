package com.violas.wallet.biz.command

import com.violas.wallet.viewModel.WalletAppViewModel

class SaveAssetsFiatRateCommand : ISingleCommand {

    override fun getIdentity() = "SaveAssetsFiatRate"

    override fun exec() {
        WalletAppViewModel.getInstance().saveAssetsFiatRate()
    }
}