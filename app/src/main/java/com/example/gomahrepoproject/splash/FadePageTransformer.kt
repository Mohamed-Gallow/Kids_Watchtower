package com.example.gomahrepoproject.splash

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class FadePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.alpha = 1 - abs(position)
        view.translationX = view.width * -position
    }
}