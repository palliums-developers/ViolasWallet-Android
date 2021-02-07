package com.violas.wallet.biz.command

import com.palliums.content.ContextProvider
import com.quincysx.crypto.CoinType
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.utils.convertDisplayUnitToAmount
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import java.math.BigDecimal

class SaveAssetsAllBalanceCommand() : ISingleCommand {
    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getViewModelInstance(ContextProvider.getContext())
    }

    override fun getIdentity() = "SaveAssetsAllBalance"

    override fun exec() {
        mWalletAppViewModel.mAssetsListLiveData.value?.let {
            it.filter { assets ->
                assets is AssetsCoinVo && assets.accountType == AccountType.Normal
            }.forEach { assets ->
                val convertDisplayUnitToAmount = convertDisplayUnitToAmount(
                    assets.amountWithUnit.amount,
                    CoinType.parseCoinNumber(assets.getCoinNumber())
                )
                DataRepository.getAccountStorage().saveCoinBalance(
                    assets.getId(),
                    convertDisplayUnitToAmount
                )
            }
            it.filterIsInstance<AssetsTokenVo>().forEach { assets ->
                DataRepository.getTokenStorage().saveCoinBalance(
                    assets.getId(),
                    BigDecimal(assets.amountWithUnit.amount).multiply(BigDecimal("1000000"))
                        .toLong()
                )
            }
        }
    }
}