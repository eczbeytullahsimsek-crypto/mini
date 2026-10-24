plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.atelier.pad"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.atelier.pad"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "3.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.13.0")
    implementation("androidx.room:room-runtime:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
    testImplementation("junit:junit:4.13.2")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
