package com.violas.wallet.utils

import com.palliums.utils.correctDateLength
import org.palliums.libracore.common.EXPIRATION_DELAYED_DEFAULT

/**
 * Created by elephant on 4/14/21 5:27 PM.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun getDiemOrderTime(expirationTime: Long, confirmedTime: Long? = null): Long {
    return if (confirmedTime != null)
        correctDateLength(confirmedTime) - 1000
    else
        correctDateLength(expirationTime) - EXPIRATION_DELAYED_DEFAULT * 1000 + 500
}

fun getDiemDealTime(expirationTime: Long, confirmedTime: Long? = null): Long {
    return if (confirmedTime != null)
        correctDateLength(confirmedTime)
    else
        correctDateLength(expirationTime) - (EXPIRATION_DELAYED_DEFAULT - 1) * 1000 + 500
}