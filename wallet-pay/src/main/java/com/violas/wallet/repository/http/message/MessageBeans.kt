package com.violas.wallet.repository.http.message

import androidx.annotation.Keep

/**
 * Created by elephant on 12/28/20 2:56 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

@Keep
data class NotificationMsgDTO(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val time: Long = 0,
    val read: Boolean = false
)