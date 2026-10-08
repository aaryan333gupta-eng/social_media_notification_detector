package com.example.socialapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var recentActivityList: LinearLayout
    private lateinit var recentActivityEmpty: TextView
    private lateinit var threeHourCountText: TextView
    private lateinit var importantCountText: TextView
    private var dashboardReady = false
    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockUpdater = object : Runnable {
        override fun run() {
            val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEjm")
            findViewById<TextView>(R.id.dateText).text =
                DateFormat.format(pattern, System.currentTimeMillis())
            clockHandler.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SocialAppSelection.isSetupComplete(this)) {
            startActivity(Intent(this, NotificationSetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        recentActivityList = findViewById(R.id.recentActivityList)
        recentActivityEmpty = findViewById(R.id.recentActivityEmpty)
        threeHourCountText = findViewById(R.id.threeHourCount)
        importantCountText = findViewById(R.id.importantCount)
        val dateText = findViewById<TextView>(R.id.dateText)
        dashboardReady = true

        val profileImage = findViewById<View>(R.id.profileImage)
        val messages3hCard = findViewById<View>(R.id.messages3hCard)
        val importantCard = findViewById<View>(R.id.importantCard)

        dateText.setOnClickListener {
            val isVisible = dateText.alpha > 0.1f
            dateText.alpha = if (isVisible) 0f else 1f
            if (!isVisible) {
                clockUpdater.run()
            }
        }

        setupBottomNavigation(R.id.nav_home)
        updateHomeSummary()
        renderRecentActivity()

        messages3hCard.setOnClickListener { showThreeHourBreakdownPopup(it) }
        importantCard.setOnClickListener {
            Toast.makeText(this, "Priority rules can be set later", Toast.LENGTH_SHORT).show()
        }

        profileImage.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (!dashboardReady) return
        clockHandler.removeCallbacks(clockUpdater)
        clockUpdater.run()
        updateHomeSummary()
        renderRecentActivity()
    }

    override fun onPause() {
        clockHandler.removeCallbacks(clockUpdater)
        super.onPause()
    }

    private fun updateHomeSummary() {
        val now = System.currentTimeMillis()
        val threeHoursAgo = now - 3L * 60L * 60L * 1000L

        val notifications = SocialNotificationStore.getAll(this)
        val recentThreeHours = notifications.filter { it.postedAt >= threeHoursAgo }

        threeHourCountText.text = recentThreeHours.size.toString()
        importantCountText.text = notifications.count { it.isImportant }.toString()
    }

    private fun showThreeHourBreakdownPopup(anchor: View) {
        val notifications = SocialNotificationStore.getAll(this)
        val threeHoursAgo = System.currentTimeMillis() - 3L * 60L * 60L * 1000L
        val recentThreeHours = notifications.filter { it.postedAt >= threeHoursAgo }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = "Messages in last 3h"
            setTextColor(Color.rgb(17, 24, 39))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(10))
        }
        container.addView(title)

        val counts = recentThreeHours
            .groupBy { it.appName }
            .map { (appName, entries) -> appName to entries.size }
            .sortedByDescending { it.second }

        if (counts.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No messages in the last 3 hours"
                setTextColor(Color.rgb(107, 114, 128))
                textSize = 14f
            })
        } else {
            counts.forEach { (appName, appCount) ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(6), 0, dp(6))
                }

                val appText = TextView(this).apply {
                    text = appName
                    setTextColor(Color.rgb(31, 41, 55))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }

                val countText = TextView(this).apply {
                    text = appCount.toString()
                    setTextColor(Color.rgb(109, 94, 245))
                    textSize = 14f
                    setTypeface(typeface, Typeface.BOLD)
                }

                row.addView(appText)
                row.addView(countText)
                container.addView(row)
            }
        }

        val popupWindow = PopupWindow(
            container,
            (resources.displayMetrics.widthPixels * 0.72).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 12f
        popupWindow.showAsDropDown(anchor, 0, -dp(10))
    }

    private fun renderRecentActivity() {
        val notifications = SocialNotificationStore.getAll(this).take(3)
        recentActivityEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        recentActivityList.removeAllViews()
        notifications.forEach { notification ->
            recentActivityList.addView(createRecentActivityCard(notification))
        }
    }

    private fun createRecentActivityCard(notification: SocialNotification): View {
        val card = CardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val appName = TextView(this).apply {
            text = notification.appName
            setTextColor(Color.rgb(31, 41, 55))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val time = TextView(this).apply {
            text = DateUtils.getRelativeTimeSpanString(
                notification.postedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            setTextColor(Color.rgb(156, 163, 175))
            textSize = 11f
        }

        heading.addView(appName)
        heading.addView(time)
        content.addView(heading)

        val summary = if (notification.message.isNotBlank()) notification.message else notification.title
        if (summary.isNotBlank()) {
            content.addView(TextView(this).apply {
                text = summary
                setTextColor(Color.rgb(75, 85, 99))
                textSize = 13f
                setPadding(0, dp(5), 0, 0)
                maxLines = 2
            })
        }

        card.addView(content)
        return card
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
