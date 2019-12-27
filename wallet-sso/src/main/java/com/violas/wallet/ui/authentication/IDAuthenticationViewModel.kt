package com.violas.wallet.ui.authentication

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.net.RequestException
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.AuthenticationIDEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.local.user.IDAuthenticationStatus
import com.violas.wallet.repository.local.user.IDInfo
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import com.violas.wallet.utils.getFilePathFromContentUri
import com.violas.wallet.utils.validationIDCar18
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.IOException

/**
 * Created by elephant on 2019-11-28 15:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class IDAuthenticationViewModel : BaseViewModel() {

    private lateinit var currentAccount: AccountDO

    private val ssoService by lazy {
        DataRepository.getSSOService()
    }

    private val localUserService by lazy {
        DataRepository.getLocalUserService()
    }

    val countryAreaVO = MutableLiveData<CountryAreaVO>()
    val idPhotoFront = MutableLiveData<Uri?>()
    val idPhotoBack = MutableLiveData<Uri?>()
    val authenticationResult = MutableLiveData<Boolean>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // 加载默认的国家地区
            val countryArea = getCountryArea()
            countryAreaVO.postValue(countryArea)

            currentAccount = AccountManager().currentAccount()
        }
    }

    override suspend fun realExecute(action: Int, vararg params: Any) {
        // 上传证件正面图片
        val idPhotoFrontFile = try {
            getFilePathFromContentUri(idPhotoFront.value!!)
        } catch (e: Exception) {
            e.printStackTrace()

            throw IOException(getString(R.string.hint_id_photo_front_unavailable))
        }
        val idPhotoFrontUrl = try {
            ssoService.uploadImage(idPhotoFrontFile).data!!
        } catch (e: Exception) {
            e.printStackTrace()

            throw if (e is RequestException)
                e
            else
                IOException(getString(R.string.tips_id_photo_front_upload_failure))
        }

        // 上传证件背面图片
        val idPhotoBackFile = try {
            getFilePathFromContentUri(idPhotoBack.value!!)
        } catch (e: Exception) {
            e.printStackTrace()

            throw IOException(getString(R.string.hint_id_photo_back_unavailable))
        }
        val idPhotoBackUrl = try {
            ssoService.uploadImage(idPhotoBackFile).data!!
        } catch (e: Exception) {
            e.printStackTrace()

            throw if (e is RequestException)
                e
            else
                IOException(getString(R.string.tips_id_photo_back_upload_failure))
        }

        // 绑定身份信息
        val walletAddress = currentAccount.address
        val countryCode = countryAreaVO.value!!.countryCode
        val idName = params[0] as String
        val idNumber = params[1] as String
        ssoService.bindIdNumber(
            walletAddress = walletAddress,
            name = idName,
            countryCode = countryCode,
            idNumber = idNumber,
            idPhotoPositiveUrl = idPhotoFrontUrl,
            idPhotoBackUrl = idPhotoBackUrl
        )

        // 上传图片时返回的图片url不是全路径，所以这里从服务器获取用户身份信息
        val userInfoDTO = ssoService.loadUserInfo(walletAddress).data
        if (userInfoDTO == null
            || userInfoDTO.idName.isNullOrEmpty()
            || userInfoDTO.idNumber.isNullOrEmpty()
            || userInfoDTO.idPhotoFrontUrl.isNullOrEmpty()
            || userInfoDTO.idPhotoBackUrl.isNullOrEmpty()
            || userInfoDTO.countryCode.isNullOrEmpty()
        ) {
            throw RequestException.responseDataException()
        }

        val idInfo = IDInfo(
            idName = userInfoDTO.idName,
            idNumber = userInfoDTO.idNumber,
            idPhotoFrontUrl = userInfoDTO.idPhotoFrontUrl,
            idPhotoBackUrl = userInfoDTO.idPhotoBackUrl,
            idCountryCode = userInfoDTO.countryCode,
            idAuthenticationStatus = IDAuthenticationStatus.AUTHENTICATED
        )

        localUserService.setIDInfo(idInfo)
        EventBus.getDefault().post(AuthenticationIDEvent(idInfo))

        authenticationResult.postValue(true)
    }

    override fun checkParams(action: Int, vararg params: Any): Boolean {
        if (params.isEmpty()) {
            return false
        }

        val countryAreaVO = countryAreaVO.value
        if (countryAreaVO == null) {
            tipsMessage.postValue(getString(R.string.hint_select_country_area))
            return false
        }

        val idName = params[0] as String
        if (idName.isEmpty()) {
            tipsMessage.postValue(getString(R.string.hint_enter_name))
            return false
        }

        val idNumber = params[1] as String
        if (idNumber.isEmpty()) {
            tipsMessage.postValue(
                getString(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.hint_enter_id_number_china
                    } else {
                        R.string.hint_enter_id_number_other
                    }
                )
            )
            return false
        } else if (isChinaMainland(countryAreaVO) && !validationIDCar18(idNumber)) {
            tipsMessage.postValue(getString(R.string.hint_id_number_format_incorrect_china))
            return false
        }

        if (idPhotoFront.value == null) {
            tipsMessage.postValue(
                getString(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.hint_photograph_id_card_front_china
                    } else {
                        R.string.hint_photograph_id_card_front_other
                    }
                )
            )
            return false
        } else if (idPhotoBack.value == null) {
            tipsMessage.postValue(
                getString(
                    if (isChinaMainland(countryAreaVO)) {
                        R.string.hint_photograph_id_card_back_china
                    } else {
                        R.string.hint_photograph_id_card_back_other
                    }
                )
            )
            return false
        }

        return true
    }
}