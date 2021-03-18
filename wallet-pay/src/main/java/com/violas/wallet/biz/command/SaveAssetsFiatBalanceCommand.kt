package com.violas.wallet.biz.command

import android.content.Context
import com.palliums.content.ContextProvider
import com.violas.wallet.repository.database.entity.AccountType
import com.violas.wallet.viewModel.WalletAppViewModel
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsTokenVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.viewModel.bean.HiddenTokenVo

class SaveAssetsFiatBalanceCommand() : ISingleCommand {
    companion object {
        fun sharedPreferencesFileName() = "fiat_balance"
        fun coinKey(assets: AssetsVo) =
            "${assets.getCoinNumber()}-${assets.getAssetsName()}-${assets.fiatAmountWithUnit.unit}"

        fun tokenKey(assets: AssetsVo): String {
            return when (assets) {
                is AssetsTokenVo -> {
                    "${assets.getCoinNumber()}-${assets.getAssetsName()}-${assets.fiatAmountWithUnit.unit}-${assets.module}-${assets.name}-${assets.address}"
                }
                is HiddenTokenVo -> {
                    "${assets.getCoinNumber()}-${assets.getAssetsName()}-${assets.fiatAmountWithUnit.unit}-${assets.module}-${assets.name}-${assets.address}"
                }
                else -> {
                    "${assets.getCoinNumber()}-${assets.getAssetsName()}-${assets.fiatAmountWithUnit.unit}"
                }
            }
        }
    }

    private val mSharedPreferences by lazy {
        ContextProvider.getContext()
            .getSharedPreferences(sharedPreferencesFileName(), Context.MODE_PRIVATE)
    }
    private val mWalletAppViewModel by lazy {
        WalletAppViewModel.getInstance()
    }

    override fun getIdentity() = "SaveAssetsFiatBalance"

    override fun exec() {
        val edit = mSharedPreferences.edit()
        mWalletAppViewModel.mAssetsListLiveData.value?.let {
            it.filter { assets ->
                assets is AssetsCoinVo && assets.accountType == AccountType.Normal
            }.forEach { assets ->
                edit.putString(
                    coinKey(assets),
                    assets.fiatAmountWithUnit.rate
                )
            }
            it.filterIsInstance<AssetsTokenVo>().forEach { assets ->
                edit.putString(
                    tokenKey(assets),
                    assets.fiatAmountWithUnit.rate
                )
            }
        }
        edit.apply()
    }
}