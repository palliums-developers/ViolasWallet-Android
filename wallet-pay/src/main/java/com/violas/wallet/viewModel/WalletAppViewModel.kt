package com.violas.wallet.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.utils.CustomMainScope
import com.palliums.utils.coroutineExceptionHandler
import com.quincysx.crypto.CoinTypes
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.biz.command.SaveAssetsAllBalanceCommand
import com.violas.wallet.biz.command.SaveAssetsFiatBalanceCommand
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsLibraCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletAppViewModel : ViewModel(), CoroutineScope by CustomMainScope() {
    companion object {
        fun getViewModelInstance(context: Context = ContextProvider.getContext()): WalletAppViewModel {
            return ViewModelProvider(context.applicationContext as App).get(WalletAppViewModel::class.java)
        }
    }

    val mAccountManager by lazy {
        AccountManager()
    }

    val mAssetsListLiveData = MutableLiveData<List<AssetsVo>>()
    val mExistsAccountLiveData = MutableLiveData<Boolean>()
    val mDataRefreshingLiveData = MutableLiveData<Boolean>()

    init {
        Log.d("==assets==", "WalletAppViewModel init")
        refreshAssetsList(true)
    }

    fun isExistsAccount() = mExistsAccountLiveData.value == true

    fun refreshAssetsList(isFirst: Boolean = false) = launch(Dispatchers.IO) {
        val existsAccount = try {
            mAccountManager.getDefaultAccount()
            true
        } catch (e: Exception) {
            false
        }
        if (mExistsAccountLiveData.value != existsAccount)
            mExistsAccountLiveData.postValue(existsAccount)

        if (mDataRefreshingLiveData.value == true) {
            return@launch
        }

        mDataRefreshingLiveData.postValue(true)
        var localAssets = mAccountManager.getLocalAssets()
        if (localAssets.isEmpty()) {
            mAssetsListLiveData.postValue(arrayListOf())
        } else {
            if (isFirst) {
                mAssetsListLiveData.postValue(localAssets)
            }

            localAssets = mAccountManager.refreshAssetsAmount(localAssets)
            Log.d("==assets==", "WalletFragment AssetsList Refresh")
            mAssetsListLiveData.postValue(localAssets) // todo 尝试效果再决定是否删除

            CommandActuator.post(SaveAssetsAllBalanceCommand())

            if (isFirst) {
                localAssets = mAccountManager.refreshFiatAssetsAmount(localAssets)
                mAssetsListLiveData.postValue(localAssets)
                CommandActuator.post(SaveAssetsFiatBalanceCommand())
            }
        }
        mDataRefreshingLiveData.postValue(false)
        checkAccountActivate(localAssets)
    }

    private suspend fun checkAccountActivate(localAssets: List<AssetsVo>) {
        withContext(Dispatchers.IO + coroutineExceptionHandler()) {
            var activateResult = false

            localAssets.filter {
                it is AssetsCoinVo && (it.getCoinNumber() == CoinTypes.Violas.coinType() || it.getCoinNumber() == CoinTypes.Libra.coinType())
            }.forEach {
                it as AssetsLibraCoinVo
                try {
                    if (mAccountManager.activateAccount(it)) {
                        activateResult = true
                    }
                } catch (e: Exception) {
                }
            }

            if (activateResult) {
                // 激活账户成功要刷新资产
                CommandActuator.postDelay(RefreshAssetsAllListCommand(), 1000)
            }
        }
    }
}