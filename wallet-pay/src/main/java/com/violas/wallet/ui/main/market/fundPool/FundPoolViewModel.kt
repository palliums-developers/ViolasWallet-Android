package com.violas.wallet.ui.main.market.fundPool

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.palliums.base.BaseViewModel

/**
 * Created by elephant on 2020/6/30 17:42.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class FundPoolViewModel : BaseViewModel() {

    private val opModeLiveData = MutableLiveData<FundPoolOpMode>(FundPoolOpMode.TransferIn)

    fun getOpModeLiveData(): LiveData<FundPoolOpMode> {
        return opModeLiveData
    }

    fun getOpModelCurrPosition(): Int {
        return opModeLiveData.value!!.ordinal
    }

    fun switchOpModel(target: FundPoolOpMode) {
        if (target != opModeLiveData.value) {
            opModeLiveData.postValue(target)
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // TODO network request
    }

}