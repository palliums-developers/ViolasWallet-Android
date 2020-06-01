package com.violas.wallet.viewModel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.palliums.content.App
import com.violas.wallet.viewModel.bean.AssetsCoinVo
import com.violas.wallet.viewModel.bean.AssetsVo

class WalletAppViewModel : ViewModel() {
    companion object {
        fun getViewModelInstance(context: Context): WalletAppViewModel {
            return ViewModelProvider(context.applicationContext as App).get(WalletAppViewModel::class.java)
        }
    }

    val mAssetsListLiveData = MutableLiveData<ArrayList<AssetsVo>>()

    init {
        refreshAssetsList()
    }

    fun refreshAssetsList() {
        mAssetsListLiveData.value = arrayListOf(
            AssetsCoinVo(0, "", "", 1, "address1", 10, ""),
            AssetsCoinVo(0, "", "", 1, "address2", 20, "")
        )
    }
}