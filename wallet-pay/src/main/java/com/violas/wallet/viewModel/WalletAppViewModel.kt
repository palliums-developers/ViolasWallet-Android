package com.violas.wallet.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.utils.CustomMainScope
import com.palliums.utils.coroutineExceptionHandler
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsAllListCommand
import com.violas.wallet.biz.command.SaveAssetsAllBalanceCommand
import com.violas.wallet.biz.command.SaveAssetsFiatBalanceCommand
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsLibraCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletAppViewModel : ViewModel(), CoroutineScope by CustomMainScope() {

    companion object {

        fun getInstance(): WalletAppViewModel {
            val context = ContextProvider.getContext().applicationContext
            return ViewModelProvider(context as App).get(WalletAppViewModel::class.java)
        }
    }

    val mAssetsListLiveData = MutableLiveData<List<AssetsVo>>()
    val mExistsAccountLiveData = MutableLiveData<Boolean>()
    val mDataRefreshingLiveData = MutableLiveData<Boolean>()

    init {
        //refreshAccountStatus()
        refreshAssetsList(true)
    }

    fun isExistsAccount() = mExistsAccountLiveData.value == true

    fun refreshAccountStatus() = launch {
        val existsAccount = try {
            AccountManager.getDefaultAccount()
            true
        } catch (e: Exception) {
            false
        }

        if (mExistsAccountLiveData.value != existsAccount)
            mExistsAccountLiveData.value = existsAccount
    }

    fun refreshAssetsList(isFirst: Boolean = false) = launch(Dispatchers.IO) {
        val existsAccount = try {
            AccountManager.getDefaultAccount()
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
        var localAssets = AccountManager.getLocalAssets()
        if (localAssets.isEmpty()) {
            mAssetsListLiveData.postValue(arrayListOf())
        } else {
            if (isFirst) {
                mAssetsListLiveData.postValue(localAssets)
            }

            localAssets = AccountManager.refreshAssetsAmount(localAssets)
            Log.d("==assets==", "WalletFragment AssetsList Refresh")
            mAssetsListLiveData.postValue(localAssets) // todo 尝试效果再决定是否删除

            CommandActuator.post(SaveAssetsAllBalanceCommand())

            //if (isFirst) {
            localAssets = AccountManager.refreshFiatAssetsAmount(localAssets)
            mAssetsListLiveData.postValue(localAssets)
            CommandActuator.post(SaveAssetsFiatBalanceCommand())
            //}
        }
        mDataRefreshingLiveData.postValue(false)
        checkAccountActivate(localAssets)
    }

    private suspend fun checkAccountActivate(localAssets: List<AssetsVo>) {
        withContext(Dispatchers.IO + coroutineExceptionHandler()) {
            var activateResult = false

            localAssets.filter {
                it is AssetsCoinVo && (it.getCoinNumber() == getViolasCoinType().coinNumber() || it.getCoinNumber() == getDiemCoinType().coinNumber())
            }.forEach {
                it as AssetsLibraCoinVo
                try {
                    if (AccountManager.activateWallet(it)) {
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