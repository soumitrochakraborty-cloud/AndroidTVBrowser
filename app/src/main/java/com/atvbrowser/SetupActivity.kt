package com.atvbrowser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "ATVBrowserPrefs"
        const val KEY_URL = "saved_url"
        const val KEY_REFRESH_INTERVAL = "refresh_interval"
        const val KEY_KIOSK_MODE = "kiosk_mode"
        const val KEY_FIRST_LAUNCH = "first_launch"
    }

    private lateinit var urlInput: EditText
    private lateinit var refreshSpinner: Spinner
    private lateinit var kioskSwitch: Switch
    private lateinit var startButton: Button
    private lateinit var statusText: TextView

    // Refresh interval options in seconds (0 = disabled)
    private val refreshOptions = arrayOf(
        "Disabled",
        "15 seconds",
        "30 seconds",
        "1 minute",
        "2 minutes",
        "5 minutes",
        "10 minutes",
        "15 minutes",
        "30 minutes",
        "1 hour"
    )
    private val refreshValues = intArrayOf(0, 15, 30, 60, 120, 300, 600, 900, 1800, 3600)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if we should skip setup and go directly to browser
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        if (!isFirstLaunch) {
            // Already configured — go straight to browser
            launchBrowser()
            return
        }

        setContentView(R.layout.activity_setup)

        urlInput = findViewById(R.id.urlInput)
        refreshSpinner = findViewById(R.id.refreshSpinner)
        kioskSwitch = findViewById(R.id.kioskSwitch)
        startButton = findViewById(R.id.startButton)
        statusText = findViewById(R.id.statusText)

        // Load saved values if any
        urlInput.setText(prefs.getString(KEY_URL, "https://"))
        kioskSwitch.isChecked = prefs.getBoolean(KEY_KIOSK_MODE, true)

        // Setup refresh interval spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, refreshOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        refreshSpinner.adapter = adapter

        // Restore saved refresh interval
        val savedInterval = prefs.getInt(KEY_REFRESH_INTERVAL, 60)
        val intervalIndex = refreshValues.indexOf(savedInterval).let { if (it < 0) 3 else it }
        refreshSpinner.setSelection(intervalIndex)

        // Start button
        startButton.setOnClickListener { saveAndLaunch() }

        // Allow Enter key on URL input to proceed
        urlInput.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                saveAndLaunch()
                true
            } else false
        }

        // Focus URL input
        urlInput.requestFocus()
    }

    private fun saveAndLaunch() {
        val url = urlInput.text.toString().trim()

        if (url.isEmpty() || url == "https://") {
            statusText.text = "⚠ Please enter a valid URL"
            statusText.visibility = View.VISIBLE
            urlInput.requestFocus()
            return
        }

        // Auto-add https:// if missing
        val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else url

        val refreshIndex = refreshSpinner.selectedItemPosition
        val refreshInterval = refreshValues[refreshIndex]
        val kioskMode = kioskSwitch.isChecked

        // Save preferences
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_URL, finalUrl)
            putInt(KEY_REFRESH_INTERVAL, refreshInterval)
            putBoolean(KEY_KIOSK_MODE, kioskMode)
            putBoolean(KEY_FIRST_LAUNCH, false)
            apply()
        }

        launchBrowser()
    }

    private fun launchBrowser() {
        startActivity(Intent(this, BrowserActivity::class.java))
        finish()
    }
}
