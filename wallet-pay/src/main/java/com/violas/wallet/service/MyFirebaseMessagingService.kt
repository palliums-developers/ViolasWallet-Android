package com.violas.wallet.service

import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.palliums.extensions.logDebug
import com.violas.wallet.R
import com.violas.wallet.ui.message.MessageCenterActivity

/**
 * Created by elephant on 2020/10/9 17:33.
 * Copyright © 2019-2020. All rights reserved.
 * <p>
 * desc:
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        logDebug { "onNewToken. Refreshed token: $token" }

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        // sendRegistrationToServer(token);
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // ...

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        logDebug { "onMessageReceived. From: ${remoteMessage.from}" }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            logDebug { "onMessageReceived. Message data payload: ${remoteMessage.data}" }

            /* Check if data needs to be processed by long running job */
            if (true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                // scheduleJob();
            } else {
                // Handle message within 10 seconds
                // handleNow();
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            logDebug { "onMessageReceived. Message Notification title : ${it.title}" }
            logDebug { "onMessageReceived. Message Notification body : ${it.body}" }
            logDebug { "onMessageReceived. Message Notification channelId : ${it.channelId}" }

            if (it.title.isNullOrBlank() || it.body.isNullOrBlank()) return@let
            sendNotification(it.title!!, it.body!!, remoteMessage.data, it.channelId)
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     */
    private fun sendNotification(
        messageTitle: CharSequence,
        messageBody: CharSequence,
        messageData: Map<String, String>,
        notificationChannelId: String?
    ) {
        val extras = Bundle()
        messageData.keys.forEach {
            extras.putString(it, messageData[it])
        }
        val intent = Intent(this, MessageCenterActivity::class.java)
        intent.putExtras(extras)
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val channelId = notificationChannelId ?: getString(R.string.notification_channel_id_default)
        com.palliums.extensions.sendNotification(
            channelId = channelId,
            smallIcon = R.mipmap.ic_notification,
            contentTitle = messageTitle as String,
            contentText = messageBody as String,
            intent = intent,
            priority = when (channelId) {
                getString(R.string.notification_channel_id_transaction) -> {
                    NotificationCompat.PRIORITY_HIGH
                }

                getString(R.string.notification_channel_id_events) -> {
                    NotificationCompat.PRIORITY_DEFAULT
                }

                getString(R.string.notification_channel_id_update) -> {
                    NotificationCompat.PRIORITY_LOW
                }

                else -> {
                    NotificationCompat.PRIORITY_LOW
                }
            },
            number = 11
        )
    }

    /**
     * 在某些情况下，FCM 可能不会传递消息。如果在特定设备连接 FCM 时，您的应用在该设备上的待处理
     * 消息过多（超过 100 条），或者如果设备超过一个月未连接到 FCM，就会发生这种情况。在这些情况下，
     * 您可能会收到对 FirebaseMessagingService.onDeletedMessages() 的回调。当应用实例收到此回调时，
     * 应该执行一次与应用服务器的完全同步。如果您在过去 4 周内未向该设备上的应用发送消息，
     * FCM 将不会调用 onDeletedMessages()。
     */
    override fun onDeletedMessages() {

    }
}