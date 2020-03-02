package com.violas.wallet.ui.main.message

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.http.governor.GovernorInfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApplyMessageViewModel : BaseViewModel() {

    val mGovernorInfoLD = MutableLiveData<GovernorInfoDTO>()
    private val mAccountLD = MutableLiveData<AccountDO>()
    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)

            withContext(Dispatchers.Main) {
                execute(checkNetworkBeforeExecute = false)
            }
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        val walletAddress = mAccountLD.value!!.address
        val response = mGovernorService.getGovernorInfo(walletAddress)
        val governorInfo = response.data
            ?: GovernorInfoDTO(walletAddress, "", -1, 1)
        mGovernorInfoLD.postValue(governorInfo)
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (mAccountLD.value == null) {
            return false
        }

        val governorInfo = mGovernorInfoLD.value ?: return true
        return governorInfo.applicationStatus != 4
    }
}
