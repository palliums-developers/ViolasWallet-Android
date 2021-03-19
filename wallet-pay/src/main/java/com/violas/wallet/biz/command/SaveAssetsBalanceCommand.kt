package com.violas.wallet.biz.command

import com.violas.wallet.viewModel.WalletAppViewModel

class SaveAssetsBalanceCommand() : ISingleCommand {

    override fun getIdentity() = "SaveAssetsBalance"

    override fun exec() {
        WalletAppViewModel.getInstance().saveAssetsBalance()
    }
}