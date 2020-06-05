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
import kotlinx.coroutines.*

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
    val mDataRefreshingLiveData = MutableLiveData(false)

    init {
        refreshAssetsList(true)
    }

    fun refreshAssetsList(isFirst: Boolean = false) = launch(Dispatchers.IO) {
        mDataRefreshingLiveData.postValue(true)
        var localAssets = mAccountManager.getLocalAssets()
        if (localAssets.isEmpty()) {
            mExistsAccountLiveData.postValue(false)
        } else {
            mExistsAccountLiveData.postValue(true)
            if (isFirst) {
                mAssetsListLiveData.postValue(localAssets)
            }
            localAssets = mAccountManager.refreshAssetsAmount(localAssets)
            mAssetsListLiveData.postValue(localAssets)
        }
        mDataRefreshingLiveData.postValue(false)
    }
}