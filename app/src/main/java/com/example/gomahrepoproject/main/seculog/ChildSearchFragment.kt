package com.example.gomahrepoproject.main.seculog

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.tasks.await

class ChildSearchFragment : Fragment() {

    private var webView: WebView? = null
    private var etUrl: EditText? = null
    private val blockedSites = mutableListOf<String>()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var lastEnteredUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listenForBlockedSites()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_child_search, container, false)
        initializeViews(view)
        setupWebView()
        setupButtonListeners(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!onBackPressed()) {
                    parentFragmentManager.popBackStack()
                }
            }
        })
    }

    private fun initializeViews(view: View) {
        webView = view.findViewById(R.id.webView) ?: run {
            showToast("WebView initialization failed")
            parentFragmentManager.popBackStack()
            return
        }
        etUrl = view.findViewById(R.id.etUrl) ?: run {
            showToast("URL input initialization failed")
            parentFragmentManager.popBackStack()
            return
        }
    }

    private fun setupWebView() {
        webView?.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                setGeolocationEnabled(false)
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
                setSupportMultipleWindows(false)
                allowFileAccess = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            webViewClient = createWebViewClient()
        } ?: run {
            showToast("WebView configuration failed")
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupButtonListeners(view: View) {
        view.findViewById<Button>(R.id.btnGo)?.setOnClickListener {
            lastEnteredUrl = etUrl?.text?.toString()?.trim()
            loadUrlSafely()
        }
        view.findViewById<ImageView>(R.id.ivBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun listenForBlockedSites() = lifecycleScope.launch {
        val parentUserId = getParentUserId() ?: run {
            showToast("Parent not linked")
            return@launch
        }
        database.getReference("users").child(parentUserId).child("blockedSites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    blockedSites.clear()
                    snapshot.children.forEach { site ->
                        site.getValue(String::class.java)?.let { blockedSites.add(it) }
                    }
                    Log.d("URL_BLOCKER", "Updated blocked sites: $blockedSites")
                }
                override fun onCancelled(error: DatabaseError) {
                    showToast("Error fetching blocked sites: ${error.message}")
                }
            })
    }

    private suspend fun getParentUserId(): String? = withContext(Dispatchers.IO) {
        val childUserId = auth.currentUser?.uid ?: return@withContext null
        try {
            val snapshot = database.getReference("users")
                .child(childUserId)
                .child("parentUserId")
                .get()
                .await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            showToast("Error fetching parent ID: ${e.message}")
            null
        }
    }

    private fun loadUrlSafely() {
        val url = lastEnteredUrl ?: return
        when {
            url.isEmpty() -> showToast("Please enter a URL")
            !isValidUrl(url) -> showToast("Please enter a valid URL")
            isBlocked(url) -> handleBlockedUrl()
            else -> loadValidUrl(url)
        }
    }

    private fun handleBlockedUrl() {
        showToast("This website is blocked!")
        showBlockedPage(webView, lastEnteredUrl)
    }

    private fun loadValidUrl(url: String) {
        try {
            val formattedUrl = formatUrl(url)
            webView?.loadUrl(formattedUrl)
            Log.d("URL_BLOCKER", "Loading URL: $formattedUrl")
        } catch (e: Exception) {
            showToast("Invalid URL: ${e.message}")
            Log.e("URL_BLOCKER", "Failed to load URL: $url", e)
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                Log.d("WEBVIEW", "shouldOverrideUrlLoading: $url")
                return checkAndBlock(url, view)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                Log.d("WEBVIEW", "shouldOverrideUrlLoading request: ${request.url}")
                return checkAndBlock(request.url.toString(), view)
            }

            override fun onLoadResource(view: WebView, url: String) {
                Log.d("WEBVIEW", "onLoadResource: $url")
                if (isBlocked(url)) {
                    view.stopLoading()
                    showBlockedPage(view, lastEnteredUrl)
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (url != "about:blank" && lastEnteredUrl != null && !url.contains("blocked")) {
                    etUrl?.setText(url)
                }
                Log.d("WEBVIEW", "Page finished loading: $url")
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                Log.e("WEBVIEW_ERROR", "Error loading ${request.url}: ${error.description}")
                showToast("Failed to load page")
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
                Log.e("WEBVIEW_ERROR", "HTTP Error for ${request.url}: ${errorResponse.statusCode}")
                showToast("HTTP Error: ${errorResponse.statusCode}")
            }
        }
    }

    private fun checkAndBlock(url: String, view: WebView): Boolean {
        return if (isBlocked(url)) {
            showBlockedPage(view, lastEnteredUrl)
            true
        } else {
            false
        }
    }

    private fun isBlocked(url: String): Boolean {
        val domain = extractDomain(url)
        val blocked = blockedSites.any { blocked ->
            val blockedDomain = extractDomain(blocked)
            domain == blockedDomain || domain.endsWith(".$blockedDomain")
        }
        if (blocked) {
            Log.d("URL_BLOCKER", "Blocked URL: $url (domain: $domain)")
        }
        return blocked
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = Uri.parse(formatUrl(url))
            uri.host?.removePrefix("www.")?.lowercase() ?: url
                .removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
                .split('/')[0]
                .lowercase()
        } catch (e: Exception) {
            Log.e("URL_BLOCKER", "Invalid URL for domain extraction: $url", e)
            url.lowercase()
        }
    }

    private fun formatUrl(url: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains("://") -> url
            else -> "https://$url"
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            Uri.parse(formatUrl(url)).isHierarchical
        } catch (e: Exception) {
            false
        }
    }

    private fun showBlockedPage(view: WebView? = webView, baseUrl: String? = null) {
        view?.loadDataWithBaseURL(
            baseUrl,
            """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f8f8f8; }
                    h1 { color: #d32f2f; }
                    p { color: #555; }
                </style>
            </head>
            <body>
                <h1>🚫 Website Blocked</h1>
                <p>This site is restricted by your parent.</p>
            </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        webView?.destroy()
        webView = null
        etUrl = null
        super.onDestroyView()
    }

    fun onBackPressed(): Boolean = webView?.run { if (canGoBack()) { goBack(); true } else false } ?: false
}