package com.atvbrowser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var overlayBar: LinearLayout
    private lateinit var urlDisplay: TextView
    private lateinit var refreshCountdown: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnChangeUrl: ImageButton

    private var countDownTimer: CountDownTimer? = null
    private var refreshIntervalMs: Long = 0
    private var currentUrl: String = ""
    private var isKioskMode: Boolean = true
    private var overlayVisible: Boolean = false
    private var overlayHideTimer: CountDownTimer? = null

    companion object {
        private const val OVERLAY_HIDE_DELAY = 5000L // 5 seconds
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)

        // Load preferences
        val prefs = getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE)
        currentUrl = prefs.getString(SetupActivity.KEY_URL, "https://google.com") ?: "https://google.com"
        val refreshIntervalSec = prefs.getInt(SetupActivity.KEY_REFRESH_INTERVAL, 60)
        refreshIntervalMs = refreshIntervalSec * 1000L
        isKioskMode = prefs.getBoolean(SetupActivity.KEY_KIOSK_MODE, true)

        // Bind views
        webView = findViewById(R.id.webView)
        overlayBar = findViewById(R.id.overlayBar)
        urlDisplay = findViewById(R.id.urlDisplay)
        refreshCountdown = findViewById(R.id.refreshCountdown)
        progressBar = findViewById(R.id.progressBar)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnSettings = findViewById(R.id.btnSettings)
        btnChangeUrl = findViewById(R.id.btnChangeUrl)

        // Full screen kiosk: hide status bar and nav bar
        if (isKioskMode) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        setupWebView()
        setupButtons()
        loadUrl(currentUrl)
        startAutoRefresh()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 9; AndroidTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingIndicator.visibility = View.VISIBLE
                progressBar.progress = 0
                urlDisplay.text = url ?: currentUrl
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingIndicator.visibility = View.GONE
                progressBar.progress = 100
                currentUrl = url ?: currentUrl
                urlDisplay.text = currentUrl
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    loadingIndicator.visibility = View.GONE
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.postDelayed({ progressBar.progress = 0 }, 500)
                }
            }
        }
    }

    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            restartAutoRefresh()
            webView.reload()
        }

        btnChangeUrl.setOnClickListener {
            showUrlDialog()
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Show overlay when D-pad menu/back is pressed
        webView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) hideOverlay()
        }
    }

    private fun loadUrl(url: String) {
        currentUrl = url
        webView.loadUrl(url)
        urlDisplay.text = url
    }

    // ── AUTO-REFRESH ────────────────────────────────────────────────────────────

    private fun startAutoRefresh() {
        countDownTimer?.cancel()
        if (refreshIntervalMs <= 0) {
            refreshCountdown.text = "Auto-refresh: Off"
            return
        }
        countDownTimer = object : CountDownTimer(refreshIntervalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val secs = seconds % 60
                refreshCountdown.text = if (minutes > 0) {
                    "Refresh in ${minutes}m ${secs}s"
                } else {
                    "Refresh in ${secs}s"
                }
            }

            override fun onFinish() {
                webView.reload()
                startAutoRefresh() // restart the countdown
            }
        }.start()
    }

    private fun restartAutoRefresh() {
        countDownTimer?.cancel()
        startAutoRefresh()
    }

    // ── OVERLAY ─────────────────────────────────────────────────────────────────

    private fun toggleOverlay() {
        if (overlayVisible) hideOverlay() else showOverlay()
    }

    private fun showOverlay() {
        overlayVisible = true
        overlayBar.visibility = View.VISIBLE
        overlayBar.alpha = 0f
        overlayBar.animate().alpha(1f).setDuration(200).start()
        scheduleOverlayHide()
    }

    private fun hideOverlay() {
        overlayHideTimer?.cancel()
        overlayVisible = false
        overlayBar.animate().alpha(0f).setDuration(200).withEndAction {
            overlayBar.visibility = View.GONE
        }.start()
    }

    private fun scheduleOverlayHide() {
        overlayHideTimer?.cancel()
        overlayHideTimer = object : CountDownTimer(OVERLAY_HIDE_DELAY, OVERLAY_HIDE_DELAY) {
            override fun onTick(p0: Long) {}
            override fun onFinish() { hideOverlay() }
        }.start()
    }

    // ── DIALOGS ──────────────────────────────────────────────────────────────────

    private fun showUrlDialog() {
        val editText = EditText(this).apply {
            setText(currentUrl)
            selectAll()
            hint = "Enter URL"
        }
        AlertDialog.Builder(this)
            .setTitle("Enter URL")
            .setView(editText)
            .setPositiveButton("Go") { _, _ ->
                var url = editText.text.toString().trim()
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                saveUrlPreference(url)
                loadUrl(url)
                restartAutoRefresh()
                hideOverlay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        val refreshLabels = arrayOf(
            "Disabled", "15 seconds", "30 seconds", "1 minute",
            "2 minutes", "5 minutes", "10 minutes", "15 minutes", "30 minutes", "1 hour"
        )
        val refreshValues = intArrayOf(0, 15, 30, 60, 120, 300, 600, 900, 1800, 3600)
        val currentIndex = refreshValues.indexOf((refreshIntervalMs / 1000).toInt()).let { if (it < 0) 3 else it }

        var selectedIndex = currentIndex
        AlertDialog.Builder(this)
            .setTitle("Auto-Refresh Interval")
            .setSingleChoiceItems(refreshLabels, currentIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton("Save") { _, _ ->
                refreshIntervalMs = refreshValues[selectedIndex] * 1000L
                saveRefreshPreference(refreshValues[selectedIndex])
                restartAutoRefresh()
                hideOverlay()
            }
            .setNeutralButton("Reset App") { _, _ ->
                resetToSetup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetToSetup() {
        getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(SetupActivity.KEY_FIRST_LAUNCH, true)
            apply()
        }
        startActivity(Intent(this, SetupActivity::class.java))
        finish()
    }

    private fun saveUrlPreference(url: String) {
        currentUrl = url
        getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(SetupActivity.KEY_URL, url).apply()
    }

    private fun saveRefreshPreference(intervalSec: Int) {
        getSharedPreferences(SetupActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(SetupActivity.KEY_REFRESH_INTERVAL, intervalSec).apply()
    }

    // ── KEY EVENTS (D-PAD / REMOTE) ──────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                toggleOverlay()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (overlayVisible) {
                    hideOverlay()
                    true
                } else if (!isKioskMode && webView.canGoBack()) {
                    webView.goBack()
                    true
                } else if (isKioskMode) {
                    // In kiosk mode, block back button
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!overlayVisible) {
                    showOverlay()
                    true
                } else super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        countDownTimer?.cancel()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        startAutoRefresh()
        if (isKioskMode) {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        overlayHideTimer?.cancel()
        webView.destroy()
    }
}
