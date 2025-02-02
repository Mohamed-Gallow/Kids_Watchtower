package com.example.gomahrepoproject.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.gomahrepoproject.R
import com.example.gomahrepoproject.splash.SplashAdapter
import com.example.gomahrepoproject.splash.ZoomOutPageTransformer

class SplashActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private val delay = 1000L // الزمن بين كل حركة (1 ثوانٍ)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // إعداد ViewPager
        viewPager = findViewById(R.id.viewPager)
        val adapter = SplashAdapter(this)
        viewPager.adapter = adapter

        
        viewPager.isUserInputEnabled = false  // تعطيل التمرير (إلغاء الحركة اليدوية من قبل المستخدم)
        // تأثير الترانزيشن
        viewPager.setPageTransformer(ZoomOutPageTransformer())

        // الحركة التلقائية
        setupAutoSlide(adapter.itemCount)
    }

    private fun setupAutoSlide(totalPages: Int) {
        runnable = object : Runnable {
            override fun run() {
                val currentItem = viewPager.currentItem
                if (currentItem < totalPages - 1) {
                    viewPager.currentItem = currentItem + 1 // انتقل للصفحة التالية
                } else {
                    navigateToMainActivity() // عند آخر صفحة، انتقل إلى MainActivity
                }
                handler.postDelayed(this, delay) // استمر في الحركة
            }
        }
        handler.postDelayed(runnable, delay)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()  // تأكد من إنهاء SplashActivity بعد الانتقال
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // إيقاف الحركة التلقائية عند تدمير النشاط
    }
}