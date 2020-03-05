package com.violas.wallet.ui.main.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.paging.PagingViewModel
import com.palliums.utils.toMap
import com.palliums.violas.http.ListResponse
import com.violas.wallet.BuildConfig
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.SSOApplicationMsgDO
import com.violas.wallet.repository.http.governor.SSOApplicationMsgDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ApplyMessageViewModel : PagingViewModel<SSOApplicationMsgVO>() {

    /**
     * 申请州长牌照状态
     */
    val mApplyGovernorLicenceStatusLD = MutableLiveData<Int>()
    val mAccountLD = MutableLiveData<AccountDO>()
    val mReadSSOApplicationMsgLD = MediatorLiveData<SSOApplicationMsgDO>()

    private var mLastReadSSOApplicationMsgLD: LiveData<SSOApplicationMsgDO?>? = null

    private val mGovernorService by lazy {
        DataRepository.getGovernorService()
    }
    private val mSSOApplicationMsgStorage by lazy {
        DataRepository.getSSOApplicationMsgStorage()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currentAccount = AccountManager().currentAccount()
            mAccountLD.postValue(currentAccount)
        }
    }

    fun observeReadSSOApplicationMsg(observedMsg: SSOApplicationMsgVO) {
        if (!observedMsg.msgUnread) {
            return
        }

        mLastReadSSOApplicationMsgLD?.let {
            mReadSSOApplicationMsgLD.removeSource(it)
        }

        mAccountLD.value?.let { account ->
            val readMsgLD =
                mSSOApplicationMsgStorage.loadReadMsgFromApplicationId(
                    account.id, observedMsg.applicationId
                )
            mLastReadSSOApplicationMsgLD = readMsgLD
            mReadSSOApplicationMsgLD.addSource(readMsgLD) { msg ->
                msg?.let { mReadSSOApplicationMsgLD.value = it }
            }
        }
    }

    // TODO delete test code
    // test code =========> start
    private var nextMockErrorCode = -1
    // test code =========> end

    override suspend fun loadData(
        pageSize: Int,
        pageNumber: Int,
        pageKey: Any?,
        onSuccess: (List<SSOApplicationMsgVO>, Any?) -> Unit
    ) {
        val response =
            // test code =========> start
            try {
                mGovernorService.getSSOApplicationMsgs(
                    mAccountLD.value!!.address,
                    pageSize,
                    (pageNumber - 1) * pageSize
                )
            } catch (e: Exception) {
                if (BuildConfig.MOCK_GOVERNOR_DATA) {
                    ListResponse<SSOApplicationMsgDTO>()
                } else {
                    throw e
                }
            }
        // test code =========> end

        // test code =========> start
        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            response.errorCode = nextMockErrorCode
            nextMockErrorCode++
            if (nextMockErrorCode == 7) {
                nextMockErrorCode = 2000
            }
            if (nextMockErrorCode == 2000) {
                nextMockErrorCode = -1
            }
        }
        // test code =========> end

        mApplyGovernorLicenceStatusLD.postValue(response.errorCode)
        if (response.errorCode < 4) {
            // 州长牌照还未批准，不能处理SSO发币申请消息
            onSuccess.invoke(emptyList(), null)
            return
        }

        // test code =========> start
        if (BuildConfig.MOCK_GOVERNOR_DATA) {
            if (response.errorCode == 4) {
                onSuccess.invoke(emptyList(), null)
                return
            }

            val fakeList = arrayListOf<SSOApplicationMsgDTO>()
            for (index in 0..4) {
                delay(200)
                val date = System.currentTimeMillis()
                fakeList.add(
                    SSOApplicationMsgDTO(
                        applicationId = "apply_id_$date",
                        applicationDate = date,
                        applicationStatus = index,
                        applicantIdName = "Name $date"
                    )
                )
            }
            response.data = fakeList
        }
        // test code =========> end

        // 州长牌照已批准，可以处理SSO发币申请消息
        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        // 获取SSO发币申请本地记录
        val applicationIds = response.data!!.map {
            it.applicationId
        }
        val localMsgs =
            mSSOApplicationMsgStorage.loadMsgsFromApplicationIds(
                mAccountLD.value!!.id,
                *applicationIds.toTypedArray()
            ).toMap { it.applicationId }

        // DTO 转换 VO
        val list = response.data!!.map {
            val msgUnread = when (it.applicationStatus) {
                0 -> { // not approved
                    val ssoRecord = localMsgs[it.applicationId]
                    ssoRecord == null || ssoRecord.issuingUnread
                }

                3 -> { // published
                    val ssoRecord = localMsgs[it.applicationId]
                    ssoRecord == null || ssoRecord.mintUnread
                }

                else -> {
                    false
                }
            }

            SSOApplicationMsgVO(
                applicationId = it.applicationId,
                applicationDate = it.applicationDate,
                applicationStatus = it.applicationStatus,
                applicantIdName = it.applicantIdName,
                msgUnread = msgUnread
            )
        }
        onSuccess.invoke(list, null)
    }
}
