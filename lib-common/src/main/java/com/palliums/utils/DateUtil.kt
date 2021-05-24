package com.palliums.utils

import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
    return when (dateStr.length) {
        10 -> "${dateStr}000"
        16 -> dateStr.substring(0, 13)
        else -> dateStr
    }
}

/**
 * 修正日期长度
 */
fun correctDateLength(date: Long): Long {
    return when (date.toString().length) {
        10 -> date * 1000
        16 -> date / 1000
        else -> date
    }
}

fun formatDate(
    date: Long,
    simpleDateFormat: SimpleDateFormat? = null,
    pattern: String = "MM.dd HH:mm:ss"
): String {
    val dateFormat = simpleDateFormat ?: SimpleDateFormat(pattern, Locale.ENGLISH)
    return dateFormat.format(correctDateLength(date))
}

fun formatDateWithNotNeedCorrectDate(
    date: Long,
    simpleDateFormat: SimpleDateFormat? = null,
    pattern: String = "MM.dd HH:mm:ss"
): String {
    val dateFormat = simpleDateFormat ?: SimpleDateFormat(pattern, Locale.ENGLISH)
    return dateFormat.format(date)
}

fun isExpired(expirationDate: Long): Boolean {
    return System.currentTimeMillis() > correctDateLength(expirationDate)
}

fun utcToLocal(
    utcTime: String,
    utcTimePattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    localTimePattern: String = "yyyy-MM-dd HH:mm:ss.SSS"
): Long {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val localDateTime = ZonedDateTime.parse(utcTime).toLocalDateTime().plusHours(8)
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localTimeStr = dateTimeFormatter.format(localDateTime)
        Date(localTimeStr).time
    } else {
        val utcSdf = SimpleDateFormat(utcTimePattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcDate = utcSdf.parse(utcTime)

        val localSdf = SimpleDateFormat(localTimePattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        val localTime = localSdf.format(utcDate)
        val localDate = localSdf.parse(localTime)
        localDate.time
    }
}

