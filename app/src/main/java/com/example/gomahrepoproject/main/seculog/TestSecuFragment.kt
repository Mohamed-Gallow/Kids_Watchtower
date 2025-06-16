package com.example.gomahrepoproject.main.seculog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gomahrepoproject.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TestSecuFragment : Fragment() {

    private var webView: WebView? = null
    private var etUrl: EditText? = null
    private val blockedSites = mutableListOf<String>()
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var blockedSitesListener: ValueEventListener? = null

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
        val view = inflater.inflate(R.layout.fragment_test_secu, container, false)

        // Initialize views
        webView = view.findViewById(R.id.webView) ?: run {
            Toast.makeText(requireContext(), "WebView initialization failed", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return null
        }

        etUrl = view.findViewById(R.id.etUrl) ?: run {
            Toast.makeText(requireContext(), "URL input initialization failed", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return null
        }

        val btnGo: Button = view.findViewById(R.id.btnGo) ?: run {
            Toast.makeText(requireContext(), "Button initialization failed", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return null
        }

        // Configure WebView safely
        try {
            webView?.settings?.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            webView?.webViewClient = createWebViewClient()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "WebView configuration failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            parentFragmentManager.popBackStack()
            return null
        }

        // Set up button click listener
        btnGo.setOnClickListener { loadUrlSafely() }

        // Handle back button
        view.findViewById<ImageView>(R.id.ivBack)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun listenForBlockedSites() {
        val childId = auth.currentUser?.uid ?: return
        val usersRef = database.getReference("users").child(childId).child("linkedAccounts").child("parentId")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val parentId = snapshot.getValue(String::class.java)
                if (parentId != null) {
                    val blockedSitesRef = database.getReference("users").child(parentId).child("blockedSites")
                    blockedSitesListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            blockedSites.clear()
                            for (site in snapshot.children) {
                                val url = site.getValue(String::class.java)
                                if (url != null) {
                                    blockedSites.add(url)
                                }
                            }
                            Toast.makeText(
                                requireContext(),
                                "Blocked sites updated: ${blockedSites.size} sites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                requireContext(),
                                "Error fetching blocked sites: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    blockedSitesRef.addValueEventListener(blockedSitesListener!!)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No linked parent account found",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error fetching parent ID: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun loadUrlSafely() {
        try {
            val url = etUrl?.text.toString().trim()
            when {
                url.isEmpty() -> {
                    Toast.makeText(requireContext(), "Please enter a URL", Toast.LENGTH_SHORT).show()
                    return
                }
                isBlocked(url) -> {
                    Toast.makeText(requireContext(), "This website is blocked!", Toast.LENGTH_LONG).show()
                    return
                }
                else -> {
                    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        "https://$url"
                    } else {
                        url
                    }
                    webView?.loadUrl(formattedUrl)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading URL: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return if (isBlocked(url)) {
                    Toast.makeText(
                        requireContext(),
                        "Blocked website detected",
                        Toast.LENGTH_LONG
                    ).show()
                    true
                } else false
            }

            override fun onPageFinished(view: WebView, url: String) {
                etUrl?.setText(url)
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Toast.makeText(
                    requireContext(),
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

    override fun onDestroyView() {
        super.onDestroyView()
        webView = null
        etUrl = null
        // Remove Firebase listener to prevent memory leaks
        blockedSitesListener?.let { listener ->
            database.getReference("users").child(auth.currentUser?.uid ?: "").child("linkedAccounts")
                .child("parentId").get().addOnSuccessListener { snapshot ->
                    val parentId = snapshot.getValue(String::class.java)
                    if (parentId != null) {
                        database.getReference("users").child(parentId).child("blockedSites")
                            .removeEventListener(listener)
                    }
                }
        }
        blockedSitesListener = null
    }

    fun onBackPressed(): Boolean {
        return if (webView?.canGoBack() == true) {
            webView?.goBack()
            true
        } else {
            false
        }
    }

    companion object {
        fun newInstance(): TestSecuFragment {
            return TestSecuFragment()
        }
    }
}