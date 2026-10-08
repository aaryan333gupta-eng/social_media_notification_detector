package com.example.socialapp

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SocialNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach(::saveNotification)
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        saveNotification(statusBarNotification)
    }

    private fun saveNotification(statusBarNotification: StatusBarNotification) {
        val appName = SocialAppSelection.selectedAppName(this, statusBarNotification.packageName) ?: return
        val extras = statusBarNotification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val messagingText = Notification.MessagingStyle.Message
            .getMessagesFromBundleArray(extras.getParcelableArray(Notification.EXTRA_MESSAGES))
            .lastOrNull()
            ?.text
            ?.toString()
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }
        val message = sequenceOf(
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            messagingText,
            textLines
        ).mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
            .orEmpty()

        if (title.isBlank() && message.isBlank()) return
        val isImportant = isImportant(statusBarNotification, title, message)

        SocialNotificationStore.save(
            this,
            SocialNotification(
                key = statusBarNotification.key,
                appName = appName,
                title = title,
                message = message,
                postedAt = statusBarNotification.postTime,
                isImportant = isImportant,
                packageName = statusBarNotification.packageName
            )
        )
    }

    private fun isImportant(
        statusBarNotification: StatusBarNotification,
        title: String,
        message: String
    ): Boolean {
        val notification = statusBarNotification.notification
        if (notification.category == Notification.CATEGORY_MESSAGE ||
            notification.category == Notification.CATEGORY_CALL ||
            notification.priority >= Notification.PRIORITY_HIGH
        ) {
            return true
        }

        val content = "$title $message"
        return importantPhrases.any { content.contains(it, ignoreCase = true) }
    }

    private companion object {
        val importantPhrases = listOf(
            "urgent",
            "security alert",
            "verification code",
            "password reset",
            "payment received",
            "invoice",
            "deadline",
            "mentioned you",
            "tagged you",
            "direct message",
            "sent you a message"
        )
    }
}