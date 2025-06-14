package com.example.gomahrepoproject


import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gomahrepoproject.databinding.ActivityBlockingBinding
import com.example.gomahrepoproject.databinding.ActivityMainBinding

class BlockingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockingBinding
    private lateinit var blockedSitesAdapter: BlockedSitesAdapter
    private val blockedSitesList = mutableListOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize adapter with remove callback
        blockedSitesAdapter = BlockedSitesAdapter(blockedSitesList) { urlToRemove ->
            blockedSitesList.remove(urlToRemove)
            blockedSitesAdapter.notifyDataSetChanged()
        }

        binding.rvBlockedSites.layoutManager = LinearLayoutManager(this)
        binding.rvBlockedSites.adapter = blockedSitesAdapter

        binding.btnBlock.setOnClickListener {
            var url = binding.etWebsiteUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "http://$url"
                }

                if (!blockedSitesList.contains(url)) {
                    blockedSitesList.add(url)
                    blockedSitesAdapter.notifyDataSetChanged()
                    binding.etWebsiteUrl.text.clear()
                    Toast.makeText(this, "Website blocked successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Website already blocked", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a website URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnTest.setOnClickListener {
            val intent = Intent(this, WebViewTestActivity::class.java)
            intent.putStringArrayListExtra("blocked_sites", ArrayList(blockedSitesList))
            startActivity(intent)
        }

        binding.ivBack.setOnClickListener {
            finish()
        }
    }
}