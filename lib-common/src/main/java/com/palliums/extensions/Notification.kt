package com.palliums.extensions

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.StreamType
import androidx.core.app.NotificationManagerCompat
import com.palliums.content.ContextProvider

/**
 * Created by elephant on 2020/10/12 11:28.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */

fun sendNotification(
    channelId: String,
    smallIcon: Int,
    contentTitle: String,
    contentText: String,
    intent: Intent,
    pendingIntentFlags: Int = PendingIntent.FLAG_ONE_SHOT,
    autoCancel: Boolean = true,
    priority: Int? = null,
    number: Int? = null,
    sound: Uri? = null,
    @StreamType audioStreamType: Int? = null,
    context: Context = ContextProvider.getContext()
) {
    sendNotification(
        channelId,
        smallIcon,
        contentTitle,
        contentText,
        PendingIntent.getActivity(context, 0, intent, pendingIntentFlags),
        autoCancel,
        priority,
        number,
        sound,
        audioStreamType,
        context
    )
}

fun sendNotification(
    channelId: String,
    smallIcon: Int,
    contentTitle: String,
    contentText: String,
    contentIntent: PendingIntent,
    autoCancel: Boolean = true,
    priority: Int? = null,
    number: Int? = null,
    sound: Uri? = null,
    @StreamType audioStreamType: Int? = null,
    context: Context = ContextProvider.getContext()
) {
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(smallIcon)
        .setContentTitle(contentTitle)
        .setContentText(contentText)
        .setContentIntent(contentIntent)
        .setAutoCancel(autoCancel)

    // 优先级设置（7.1及以下）
    if (priority != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        notificationBuilder.priority = priority
    }

    // 设置自定义通知计数
    if (number != null) {
        notificationBuilder.setNumber(number)
    }

    // 声音设置
    if (sound != null) {
        if (audioStreamType != null)
            notificationBuilder.setSound(sound, audioStreamType)
        else
            notificationBuilder.setSound(sound)
    }

    sendNotification(notificationBuilder.build())
}

fun sendNotification(
    notification: Notification
) {
    NotificationManagerCompat.from(ContextProvider.getContext())
        .notify(System.currentTimeMillis().toInt(), notification)
}

@RequiresApi(Build.VERSION_CODES.O)
fun createNotificationChannel(
    channelId: String,
    channelName: CharSequence,
    channelImportance: Int = NotificationManager.IMPORTANCE_DEFAULT,
    channelDescription: String? = null,
    groupId: String? = null,
    enableLights: Boolean = false,
    lightColor: Int? = null,
    enableVibration: Boolean = false,
    vibrationPattern: LongArray? = null,
    enableSound: Boolean = true,
    sound: Uri? = null,
    audioAttributes: AudioAttributes? = null,
    showBadge: Boolean = true,
    bypassDnd: Boolean = false,
    allowBubbles: Boolean = true,
    lockscreenVisibility: Int? = null,
    notificationManager: NotificationManager = getNotificationManager()
) {
    if (notificationManager.getNotificationChannel(channelId) == null) {
        val notificationChannel = NotificationChannel(channelId, channelName, channelImportance)

        if (!channelDescription.isNullOrBlank()) {
            notificationChannel.description = channelDescription
        }

        // 通知渠道分组
        if (!groupId.isNullOrBlank()) {
            notificationChannel.group = groupId
        }

        // 指示灯设置
        notificationChannel.enableLights(enableLights)
        if (enableLights) {
            notificationChannel.lightColor = lightColor ?: Color.GREEN
        }

        // 震动设置
        notificationChannel.enableVibration(enableVibration)
        if (enableVibration) {
            notificationChannel.vibrationPattern =
                vibrationPattern ?: longArrayOf(100, 200, 300, 400)
        }

        // 声音设置
        if (enableSound) {
            notificationChannel.setSound(
                sound ?: Settings.System.DEFAULT_NOTIFICATION_URI,
                audioAttributes ?: Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
        } else {
            notificationChannel.setSound(null, null)
        }

        // 显示通知标志
        notificationChannel.setShowBadge(showBadge)

        // 绕过勿扰模式
        notificationChannel.setBypassDnd(bypassDnd)

        // 允许通知气泡
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            notificationChannel.setAllowBubbles(allowBubbles)
        }

        // 锁屏显示设置
        if (lockscreenVisibility != null) {
            notificationChannel.lockscreenVisibility = lockscreenVisibility
        }

        notificationManager.createNotificationChannel(notificationChannel)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun deleteNotificationChannel(
    channelId: String,
    notificationManager: NotificationManager = getNotificationManager()
) {
    if (notificationManager.getNotificationChannel(channelId) != null) {
        notificationManager.deleteNotificationChannel(channelId)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun createNotificationChannelGroup(
    groupId: String,
    groupName: CharSequence,
    notificationManager: NotificationManager = getNotificationManager()
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        || notificationManager.getNotificationChannelGroup(groupId) == null
    ) {
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(groupId, groupName)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun deleteNotificationChannelGroup(
    groupId: String,
    notificationManager: NotificationManager = getNotificationManager()
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
        || notificationManager.getNotificationChannelGroup(groupId) != null
    ) {
        notificationManager.deleteNotificationChannelGroup(groupId)
    }
}

fun getNotificationManager(): NotificationManager {
    return ContextProvider.getContext()
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}