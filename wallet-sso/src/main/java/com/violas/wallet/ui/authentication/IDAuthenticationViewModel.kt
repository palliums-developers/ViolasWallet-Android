package com.violas.wallet.ui.authentication

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.palliums.base.BaseViewModel
import com.palliums.content.ContextProvider
import com.palliums.utils.getString
import com.violas.wallet.R
import com.violas.wallet.biz.AccountManager
import com.violas.wallet.event.AuthenticationIDEvent
import com.violas.wallet.repository.DataRepository
import com.violas.wallet.repository.database.entity.AccountDO
import com.violas.wallet.repository.local.user.IDInfo
import com.violas.wallet.ui.selectCountryArea.CountryAreaVO
import com.violas.wallet.ui.selectCountryArea.getCountryArea
import com.violas.wallet.ui.selectCountryArea.isChinaMainland
import com.violas.wallet.utils.getFilePathFromContentUri
import com.violas.wallet.utils.validationIDCar18
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

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
        if (idPhotoFront.value == null) {
            throw FileNotFoundException(getString(R.string.hint_id_photo_front_not_found))
        }
        val idPhotoFrontFile = try {
            val context = ContextProvider.getContext()
            val inputUri = idPhotoBack.value!!
            getFilePathFromContentUri(inputUri, context)
        } catch (e: Exception) {
            e.printStackTrace()

            throw IOException(getString(R.string.hint_id_photo_front_unavailable))
        }
        val idPhotoFrontUrl = ssoService.uploadImage(idPhotoFrontFile).data!!

        // 上传证件背面图片
        if (idPhotoBack.value == null) {
            throw FileNotFoundException(getString(R.string.hint_id_photo_back_not_found))
        }
        val idPhotoBackFile = try {
            val context = ContextProvider.getContext()
            val inputUri = idPhotoBack.value!!
            getFilePathFromContentUri(inputUri, context)
        } catch (e: Exception) {
            e.printStackTrace()

            throw IOException(getString(R.string.hint_id_photo_back_unavailable))
        }
        val idPhotoBackUrl = ssoService.uploadImage(idPhotoBackFile).data!!

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

        val idInfo = IDInfo(
            idName = idName,
            idNumber = idNumber,
            idPhotoFrontUrl = idPhotoFrontUrl,
            idPhotoBackUrl = idPhotoBackUrl,
            idCountryCode = countryCode
        )

        // 上传图片时返回的图片url不是全路径，所以这里不存储身份信息，通过发送事件让我的页面刷新用户身份信息
        //localUserService.setIDInfo(idInfo)
        EventBus.getDefault().post(AuthenticationIDEvent(idInfo))

        tipsMessage.postValue(getString(R.string.hint_id_authentication_success))
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


    fun someFunc(`in`: InputStream, output: OutputStream) {
        try {
            var read: Int = -1
            `in`.use { input ->
                output.use {
                    while (input.read().also { read = it } != -1) {
                        it.write(read)
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}