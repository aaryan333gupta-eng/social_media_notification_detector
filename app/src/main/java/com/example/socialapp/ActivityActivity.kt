package com.example.socialapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ActivityActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity)
        findViewById<TextView>(R.id.inboxHeader).text = "Activity"
        findViewById<TextView>(R.id.inboxSubheader).text = "Your recent digital rhythm"
        findViewById<TextView>(R.id.updatePlatform1).text = "Inbox review"
        findViewById<TextView>(R.id.updateMessage1).text = "12 notifications today, 4 need follow-up"
        findViewById<TextView>(R.id.updateTime1).text = "Today"
        findViewById<TextView>(R.id.updatePlatform2).text = "Important alerts"
        findViewById<TextView>(R.id.updateMessage2).text = "3 messages marked as high priority"
        findViewById<TextView>(R.id.updateTime2).text = "2h ago"
        findViewById<TextView>(R.id.updatePlatform3).text = "Connected apps"
        findViewById<TextView>(R.id.updateMessage3).text = "WhatsApp, Instagram, Gmail, and Messages are active"
        findViewById<TextView>(R.id.updateTime3).text = "This week"
        setupBottomNavigation(R.id.nav_activity)
    }
}