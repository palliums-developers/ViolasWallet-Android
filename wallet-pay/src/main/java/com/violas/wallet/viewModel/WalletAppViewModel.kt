package com.violas.wallet.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.content.App
import com.palliums.content.ContextProvider
import com.palliums.utils.CustomMainScope
import com.palliums.utils.exceptionAsync
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo
import com.violas.wallet.walletconnect.WalletConnect
import com.violas.wallet.walletconnect.WalletConnectStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WalletAppViewModel : ViewModel(), CoroutineScope by CustomMainScope() {
    companion object {
        fun getViewModelInstance(context: Context): WalletAppViewModel {
            return ViewModelProvider(context.applicationContext as App).get(WalletAppViewModel::class.java)
        }
    }

    val mAccountManager by lazy {
        AccountManager()
    }

    val mAssetsListLiveData = MutableLiveData<List<AssetsVo>>()
    val mExistsAccountLiveData = MutableLiveData(true)

    init {
        refreshAssetsList()
    }

    fun refreshAssetsList() = launch(Dispatchers.IO) {
        var localAssets = mAccountManager.getLocalAssets()
        if (localAssets.isEmpty()) {
            mExistsAccountLiveData.postValue(false)
        } else {
            mAssetsListLiveData.postValue(localAssets)
            localAssets = mAccountManager.refreshAssetsAmount(localAssets)
            mAssetsListLiveData.postValue(localAssets)
        }
    }
}