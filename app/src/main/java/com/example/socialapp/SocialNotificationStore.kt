package com.example.socialapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SocialNotificationStore {
    private const val PREFERENCES_NAME = "social_notifications"
    private const val NOTIFICATIONS_KEY = "items"
    private const val DISMISSED_KEY = "dismissed_items"
    private const val MAX_NOTIFICATIONS = 100
    private const val MAX_DISMISSED_KEYS = 200

    @Synchronized
    fun getAll(context: Context): List<SocialNotification> {
        val storedItems = preferences(context).getString(NOTIFICATIONS_KEY, "[]") ?: "[]"
        val jsonItems = JSONArray(storedItems)
        return (0 until jsonItems.length())
            .mapNotNull { index -> jsonItems.optJSONObject(index)?.toNotification() }
            .sortedByDescending(SocialNotification::postedAt)
    }

    @Synchronized
    fun save(context: Context, notification: SocialNotification) {
        if (isDismissed(context, notification.key, notification.postedAt)) return

        val currentItems = getAll(context)
        val preserveVeryImportant = currentItems.any {
            it.key == notification.key && it.isVeryImportant
        }
        val updatedItems = currentItems
            .filterNot { it.key == notification.key }
            .plus(notification.copy(isVeryImportant = notification.isVeryImportant || preserveVeryImportant))
            .sortedByDescending(SocialNotification::postedAt)
            .take(MAX_NOTIFICATIONS)
        writeAll(context, updatedItems)
    }

    @Synchronized
    fun setVeryImportant(context: Context, key: String, isVeryImportant: Boolean) {
        val updatedItems = getAll(context).map { notification ->
            if (notification.key == key) {
                notification.copy(isVeryImportant = isVeryImportant)
            } else {
                notification
            }
        }
        writeAll(context, updatedItems)
    }

    @Synchronized
    fun delete(context: Context, notification: SocialNotification) {
        val dismissed = dismissedItems(context)
        dismissed.put(notification.key, notification.postedAt)
        val boundedDismissals = JSONObject()
        dismissed.keys().asSequence()
            .toList()
            .takeLast(MAX_DISMISSED_KEYS)
            .forEach { key -> boundedDismissals.put(key, dismissed.optLong(key)) }
        preferences(context).edit()
            .putString(DISMISSED_KEY, boundedDismissals.toString())
            .apply()

        writeAll(context, getAll(context).filterNot { it.key == notification.key })
    }

    @Synchronized
    private fun isDismissed(context: Context, key: String, postedAt: Long): Boolean {
        val dismissed = dismissedItems(context)
        val dismissedAt = dismissed.optLong(key, -1L)
        if (dismissedAt < 0L) return false
        if (postedAt <= dismissedAt) return true

        dismissed.remove(key)
        preferences(context).edit().putString(DISMISSED_KEY, dismissed.toString()).apply()
        return false
    }

    private fun dismissedItems(context: Context): JSONObject {
        val storedItems = preferences(context).getString(DISMISSED_KEY, "{}") ?: "{}"
        return JSONObject(storedItems)
    }

    private fun writeAll(context: Context, notifications: List<SocialNotification>) {
        val jsonItems = JSONArray()

        notifications.forEach { item ->
            jsonItems.put(
                JSONObject()
                    .put("key", item.key)
                    .put("appName", item.appName)
                    .put("title", item.title)
                    .put("message", item.message)
                    .put("postedAt", item.postedAt)
                    .put("isImportant", item.isImportant)
                    .put("isVeryImportant", item.isVeryImportant)
                    .put("packageName", item.packageName)
            )
        }

        preferences(context).edit().putString(NOTIFICATIONS_KEY, jsonItems.toString()).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private fun JSONObject.toNotification() = SocialNotification(
        key = optString("key"),
        appName = optString("appName"),
        title = optString("title"),
        message = optString("message"),
        postedAt = optLong("postedAt"),
        isImportant = optBoolean("isImportant"),
        isVeryImportant = optBoolean("isVeryImportant"),
        packageName = optString("packageName")
    )
}