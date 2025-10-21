/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.google.jetpackcamera.ui.components.capture"
    compileSdk = libs.versions.compileSdk.get().toInt()

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
        isCoreLibraryDesugaringEnabled = true
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

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

dependencies {
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    // Accompanist - Permissions
    implementation(libs.accompanist.permissions)

    // Compose - Material Design 3
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Compose - Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Compose - Integration with ViewModels with Navigation and Hilt
    implementation(libs.hilt.navigation.compose)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.compose)

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

    implementation(project(":ui:uistate"))
    implementation(project(":ui:uistate:capture"))
    implementation(project(":core:camera"))
    implementation(project(":core:common"))
    implementation(project(":data:media"))
    implementation(project(":core:model"))
    testImplementation(project(":core:common"))
    testImplementation(project(":data:settings"))

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
