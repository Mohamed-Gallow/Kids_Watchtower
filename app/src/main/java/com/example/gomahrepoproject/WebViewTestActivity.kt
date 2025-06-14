package com.example.gomahrepoproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class WebViewTestActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var etUrl: EditText
    private val blockedSites = mutableListOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_test)

        // Initialize views with null checks
        webView = findViewById(R.id.webView) ?: run {
            Toast.makeText(this, "WebView initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        etUrl = findViewById(R.id.etUrl) ?: run {
            Toast.makeText(this, "URL input initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val btnGo: Button = findViewById(R.id.btnGo) ?: run {
            Toast.makeText(this, "Button initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Get blocked sites
        blockedSites.addAll(intent?.getStringArrayListExtra("blocked_sites") ?: emptyList())

        // Configure WebView safely
        try {
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            webView.webViewClient = createWebViewClient()
        } catch (e: Exception) {
            Toast.makeText(this, "WebView configuration failed: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Set up button click listener
        btnGo.setOnClickListener { loadUrlSafely() }

        // Handle back button
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }
    }

    private fun loadUrlSafely() {
        try {
            var url = etUrl.text.toString().trim()
            when {
                url.isEmpty() -> {
                    Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
                    return
                }
                isBlocked(url) -> {
                    Toast.makeText(this, "This website is blocked!", Toast.LENGTH_LONG).show()
                    return
                }
                else -> {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://$url"
                    }
                    webView.loadUrl(url)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading URL: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (isBlocked(url)) {
                    Toast.makeText(this@WebViewTestActivity, "Blocked website detected", Toast.LENGTH_LONG).show()
                    true
                } else false
            }

            override fun onPageFinished(view: WebView, url: String) {
                etUrl.setText(url)
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Toast.makeText(
                    this@WebViewTestActivity,
                    "Error loading page: $description",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isBlocked(url: String): Boolean {
        return blockedSites.any { blocked ->
            url.contains(
                blocked.removePrefix("http://")
                    .removePrefix("https://")
                    .removePrefix("www."),
                ignoreCase = true
            )
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}