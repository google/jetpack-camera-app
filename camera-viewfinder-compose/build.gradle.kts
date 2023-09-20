plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.google.jetpackcamera.camerax"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Use SNAPSHOT version for Foundation in order to use GrphicsSurface/EmbeddedGraphicsSurface
    implementation("androidx.compose.foundation:foundation:1.6.0-SNAPSHOT")

    // Compose - Material Design 3
    implementation("androidx.compose.material3:material3")

    // Compose - Testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Compose - Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-tooling")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

    // CameraX
    val camerax_version = "1.4.0-SNAPSHOT"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // AndroidX Core
    val core_version = "1.9.0"
    implementation("androidx.core:core:{$core_version}")
}
