package com.violas.wallet.repository.local.user

import android.content.Context
import com.palliums.content.ContextProvider

/**
 * Created by elephant on 2019-11-29 10:34.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 本地用户 service
 */
class LocalUserService {

    private val userRepository by lazy {
        LocalUserRepository(
            ContextProvider.getContext().getSharedPreferences(
                "user",
                Context.MODE_PRIVATE
            )
        )
    }

    /**
     * 清除所有缓存
     */
    fun clear() {
        userRepository.clear()
    }

    /**
     *  设置邮箱手机的绑定状态为未绑定状态，身份的认证状态为未认证状态
     *  在创建身份钱包时调用
     */
    fun setAllStatusToNoOperation() {
        userRepository.setAllStatus(
            AccountBindingStatus.UNBOUND,
            AccountBindingStatus.UNBOUND,
            IDAuthenticationStatus.UNAUTHORIZED
        )
    }

    /**
     * 设置邮箱信息
     */
    fun setEmailInfo(emailInfo: EmailInfo) {
        userRepository.setEmailInfo(emailInfo)
    }

    /**
     * 获取邮箱信息
     */
    fun getEmailInfo(): EmailInfo {
        try {
            val emailInfo = userRepository.getEmailInfo(AccountBindingStatus.UNKNOWN)

            if (emailInfo.accountBindingStatus == AccountBindingStatus.BOUND
                && emailInfo.emailAddress.isEmpty()
            ) {
                emailInfo.accountBindingStatus = AccountBindingStatus.UNKNOWN
            } else if (emailInfo.emailAddress.isNotEmpty()
                && emailInfo.accountBindingStatus != AccountBindingStatus.BOUND
            ) {
                emailInfo.emailAddress = ""
            }

            return emailInfo
        } catch (ignore: Exception) {
            return EmailInfo("", AccountBindingStatus.UNKNOWN)
        }
    }

    /**
     * 设置手机信息
     */
    fun setPhoneInfo(phoneInfo: PhoneInfo) {
        userRepository.setPhoneInfo(phoneInfo)
    }

    /**
     * 获取手机信息
     */
    fun getPhoneInfo(): PhoneInfo {
        try {
            val phoneInfo = userRepository.getPhoneInfo(AccountBindingStatus.UNKNOWN)

            if (phoneInfo.accountBindingStatus == AccountBindingStatus.BOUND
                && (phoneInfo.areaCode.isEmpty() || phoneInfo.phoneNumber.isEmpty())
            ) {
                phoneInfo.accountBindingStatus = AccountBindingStatus.UNKNOWN
                phoneInfo.areaCode = ""
                phoneInfo.phoneNumber = ""
            } else if (phoneInfo.areaCode.isNotEmpty()
                && phoneInfo.phoneNumber.isNotEmpty()
                && phoneInfo.accountBindingStatus != AccountBindingStatus.BOUND
            ) {
                phoneInfo.areaCode = ""
                phoneInfo.phoneNumber = ""
            }

            return phoneInfo
        } catch (ignore: Exception) {
            return PhoneInfo("", "", AccountBindingStatus.UNKNOWN)
        }
    }

    /**
     * 设置身份信息
     */
    fun setIDInfo(idInfo: IDInfo) {
        userRepository.setIDInfo(idInfo)
    }

    /**
     * 获取身份信息
     */
    fun getIDInfo(): IDInfo {
        try {
            val idInfo = userRepository.getIDInfo(IDAuthenticationStatus.UNKNOWN)

            if (idInfo.idAuthenticationStatus != IDAuthenticationStatus.UNKNOWN
                && idInfo.idAuthenticationStatus != IDAuthenticationStatus.UNAUTHORIZED
                && (idInfo.idName.isEmpty()
                        || idInfo.idNumber.isEmpty()
                        || idInfo.idPhotoFrontUrl.isEmpty()
                        || idInfo.idPhotoBackUrl.isEmpty()
                        || idInfo.idCountryCode.isEmpty())
            ) {
                idInfo.idAuthenticationStatus = IDAuthenticationStatus.UNKNOWN
                idInfo.idName = ""
                idInfo.idNumber = ""
                idInfo.idPhotoFrontUrl = ""
                idInfo.idPhotoBackUrl = ""
                idInfo.idCountryCode = ""
            } else if (idInfo.idName.isNotEmpty()
                && idInfo.idNumber.isNotEmpty()
                && idInfo.idPhotoFrontUrl.isNotEmpty()
                && idInfo.idPhotoBackUrl.isNotEmpty()
                && idInfo.idCountryCode.isNotEmpty()
                && (idInfo.idAuthenticationStatus == IDAuthenticationStatus.UNKNOWN
                        || idInfo.idAuthenticationStatus == IDAuthenticationStatus.UNAUTHORIZED)
            ) {
                idInfo.idName = ""
                idInfo.idNumber = ""
                idInfo.idPhotoFrontUrl = ""
                idInfo.idPhotoBackUrl = ""
                idInfo.idCountryCode = ""
            }

            return idInfo
        } catch (ignore: Exception) {
            return IDInfo.newEmptyInstance()
        }
    }
}