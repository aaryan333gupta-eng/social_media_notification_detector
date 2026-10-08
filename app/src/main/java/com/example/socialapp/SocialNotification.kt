package com.example.socialapp

data class SocialNotification(
    val key: String,
    val appName: String,
    val title: String,
    val message: String,
    val postedAt: Long,
    val isImportant: Boolean = false,
    val isVeryImportant: Boolean = false,
    val packageName: String = ""
)