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

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

abstract class GitRevParseValueSource : ValueSource<String, GitRevParseValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        var args: List<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        return try {
            val output = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("git", "rev-parse")
                args(parameters.args)
                standardOutput = output
                isIgnoreExitValue = true
            }
            output.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.compose.compiler)
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()

    namespace = "com.google.jetpackcamera"

    defaultConfig {
        applicationId = "com.google.jetpackcamera"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        val baseVersion = "0.1"
        val runNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "0"

        versionCode = if (runNumber != "0") runNumber.toInt() else 1
        versionName = if (runNumber != "0") "$baseVersion.$runNumber" else "$baseVersion.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        val gitFullSha = providers.of(GitRevParseValueSource::class.java) {
            parameters.args = listOf("HEAD")
        }.get()
        val gitShortSha = providers.of(GitRevParseValueSource::class.java) {
            parameters.args = listOf("--short", "HEAD")
        }.get().ifEmpty { "unknown" }

        buildConfigField("String", "PRIMARY_VERSION_STRING", "\"${versionName}-${gitShortSha}\"")
        val buildOrigin = if (System.getenv("GITHUB_ACTIONS") == "true") {
            "GitHub"
        } else {
            "Local Gradle"
        }
        buildConfigField("String", "BUILD_ORIGIN", "\"${buildOrigin}\"")
        buildConfigField("String", "GIT_SHA", "\"${gitFullSha}\"")
        buildConfigField("String", "CHANGELIST", "\"\"")
        buildConfigField("String", "SOONG_BUILD_ID", "\"\"")
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

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
}

dependencies {
    implementation(libs.androidx.tracing)
    implementation(project(":core:common"))
    implementation(project(":feature:postcapture"))
    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose - Material Design 3
    implementation(libs.compose.material3)

    // Compose - Android Studio Preview support
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Compose - Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Compose - Integration with Activities
    implementation(libs.androidx.activity.compose)

    // Compose - Testing
    androidTestImplementation(libs.compose.junit)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.camera.lifecycle) // to reset CameraX between tests
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.testParameterInjector)
    androidTestImplementation(project(":ui:uistate"))
    androidTestImplementation(project(":ui:components:capture"))
    androidTestImplementation(project(":ui:debug"))
    androidTestUtil(libs.androidx.orchestrator)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt
    implementation(libs.dagger.hilt.android)
    kapt(libs.dagger.hilt.compiler)

    // Accompanist - Permissions
    implementation(libs.accompanist.permissions)

    // Jetpack Navigation
    implementation(libs.androidx.navigation.compose)

    // Access settings & model data
    implementation(project(":data:settings"))
    implementation(project(":core:settings:datastore-prefs"))
    implementation(project(":core:settings"))
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)

    // Camera Preview
    implementation(project(":feature:preview"))

    // Settings Screen
    implementation(project(":feature:settings"))

    // Permissions Screen
    implementation(project(":feature:permissions"))
    // benchmark
    implementation(libs.androidx.profileinstaller)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // capture components
    implementation(project(":ui:uistate"))
    implementation(project(":ui:components:capture"))
    implementation(project(":ui:debug"))

    implementation(project(":core:camera:low-light-playservices"))
    implementation(project(":core:camera:effects:single-stream"))
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
