package com.example.gomahrepoproject.main.blockapps

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.gomahrepoproject.R

object BlockingOverlay {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    fun show(context: Context, message: String) {
        if (overlayView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_blocked_app, null)
        overlayView?.findViewById<TextView>(R.id.overlayText)?.text = message

        windowManager?.addView(overlayView, params)
    }

    fun hide() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }
}

