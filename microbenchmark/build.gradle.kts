/*
 * Copyright (C) 2026 The Android Open Source Project
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

// The androidx.benchmark Gradle plugin is deliberately not applied. Version 1.3.4 of it reaches for
// the legacy `TestedExtension`, which this AGP no longer registers, so applying it fails outright.
// The plugin is a convenience wrapper: it flips the module to the release build type, wires the
// benchmark runner, and adds clock-locking tasks. The first two are done explicitly below. The
// third is a device-side concern and is handled by the runner arguments instead.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.google.jetpackcamera.microbenchmark"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testOptions.targetSdk = libs.versions.targetSdk.get().toInt()
        lint.targetSdk = libs.versions.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"

        // The module under test is flavoured and this module is not, so pick its only flavour.
        missingDimensionStrategy("flavor", "stable")
    }

    // Benchmarking a debuggable build measures the debugger, not the code, so the instrumentation
    // tests are built against release.
    testBuildType = "release"
    buildTypes {
        release {
            isDefault = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    androidTestImplementation(libs.androidx.benchmark.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.junit)

    val composeBom = platform(libs.compose.bom)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.foundation.layout)

    androidTestImplementation(project(":ui:components:capture"))
}
