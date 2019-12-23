package com.violas.wallet.repository.local.user

/**
 * Created by elephant on 2019-11-29 18:01.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 邮箱信息
 */
data class EmailInfo(
    var emailAddress: String,

    @AccountBindingStatus
    var accountBindingStatus: Int
) {

    fun isBoundEmail(): Boolean {
        return accountBindingStatus == AccountBindingStatus.BOUND
    }
}