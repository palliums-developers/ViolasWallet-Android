package com.violas.wallet.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.utils.CustomMainScope
import com.palliums.utils.coroutineExceptionHandler
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.biz.AssetsManager
import com.violas.wallet.biz.command.CommandActuator
import com.violas.wallet.biz.command.RefreshAssetsCommand
import com.violas.wallet.biz.command.SaveAssetsBalanceCommand
import com.violas.wallet.biz.command.SaveAssetsFiatRateCommand
import com.violas.wallet.common.getDiemCoinType
import com.violas.wallet.common.getViolasCoinType
import com.violas.wallet.viewModel.bean.CoinAssetVo
import com.violas.wallet.viewModel.bean.DiemCoinAssetVo
import com.violas.wallet.viewModel.bean.AssetVo
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

    val mAssetsLiveData = MutableLiveData<List<AssetVo>>()
    val mExistsAccountLiveData = MutableLiveData<Boolean>()
    val mDataRefreshingLiveData = MutableLiveData<Boolean>()

    private val mAssetsManager by lazy { AssetsManager() }

    init {
        refreshAssets(true)
    }

    fun isExistsAccount() = mExistsAccountLiveData.value == true

    fun refreshAccountStatus() = launch {
        val existsAccount = withContext(Dispatchers.IO) {
            try {
                AccountManager.getDefaultAccount()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (mExistsAccountLiveData.value != existsAccount)
            mExistsAccountLiveData.value = existsAccount
    }

    fun refreshAssets(isFirst: Boolean = false) = launch(Dispatchers.IO) {
        refreshAccountStatus()

        if (mDataRefreshingLiveData.value == true) {
            return@launch
        }

        mDataRefreshingLiveData.postValue(true)
        var localAssets = mAssetsManager.getLocalAssets()
        if (localAssets.isEmpty()) {
            mAssetsLiveData.postValue(arrayListOf())
        } else {
            if (isFirst) {
                mAssetsLiveData.postValue(localAssets)
            }

            localAssets = mAssetsManager.refreshAssets(localAssets)
            mAssetsLiveData.postValue(localAssets)
            CommandActuator.post(SaveAssetsBalanceCommand())

            localAssets = mAssetsManager.refreshFiatAssets(localAssets)
            mAssetsLiveData.postValue(localAssets)
            CommandActuator.post(SaveAssetsFiatRateCommand())
        }
        mDataRefreshingLiveData.postValue(false)

        checkWalletActivate(localAssets)
    }

    private suspend fun checkWalletActivate(localAssets: List<AssetVo>) {
        withContext(Dispatchers.IO + coroutineExceptionHandler()) {
            var activateResult = false

            localAssets.filter {
                it is CoinAssetVo && (it.getCoinNumber() == getViolasCoinType().coinNumber() || it.getCoinNumber() == getDiemCoinType().coinNumber())
            }.forEach {
                it as DiemCoinAssetVo

                try {
                    if (AccountManager.activateWallet(it)) {
                        activateResult = true
                    }
                } catch (e: Exception) {
                }
            }

            if (activateResult) {
                // 激活账户成功要刷新资产
                CommandActuator.postDelay(RefreshAssetsCommand(), 1000)
            }
        }
    }

    fun saveAssetsBalance(){
        mAssetsManager.saveBalance(mAssetsLiveData.value)
    }

    fun saveAssetsFiatRate(){
        mAssetsManager.saveFiatRate(mAssetsLiveData.value)
    }

}