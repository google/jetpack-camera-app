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
    namespace = "com.google.jetpackcamera.ui.uistateadapter.capture"
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

    // Compose - Material Design 3
    implementation(libs.compose.material3)

    implementation(project(":data:settings"))
    implementation(project(":core:model"))
    implementation(project(":data:media"))
    implementation(project(":core:camera"))
    implementation(project(":ui:uistate"))
    implementation(project(":ui:uistateadapter"))
    implementation(project(":ui:uistate:capture"))
    implementation(project(":ui:components:capture"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)

}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
