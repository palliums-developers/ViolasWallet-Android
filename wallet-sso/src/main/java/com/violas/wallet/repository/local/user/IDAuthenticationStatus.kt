package com.violas.wallet.repository.local.user

import androidx.annotation.IntDef

/**
 * Created by elephant on 2019-11-29 13:36.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 身份认证状态
 */

@IntDef(
    IDAuthenticationStatus.UNKNOWN,
    IDAuthenticationStatus.UNAUTHORIZED,
    IDAuthenticationStatus.AUTHENTICATED,
    IDAuthenticationStatus.FAILED,
    IDAuthenticationStatus.AUTHENTICATING
)
annotation class IDAuthenticationStatus {

    companion object {
        const val UNKNOWN = 0           // 未知
        const val UNAUTHORIZED = 1      // 未认证
        const val AUTHENTICATED = 2     // 已认证
        const val FAILED = 3            // 认证失败
        const val AUTHENTICATING = 4    // 认证中
    }
}