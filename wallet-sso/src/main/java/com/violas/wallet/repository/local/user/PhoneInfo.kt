package com.violas.wallet.repository.local.user

/**
 * Created by elephant on 2019-11-29 17:57.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 手机信息
 */
data class PhoneInfo(
    var areaCode: String,
    var phoneNumber: String,

    @AccountBindingStatus
    var accountBindingStatus: Int = AccountBindingStatus.BOUND
) {

    fun isBoundPhone(): Boolean {
        return accountBindingStatus == AccountBindingStatus.BOUND
    }
}