package com.violas.wallet.ui.collection

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.violas.wallet.viewModel.bean.AssetsVo

class MultiCollectionViewModel : ViewModel() {
    val mCurrAssets = MutableLiveData<AssetsVo>()
}