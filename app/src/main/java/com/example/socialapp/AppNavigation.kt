package com.example.socialapp

import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

@RequiresApi(Build.VERSION_CODES.ECLAIR)
fun AppCompatActivity.setupBottomNavigation(selectedItemId: Int) {
    val bottomBar = findViewById<BottomNavigationView>(R.id.bottomBar)
    bottomBar.selectedItemId = selectedItemId
    bottomBar.setOnItemSelectedListener { item ->
        if (item.itemId == selectedItemId) {
            return@setOnItemSelectedListener true
        }

        val destination = when (item.itemId) {
            R.id.nav_home -> MainActivity::class.java
            R.id.nav_notifications -> NotificationsActivity::class.java
            R.id.nav_activity -> ActivityActivity::class.java
            R.id.nav_profile -> ProfileActivity::class.java
            else -> return@setOnItemSelectedListener false
        }

        startActivity(
            Intent(this, destination).addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        true
    }
}