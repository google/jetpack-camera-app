plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.google.jetpackcamera.feature.postcapture"
    compileSdk = 35

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()
        lint.targetSdk = libs.versions.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    flavorDimensions += "flavor"
    productFlavors {
        create("stable") {
            dimension = "flavor"
            isDefault = true
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
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
        managedDevices {
            localDevices {
                create("pixel2Api28") {
                    device = "Pixel 2"
                    apiLevel = 28
                }
                create("pixel8Api34") {
                    device = "Pixel 8"
                    apiLevel = 34
                    systemImageSource = "aosp_atd"
                }
            }
        }
    }

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

dependencies {

    // Reflect
    implementation(libs.kotlin.reflect)
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose - Material Design 3
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Compose - Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Compose - Integration with ViewModels with Navigation and Hilt
    implementation(libs.hilt.navigation.compose)

    // Compose - Lifecycle utilities
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose - Testing
    androidTestImplementation(libs.compose.junit)
    debugImplementation(libs.compose.test.manifest)
    // noinspection TestManifestGradleConfiguration: required for release build unit tests
    testImplementation(libs.compose.test.manifest)
    testImplementation(libs.compose.junit)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    debugImplementation(libs.androidx.test.monitor)
    implementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Futures
    implementation(libs.futures.ktx)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.compose)

//    // Hilt
//    implementation(libs.dagger.hilt.android)
//    kapt(libs.dagger.hilt.compiler)

    //Tracing
//    implementation(libs.androidx.tracing)

    implementation(libs.kotlinx.atomicfu)

    // Project dependencies
    implementation(project(":data:settings"))
    implementation(project(":core:camera"))
    implementation(project(":core:common"))
    testImplementation(project(":core:common"))
}