package com.example.socialapp

import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotificationSetupActivity : AppCompatActivity() {
    private val appCheckboxes = mutableMapOf<String, CheckBox>()
    private var openedNotificationSettings = false
    private val isEditing: Boolean
        get() = intent.getBooleanExtra(EXTRA_EDITING, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_setup)

        findViewById<TextView>(R.id.setupTitle).text = if (isEditing) {
            "Manage notification apps"
        } else {
            "Choose your apps"
        }
        findViewById<TextView>(R.id.setupDescription).text = if (isEditing) {
            "Choose which apps Social Digest should include. Your saved notifications will stay in Inbox."
        } else {
            "Pick the apps whose phone notifications you want in one inbox. You can change this selection before continuing."
        }

        val appList = findViewById<LinearLayout>(R.id.appSelectionList)
        val selectedApps = SocialAppSelection.selectedPackages(this)
        SocialAppSelection.supportedApps.groupBy(SupportedSocialApp::displayName).forEach { (displayName, appVariants) ->
            val checkbox = CheckBox(this).apply {
            text = displayName
                textSize = 16f
                minHeight = dp(52)
                setTextColor(getColor(R.color.black))
                buttonTintList = ColorStateList.valueOf(getColor(R.color.purple_500))
            isChecked = appVariants.any { it.packageName in selectedApps }
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            appCheckboxes[displayName] = checkbox
            appList.addView(checkbox)
        }

        findViewById<Button>(R.id.continueSetupButton).setOnClickListener {
            continueSetup()
        }
        findViewById<Button>(R.id.continueSetupButton).text = if (isEditing) {
            "Save app choices"
        } else {
            "Save apps and continue"
        }
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        if (openedNotificationSettings && hasNotificationAccess() &&
            SocialAppSelection.selectedPackages(this).isNotEmpty()
        ) {
            completeSetup()
        } else {
            updateStatus()
        }
    }

    private fun continueSetup() {
        val selectedAppNames = appCheckboxes
            .filterValues(CheckBox::isChecked)
            .keys
            .toSet()
        val selectedPackages = SocialAppSelection.supportedApps
            .filter { it.displayName in selectedAppNames }
            .map(SupportedSocialApp::packageName)
            .toSet()

        if (selectedAppNames.isEmpty()) {
            findViewById<TextView>(R.id.setupStatus).text =
                "Choose at least one app to continue."
            return
        }

        SocialAppSelection.saveSelection(this, selectedPackages)
        if (hasNotificationAccess()) {
            completeSetup()
        } else {
            openedNotificationSettings = true
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun completeSetup() {
        SocialAppSelection.markSetupComplete(this)
        if (!isEditing) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }

    private fun updateStatus() {
        val status = findViewById<TextView>(R.id.setupStatus)
        status.text = if (hasNotificationAccess()) {
            "Choose which apps to include. Notification access is already enabled."
        } else {
            "Choose apps, then allow Social Digest notification access in Android Settings."
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()

        return enabledListeners.split(':').any { listener ->
            ComponentName.unflattenFromString(listener)?.packageName == packageName
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_EDITING = "editing_app_choices"
    }
}