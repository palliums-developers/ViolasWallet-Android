package com.violas.wallet.event

import com.violas.wallet.repository.local.user.IDInfo

/**
 * Created by elephant on 2019-11-29 17:12.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 认证身份事件
 */
class AuthenticationIDEvent(
    val idInfo: IDInfo
)