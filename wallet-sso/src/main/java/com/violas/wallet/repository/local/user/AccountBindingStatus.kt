package com.violas.wallet.repository.local.user

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-11-29 13:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 账号绑定状态
 */

@IntDef(
    AccountBindingStatus.UNKNOWN,
    AccountBindingStatus.UNBOUND,
    AccountBindingStatus.BOUND
)
annotation class AccountBindingStatus {

    companion object {
        const val UNKNOWN = 0   // 未知
        const val UNBOUND = 1   // 未绑定
        const val BOUND = 2     // 已绑定
    }
}