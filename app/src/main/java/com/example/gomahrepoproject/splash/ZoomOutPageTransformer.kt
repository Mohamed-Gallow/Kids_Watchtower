package com.example.gomahrepoproject.splash

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        val scale = max(0.85f, 1 - abs(position))
        val alpha = max(0.5f, 1 - abs(position))

        view.scaleX = scale
        view.scaleY = scale
        view.alpha = alpha

        view.translationX = view.width * -position * 0.2f
    }
}