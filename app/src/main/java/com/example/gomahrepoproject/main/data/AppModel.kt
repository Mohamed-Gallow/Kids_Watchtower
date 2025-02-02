package com.example.gomahrepoproject.main.data

import android.graphics.drawable.Drawable

data class AppModel(
    val name: String,       // اسم التطبيق
    val packageName: String, // اسم الحزمة (Package Name)
    val icon: Drawable       // أيقونة التطبيق
)