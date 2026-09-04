plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.docfinder"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.docfinder"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
