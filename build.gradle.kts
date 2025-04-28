// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url =uri("https://www.jitpack.io") }
    }
    dependencies {
        val nav_version = "2.8.3"
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0") // تحديد الإصدار مباشرةً
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
    }
}
