package com.violas.wallet.event

import com.violas.wallet.repository.local.user.PhoneInfo

/**
 * Created by elephant on 2019-11-29 17:20.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc: 绑定手机事件
 */
class BindPhoneEvent(
    val phoneInfo: PhoneInfo
)