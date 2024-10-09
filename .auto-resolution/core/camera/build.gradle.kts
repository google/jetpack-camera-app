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
    alias(libs.plugins.dagger.hilt.android)
}

android {
    namespace = "com.google.jetpackcamera.core.camera"
    compileSdk = libs.versions.compileSdk.get().toInt()
    compileSdkPreview = libs.versions.compileSdkPreview.get()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()
        lint.targetSdk = libs.versions.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            val versionScript = file("src/main/cpp/jni.lds").absolutePath
            cmake {
                cppFlags += listOf(
                    "-std=c++17",
                    "-O3",
                    "-flto",
                    "-fPIC",
                    "-fno-exceptions",
                    "-fno-rtti",
                    "-fomit-frame-pointer",
                    "-fdata-sections",
                    "-ffunction-sections"
                )
                arguments += listOf(
                    "-DCMAKE_VERBOSE_MAKEFILE=ON",
                    "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--gc-sections " +
                        "-Wl,--version-script=${versionScript}"
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            version = libs.versions.cmake.get()
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    flavorDimensions += "flavor"
    productFlavors {
        create("stable") {
            dimension = "flavor"
            isDefault = true
        }

        create("preview") {
            dimension = "flavor"
            targetSdkPreview = libs.versions.targetSdkPreview.get()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

dependencies {
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.truth)

    // Futures
    implementation(libs.futures.ktx)

    // LiveData
    implementation(libs.androidx.lifecycle.livedata)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)

    // Hilt
    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)

    // Tracing
    implementation(libs.androidx.tracing)
    implementation(libs.kotlinx.atomicfu)

    // Graphics libraries
    implementation(libs.androidx.graphics.core)

    // Project dependencies
    implementation(project(":data:settings"))
    implementation(project(":core:common"))
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
