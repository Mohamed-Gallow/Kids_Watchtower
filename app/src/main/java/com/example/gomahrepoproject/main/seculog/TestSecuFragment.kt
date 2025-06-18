package com.example.gomahrepoproject.main.seculog

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TestSecuFragment : Fragment() {

    private var webView: WebView? = null
    private var etUrl: EditText? = null
    private val blockedSites = mutableSetOf<String>()
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var isSitesLoaded = false
    private var retryCount = 0
    private val maxRetries = 3
    private var lastEnteredUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBlockedSitesListener()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_test_security, container, false)
        webView = view.findViewById(R.id.webView)
        etUrl = view.findViewById(R.id.etUrl)
        setupWebView()
        setupButtons(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!onBackPressed()) parentFragmentManager.popBackStack()
            }
        })
    }

    private fun setupWebView() {
        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.setGeolocationEnabled(false)
            settings.userAgentString = "Mozilla/5.0 Chrome/91 Safari/537.36"
            webViewClient = createWebViewClient()
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<LinearLayout>(R.id.btnSearch)?.setOnClickListener {
            lastEnteredUrl = etUrl?.text?.toString()?.trim()
            if (!isSitesLoaded) {
                if (retryCount < maxRetries) {
                    retryCount++
                    showToast("Sites loading... ($retryCount/$maxRetries)")
                    setupBlockedSitesListener()
                } else {
                    showToast("Proceeding without block list.")
                    isSitesLoaded = true
                }
                return@setOnClickListener
            }

            if (lastEnteredUrl.isNullOrBlank()) {
                showToast("Please enter a URL.")
            } else if (!isValidUrl(lastEnteredUrl!!)) {
                showToast("Invalid URL.")
            } else if (isBlocked(lastEnteredUrl!!)) {
                showBlockedPage(webView, lastEnteredUrl)
            } else {
                webView?.loadUrl(formatUrl(lastEnteredUrl!!))
            }
        }

        view.findViewById<ImageView>(R.id.ivBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupBlockedSitesListener() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val role = snapshot.child("role").getValue(String::class.java)
                    if (role == "child") {
                        findAllParentsOfChild(userId)
                    } else {
                        listenForOwnBlockedSites(userId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DBG", "Failed to load user info: ${error.message}")
                    isSitesLoaded = true
                }
            })
    }

    private fun findAllParentsOfChild(childId: String) {
        database.getReference("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val parentIds = mutableListOf<String>()
                    for (userSnap in snapshot.children) {
                        val role = userSnap.child("role").getValue(String::class.java)
                        if (role == "parent") {
                            val linkedId = userSnap.child("linkedAccounts").child("childId").getValue(String::class.java)
                            if (linkedId == childId) {
                                userSnap.key?.let { parentIds.add(it) }
                            }
                        }
                    }

                    if (parentIds.isEmpty()) {
                        showToast("No parent found for this child.")
                        isSitesLoaded = true
                    } else {
                        loadAllParentsBlockedSites(parentIds)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DBG", "Error finding parents: ${error.message}")
                    isSitesLoaded = true
                }
            })
    }

    private fun loadAllParentsBlockedSites(parentIds: List<String>) {
        blockedSites.clear()
        var remaining = parentIds.size

        for (parentId in parentIds) {
            database.getReference("users").child(parentId).child("blockedSites")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.mapNotNullTo(blockedSites) { it.getValue(String::class.java) }
                        remaining--
                        if (remaining == 0) {
                            isSitesLoaded = true
                            Log.d("BLOCKED_SITES", "Loaded from all parents: $blockedSites")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("DBG", "Failed loading from parent $parentId: ${error.message}")
                        remaining--
                        if (remaining == 0) isSitesLoaded = true
                    }
                })
        }
    }

    private fun listenForOwnBlockedSites(userId: String) {
        database.getReference("users").child(userId).child("blockedSites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    blockedSites.clear()
                    snapshot.children.mapNotNullTo(blockedSites) { it.getValue(String::class.java) }
                    isSitesLoaded = true
                    Log.d("BLOCKED_SITES", "Your blocked sites: $blockedSites")
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Error loading your block list: ${error.message}")
                    isSitesLoaded = true
                }
            })
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return blockIfNeeded(url, view)
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return blockIfNeeded(request.url.toString(), view)
            }

            override fun onLoadResource(view: WebView, url: String) {
                if (isBlocked(url)) {
                    view.stopLoading()
                    showBlockedPage(view, lastEnteredUrl)
                }
            }
        }
    }

    private fun blockIfNeeded(url: String, view: WebView): Boolean {
        return if (isBlocked(url)) {
            showBlockedPage(view, url)
            true
        } else false
    }

    private fun isBlocked(url: String): Boolean {
        val domain = extractDomain(url)
        return blockedSites.any {
            val blockedDomain = extractDomain(it)
            domain == blockedDomain || domain.endsWith(".$blockedDomain")
        }
    }

    private fun extractDomain(url: String): String {
        return try {
            Uri.parse(formatUrl(url)).host?.removePrefix("www.")?.lowercase().orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    private fun formatUrl(url: String): String {
        return if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            Uri.parse(formatUrl(url)).isHierarchical
        } catch (e: Exception) {
            false
        }
    }

    private fun showBlockedPage(view: WebView?, baseUrl: String?) {
        view?.loadDataWithBaseURL(
            baseUrl,
            """
            <html><head>
            <style>body{text-align:center;padding:50px;font-family:sans-serif;background:#f8f8f8}h1{color:#d32f2f}</style>
            </head><body>
            <h1>🚫 Website Blocked</h1>
            <p>This site is blocked by your parent account.</p>
            </body></html>
            """,
            "text/html", "UTF-8", null
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        webView?.destroy()
        super.onDestroyView()
    }

    fun onBackPressed(): Boolean {
        return webView?.canGoBack() == true && webView?.goBack().let { true } ?: false
    }
}