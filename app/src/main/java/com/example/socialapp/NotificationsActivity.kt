package com.example.socialapp

import android.content.ComponentName
import android.content.ActivityNotFoundException
import android.content.res.ColorStateList
import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.cardview.widget.CardView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup

class NotificationsActivity : AppCompatActivity() {

    private lateinit var notificationList: LinearLayout
    private lateinit var accessPanel: View
    private lateinit var emptyMessage: TextView
    private lateinit var folderSelector: RadioGroup
    private lateinit var appFilterGroup: ChipGroup
    private var showingVeryImportant = false
    private var selectedAppFilter: String? = null
    private val appFilterNames = mutableMapOf<Int, String?>()
    private var lastRefreshAt = 0L
    private var lastRefreshCornerTapAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)
        setupBottomNavigation(R.id.nav_notifications)

        notificationList = findViewById(R.id.notificationList)
        accessPanel = findViewById(R.id.notificationAccessPanel)
        emptyMessage = findViewById(R.id.inboxEmptyMessage)
        folderSelector = findViewById(R.id.notificationFolderSelector)
        appFilterGroup = findViewById(R.id.appFilterGroup)
        folderSelector.setOnCheckedChangeListener { _, checkedId ->
            showingVeryImportant = checkedId == R.id.veryImportantFolder
            renderNotifications()
        }

        findViewById<Button>(R.id.enableNotificationAccessButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.manageNotificationAppsButton).setOnClickListener {
            startActivity(
                Intent(this, NotificationSetupActivity::class.java)
                    .putExtra(NotificationSetupActivity.EXTRA_EDITING, true)
            )
        }

        findViewById<View>(R.id.inboxRefreshGestureArea).setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - lastRefreshCornerTapAt <= ViewConfiguration.getDoubleTapTimeout()) {
                lastRefreshCornerTapAt = 0L
                refreshInbox()
            } else {
                lastRefreshCornerTapAt = now
            }
        }
    }

    override fun onResume() {
        super.onResume()
        buildAppFilters()
        renderNotifications()
    }

    private fun buildAppFilters() {
        appFilterGroup.setOnCheckedStateChangeListener(null)
        appFilterGroup.removeAllViews()
        appFilterNames.clear()

        val appNames = SocialAppSelection.selectedPackages(this)
            .mapNotNull { packageName ->
                SocialAppSelection.supportedApps
                    .firstOrNull { it.packageName == packageName }
                    ?.displayName
            }
            .distinct()
        val allAppsChip = createAppFilterChip("All")
        appFilterNames[allAppsChip.id] = null
        appFilterGroup.addView(allAppsChip)

        appNames.forEach { appName ->
            val chip = createAppFilterChip(appName)
            appFilterNames[chip.id] = appName
            appFilterGroup.addView(chip)
        }

        val selectedChipId = appFilterNames.entries
            .firstOrNull { it.value == selectedAppFilter }
            ?.key
        val selectedChip = if (selectedChipId == null) {
            allAppsChip
        } else {
            appFilterGroup.findViewById<Chip>(selectedChipId)
        }
        selectedChip.isChecked = true

        appFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedAppFilter = checkedIds.firstOrNull()?.let(appFilterNames::get)
            renderNotifications()
        }
    }

    private fun createAppFilterChip(label: String): Chip = Chip(this).apply {
        id = View.generateViewId()
        text = label
        isCheckable = true
        isClickable = true
        chipStrokeWidth = dp(1).toFloat()
        chipStrokeColor = ColorStateList.valueOf(Color.rgb(214, 219, 231))
        chipBackgroundColor = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(Color.rgb(48, 55, 89), Color.WHITE)
        )
        setTextColor(
            ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(Color.WHITE, Color.rgb(55, 65, 81))
            )
        )
    }

    private fun renderNotifications() {
        val hasAccess = isNotificationAccessEnabled()
        accessPanel.visibility = if (hasAccess) View.GONE else View.VISIBLE
        notificationList.removeAllViews()

        val allNotifications = SocialNotificationStore.getAll(this)
        val folderNotifications = allNotifications.filter {
            it.isVeryImportant == showingVeryImportant
        }
        val appNotifications = selectedAppFilter?.let { appName ->
            folderNotifications.filter { it.appName == appName }
        } ?: folderNotifications
        val notifications = appNotifications
        emptyMessage.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        emptyMessage.text = when {
            notifications.isNotEmpty() -> ""
            showingVeryImportant -> "Double-tap a notification in Inbox to save it here."
            selectedAppFilter != null -> "No notifications from $selectedAppFilter in this folder yet."
            hasAccess -> "New social notifications will appear here."
            else -> "Enable notification access to start your inbox."
        }

        notifications.forEach { notification ->
            notificationList.addView(createNotificationCard(notification))
        }
    }

    private fun refreshInbox() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRefreshAt < 500L) return
        lastRefreshAt = now
        buildAppFilters()
        renderNotifications()
        Toast.makeText(this, "Inbox refreshed", Toast.LENGTH_SHORT).show()
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()

        return enabledListeners.split(':').any { listener ->
            ComponentName.unflattenFromString(listener)?.packageName == packageName
        }
    }

    private fun createNotificationCard(notification: SocialNotification): View {
        val card = CardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.rgb(255, 255, 255))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(249, 250, 251))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val appName = TextView(this).apply {
            text = notification.appName
            setTextColor(Color.rgb(17, 24, 39))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val time = TextView(this).apply {
            text = DateUtils.getRelativeTimeSpanString(
                notification.postedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            setTextColor(Color.rgb(107, 114, 128))
            textSize = 11f
        }

        val importantLabel = TextView(this).apply {
            text = if (notification.isVeryImportant) "Saved" else "Important"
            setTextColor(if (notification.isVeryImportant) Color.rgb(67, 56, 202) else Color.rgb(154, 52, 18))
            textSize = 10f
            setPadding(dp(7), dp(4), dp(7), dp(4))
            setBackgroundColor(if (notification.isVeryImportant) Color.rgb(238, 242, 255) else Color.rgb(255, 237, 213))
            visibility = if (notification.isImportant || notification.isVeryImportant) View.VISIBLE else View.GONE
        }

        val title = TextView(this).apply {
            text = notification.title
            setTextColor(Color.rgb(31, 41, 55))
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(10), 0, 0)
            setLineSpacing(0f, 1.15f)
        }

        val message = TextView(this).apply {
            text = notification.message
            setTextColor(Color.rgb(75, 85, 99))
            textSize = 14f
            setPadding(0, dp(4), 0, 0)
            setLineSpacing(0f, 1.2f)
        }

        heading.addView(appName)
        heading.addView(importantLabel)
        heading.addView(time)
        content.addView(heading)
        if (notification.title.isNotBlank()) content.addView(title)
        if (notification.message.isNotBlank()) content.addView(message)
        card.addView(content)
        attachCardGestures(card, notification)
        return card
    }

    private fun attachCardGestures(card: CardView, notification: SocialNotification) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0f
        var startY = 0f
        var horizontalSwipe = false
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                openSourceApp(notification)
                return true
            }

            override fun onDoubleTap(event: MotionEvent): Boolean {
                toggleVeryImportant(notification)
                return true
            }
        })

        card.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    horizontalSwipe = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val distanceX = event.rawX - startX
                    val distanceY = event.rawY - startY
                    if (kotlin.math.abs(distanceX) > touchSlop &&
                        kotlin.math.abs(distanceX) > kotlin.math.abs(distanceY)
                    ) {
                        horizontalSwipe = true
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        view.translationX = distanceX
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val distanceX = event.rawX - startX
                    if (horizontalSwipe && kotlin.math.abs(distanceX) >= card.width * 0.25f) {
                        dismissCard(card, notification, distanceX)
                    } else {
                        card.animate().translationX(0f).setDuration(140).start()
                    }
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    card.animate().translationX(0f).setDuration(140).start()
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    true
                }
                else -> true
            }
        }
    }

    private fun openSourceApp(notification: SocialNotification) {
        val packageNames = if (notification.packageName.isNotBlank()) {
            listOf(notification.packageName)
        } else {
            SocialAppSelection.supportedApps
                .filter { it.displayName == notification.appName }
                .map(SupportedSocialApp::packageName)
        }
        val launchIntent = packageNames.firstNotNullOfOrNull {
            packageManager.getLaunchIntentForPackage(it)
        }

        if (launchIntent == null) {
            Toast.makeText(this, "${notification.appName} is not available on this phone", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            startActivity(launchIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Could not open ${notification.appName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dismissCard(card: CardView, notification: SocialNotification, distanceX: Float) {
        card.animate()
            .translationX(if (distanceX > 0) card.width.toFloat() else -card.width.toFloat())
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                SocialNotificationStore.delete(this, notification)
                notificationList.removeView(card)
                if (notificationList.childCount == 0) renderNotifications()
            }
            .start()
    }

    private fun toggleVeryImportant(notification: SocialNotification) {
        val saveToVeryImportant = !notification.isVeryImportant
        SocialNotificationStore.setVeryImportant(
            this,
            notification.key,
            saveToVeryImportant
        )
        renderNotifications()
        val message = if (saveToVeryImportant) {
            "Saved to Very Important"
        } else {
            "Moved back to Inbox"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
