package com.violas.wallet.repository.local.user

import android.content.SharedPreferences
import com.google.gson.Gson

/**
 * Created by elephant on 2019-11-29 10:43.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 本地用户 repository
 */
class LocalUserRepository(private val sharedPreferences: SharedPreferences) {

    companion object {

        // 用户邮箱信息 key
        private const val KEY_EMAIL_ADDRESS = "KEY_EMAIL_ADDRESS"
        private const val KEY_EMAIL_BINDING_STATUS = "KEY_EMAIL_BINDING_STATUS"

        // 用户手机信息 key
        private const val KEY_PHONE_NUMBER = "KEY_PHONE_NUMBER"
        private const val KEY_PHONE_AREA_CODE = "KEY_PHONE_AREA_CODE"
        private const val KEY_PHONE_BINDING_STATUS = "KEY_PHONE_BINDING_STATUS"

        // 用户身份信息 key
        private const val KEY_ID_INFO = "KEY_ID_INFO"
        private const val KEY_ID_AUTHENTICATION_STATUS = "KEY_ID_AUTHENTICATION_STATUS"
    }

    /**
     * 清除用户信息
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * 设置邮箱绑定状态、手机绑定状态、和身份认证状态
     */
    fun setAllStatus(
        emailBindingStatus: Int,
        phoneBindingStatus: Int,
        idAuthenticationStatus: Int
    ) {
        sharedPreferences.edit()
            .putInt(KEY_EMAIL_BINDING_STATUS, emailBindingStatus)
            .putInt(KEY_PHONE_BINDING_STATUS, phoneBindingStatus)
            .putInt(KEY_ID_AUTHENTICATION_STATUS, idAuthenticationStatus)
            .apply()
    }

    /**
     * 设置邮箱地址
     */
    fun setEmailInfo(emailInfo: EmailInfo) {
        sharedPreferences.edit()
            .putString(KEY_EMAIL_ADDRESS, emailInfo.emailAddress)
            .putInt(KEY_EMAIL_BINDING_STATUS, emailInfo.accountBindingStatus)
            .apply()
    }

    /**
     * 获取邮箱地址
     */
    fun getEmailInfo(defValue: Int): EmailInfo {
        val status = sharedPreferences.getInt(KEY_EMAIL_BINDING_STATUS, defValue)
        val emailAddress = sharedPreferences.getString(KEY_EMAIL_ADDRESS, null) ?: ""

        return EmailInfo(emailAddress, status)
    }

    /**
     * 设置手机信息
     */
    fun setPhoneInfo(phoneInfo: PhoneInfo) {
        sharedPreferences.edit()
            .putString(KEY_PHONE_NUMBER, phoneInfo.phoneNumber)
            .putString(KEY_PHONE_AREA_CODE, phoneInfo.areaCode)
            .putInt(KEY_PHONE_BINDING_STATUS, phoneInfo.accountBindingStatus)
            .apply()
    }

    /**
     * 获取手机信息
     */
    fun getPhoneInfo(defValue: Int): PhoneInfo {
        val status = sharedPreferences.getInt(KEY_PHONE_BINDING_STATUS, defValue)
        val phoneNumber = sharedPreferences.getString(KEY_PHONE_NUMBER, null) ?: ""
        val areaCode = sharedPreferences.getString(KEY_PHONE_AREA_CODE, null) ?: ""

        return PhoneInfo(areaCode, phoneNumber, status)
    }

    /**
     * 设置身份信息
     */
    fun setIDInfo(idInfo: IDInfo) {
        val idInformationJson = Gson().toJson(idInfo)
        sharedPreferences.edit()
            .putString(KEY_ID_INFO, idInformationJson)
            .putInt(KEY_ID_AUTHENTICATION_STATUS, idInfo.idAuthenticationStatus)
            .apply()
    }

    /**
     * 获取身份信息
     */
    fun getIDInfo(defValue: Int): IDInfo {
        val status = sharedPreferences.getInt(KEY_ID_AUTHENTICATION_STATUS, defValue)
        val idInformationJson = sharedPreferences.getString(KEY_ID_INFO, null)

        return if (idInformationJson.isNullOrEmpty()) {
            IDInfo(
                "",
                "",
                "",
                "",
                "",
                idAuthenticationStatus = status
            )
        } else {
            try {
                Gson().fromJson(idInformationJson, IDInfo::class.java).apply {
                    idAuthenticationStatus = status
                }
            } catch (e: Exception) {
                IDInfo(
                    "",
                    "",
                    "",
                    "",
                    "",
                    idAuthenticationStatus = status
                )
            }
        }
    }
}