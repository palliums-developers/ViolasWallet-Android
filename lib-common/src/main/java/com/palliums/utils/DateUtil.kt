package com.palliums.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by elephant on 2019-12-17 11:09.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

/**
 * 修正日期长度
 */
fun correctDateLength(dateStr: String): String {
    return if (dateStr.length == 10)
        "${dateStr}000"
    else
        dateStr
}

/**
 * 修正日期长度
 */
fun correctDateLength(date: Long): Long {
    return if (date.toString().length == 10)
        date * 1000
    else
        date
}

fun formatDate(
    date: Long,
    simpleDateFormat: SimpleDateFormat? = null,
    pattern: String? = null
): String {
    val dateFormat = simpleDateFormat
        ?: SimpleDateFormat(pattern ?: "MM.dd HH:mm:ss", Locale.ENGLISH)
    return dateFormat.format(correctDateLength(date))
}

