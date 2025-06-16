plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs")
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.gomahrepoproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gomahrepoproject"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(libs.play.services.location)

    // LiveData and Lifecycle
    val lifecycle_ver = "2.8.7"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_ver")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_ver")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycle_ver")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Navigation
    val nav_version = "2.8.3"
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")

    // Lottie
    implementation("com.airbnb.android:lottie:6.6.0")

    // Room
    val room_version = "2.6.1"
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation ("com.google.android.material:material:1.4.0")
    //smooth appbar
    implementation ("com.github.ibrahimsn98:SmoothBottomBar:1.7.9")
    //interceptor
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")

    //googlemaps
    implementation("com.google.maps.android:android-maps-utils:3.9.0")

    //firebase ui
    implementation ("com.firebaseui:firebase-ui-auth:8.0.2")

    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation ("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation ("com.google.firebase:firebase-database-ktx:21.0.0")


}