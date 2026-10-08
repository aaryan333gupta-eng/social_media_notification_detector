package com.example.socialapp

import android.content.Context

data class SupportedSocialApp(
    val packageName: String,
    val displayName: String
)

object SocialAppSelection {
    private const val PREFERENCES_NAME = "notification_setup"
    private const val SELECTED_APPS_KEY = "selected_apps"
    private const val SETUP_COMPLETE_KEY = "setup_complete"

    val supportedApps = listOf(
        SupportedSocialApp("com.google.android.gm", "Gmail"),
        SupportedSocialApp("com.google.android.apps.messaging", "Messages"),
        SupportedSocialApp("com.samsung.android.messaging", "Messages"),
        SupportedSocialApp("com.instagram.android", "Instagram"),
        SupportedSocialApp("com.facebook.katana", "Facebook"),
        SupportedSocialApp("com.facebook.orca", "Messenger"),
        SupportedSocialApp("com.instagram.barcelona", "Threads"),
        SupportedSocialApp("com.twitter.android", "X"),
        SupportedSocialApp("com.linkedin.android", "LinkedIn"),
        SupportedSocialApp("com.snapchat.android", "Snapchat"),
        SupportedSocialApp("com.reddit.frontpage", "Reddit"),
        SupportedSocialApp("com.pinterest", "Pinterest"),
        SupportedSocialApp("com.whatsapp", "WhatsApp")
    )

    fun selectedPackages(context: Context): Set<String> =
        preferences(context).getStringSet(SELECTED_APPS_KEY, emptySet())?.toSet().orEmpty()

    fun saveSelection(context: Context, packageNames: Set<String>) {
        preferences(context).edit().putStringSet(SELECTED_APPS_KEY, packageNames).apply()
    }

    fun selectedAppName(context: Context, packageName: String): String? {
        if (packageName !in selectedPackages(context)) return null
        return supportedApps.firstOrNull { it.packageName == packageName }?.displayName
    }

    fun isSetupComplete(context: Context): Boolean =
        preferences(context).getBoolean(SETUP_COMPLETE_KEY, false)

    fun markSetupComplete(context: Context) {
        preferences(context).edit().putBoolean(SETUP_COMPLETE_KEY, true).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}