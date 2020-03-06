package com.violas.wallet.ui.main.message

import android.content.Context
import androidx.lifecycle.*
import com.palliums.net.RequestException
import com.palliums.net.RequestException.Companion.ERROR_CODE_UNKNOWN_ERROR
import com.palliums.net.postTipsMessage
import com.palliums.paging.PagingViewModel
import com.palliums.utils.getDistinct
import com.palliums.utils.getString
import com.palliums.utils.isNetworkConnected
import com.palliums.utils.toMap
import com.palliums.violas.http.ListResponse
import com.palliums.violas.http.Response
import com.violas.wallet.BuildConfig
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.database.entity.SSOApplicationMsgDO
import com.violas.wallet.repository.http.governor.GovernorInfoDTO
import com.violas.wallet.repository.http.governor.SSOApplicationMsgDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.palliums.violascore.wallet.Account

class ApplyMessageViewModel : PagingViewModel<SSOApplicationMsgVO>() {

    /**
     * 申请州长牌照状态
     */
    val mAccountLD = MutableLiveData<AccountDO>()
    val mChangedSSOApplicationMsgLD = MediatorLiveData<SSOApplicationMsgDO>()
    val mTipsMessageLD by lazy { EnhancedMutableLiveData<String>() }
    var mGovernorApplicationStatus = -2

    private var mLastObservedMsgApplicationId: String? = null
    private var mLastChangedSSOApplicationMsgLD: LiveData<SSOApplicationMsgDO?>? = null

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

    fun observeChangedSSOApplicationMsg(observedMsg: SSOApplicationMsgVO) {
        synchronized(this) {
            if (mLastObservedMsgApplicationId == observedMsg.applicationId) {
                return
            }

            mLastChangedSSOApplicationMsgLD?.let {
                mChangedSSOApplicationMsgLD.removeSource(it)
            }
            mLastObservedMsgApplicationId = null
            mLastChangedSSOApplicationMsgLD = null

            mAccountLD.value?.let { account ->
                val msgLD =
                    mSSOApplicationMsgStorage.loadLiveDataMsgFromApplicationId(
                        account.id, observedMsg.applicationId
                    ).getDistinct()
                mChangedSSOApplicationMsgLD.addSource(msgLD) { msg ->
                    msg?.let { mChangedSSOApplicationMsgLD.value = it }
                }
                mLastObservedMsgApplicationId = observedMsg.applicationId
                mLastChangedSSOApplicationMsgLD = msgLD
            }
        }
    }

    // TODO delete test code
    // test code =========> start
    private var nextMockGovernorApplicationStatus = -1
    // test code =========> end

    fun loadGovernorApplicationProgress(
        failureCallback: ((error: Throwable) -> Unit)? = null,
        successCallback: ((applicationStatus: Int) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val applicationStatus = withContext(Dispatchers.IO) {
                    val response =
                        // test code =========> start
                        try {
                            mGovernorService.getGovernorInfo(mAccountLD.value!!.address)
                        } catch (e: Exception) {
                            if (BuildConfig.MOCK_GOVERNOR_DATA) {
                                Response<GovernorInfoDTO>()
                            } else {
                                throw e
                            }
                        }
                    // test code =========> end

                    // test code =========> start
                    if (BuildConfig.MOCK_GOVERNOR_DATA) {
                        response.data = GovernorInfoDTO(
                            walletAddress = mAccountLD.value!!.address,
                            name = mAccountLD.value!!.walletNickname,
                            applicationStatus = nextMockGovernorApplicationStatus,
                            subAccountCount = 1
                        )
                        nextMockGovernorApplicationStatus++
                        if (nextMockGovernorApplicationStatus == 5) {
                            nextMockGovernorApplicationStatus = -1
                        }
                    }
                    // test code =========> end

                    return@withContext response.data?.applicationStatus ?: -1
                }

                mGovernorApplicationStatus = applicationStatus
                successCallback?.invoke(applicationStatus)
            } catch (e: Exception) {
                failureCallback?.invoke(e)
                postTipsMessage(mTipsMessageLD, e)
            }
        }
    }

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
            val fakeList = arrayListOf<SSOApplicationMsgDTO>()
            for (index in 0..5) {
                delay(200)
                val date = System.currentTimeMillis()
                fakeList.add(
                    SSOApplicationMsgDTO(
                        applicationId = "apply_id_$date",
                        applicationStatus = if (index > 4) 1 else index,
                        applicationDate = date,
                        expirationDate = if (index > 4) date - 1000 else date + 1000,
                        applicantIdName = "Name $date"
                    )
                )
            }
            response.data = fakeList
        }
        // test code =========> end

        if (response.data.isNullOrEmpty()) {
            onSuccess.invoke(emptyList(), null)
            return
        }

        // 获取SSO发币申请本地记录
        val applicationIds = response.data!!.map { it.applicationId }.toTypedArray()
        val localMsgs =
            mSSOApplicationMsgStorage.loadMsgsFromApplicationIds(
                mAccountLD.value!!.id,
                *applicationIds
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

    fun publishVStake(
        context: Context,
        account: Account,
        failureCallback: ((error: Throwable) -> Unit)? = null,
        successCallback: (() -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    val vStakeAddress = try {
                        mGovernorService.getVStakeAddress().data!!
                    } catch (e: Exception) {
                        if (BuildConfig.MOCK_GOVERNOR_DATA) {
                            account.getAddress().toHex()
                        } else {
                            throw e
                        }
                    }

                    var result = false
                    DataRepository.getViolasService()
                        .publishToken(context, account, vStakeAddress) {
                            result = it
                        }

                    if (!result) {
                        throw if (!isNetworkConnected())
                            RequestException.networkUnavailable()
                        else
                            RequestException(
                                ERROR_CODE_UNKNOWN_ERROR,
                                getString(R.string.tips_apply_for_licence_fail)
                            )
                    }
                }

                successCallback?.invoke()
            } catch (e: Exception) {
                failureCallback?.invoke(e)
                postTipsMessage(mTipsMessageLD, e)
            }
        }
    }
}
