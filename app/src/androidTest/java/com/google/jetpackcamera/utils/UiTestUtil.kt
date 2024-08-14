/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.google.jetpackcamera.MainActivity
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.settings.model.LensFacing
import java.io.File
import java.net.URLConnection

const val APP_START_TIMEOUT_MILLIS = 10_000L
const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 5_000L
const val VIDEO_CAPTURE_TIMEOUT_MILLIS = 5_000L
const val VIDEO_DURATION_MILLIS = 2_000L

inline fun <reified T : Activity> runScenarioTest(
    crossinline block: ActivityScenario<T>.() -> Unit
) {
    ActivityScenario.launch(T::class.java).use { scenario ->
        scenario.apply(block)
    }
}

inline fun <reified T : Activity> runScenarioTestForResult(
    intent: Intent,
    crossinline block: ActivityScenario<T>.() -> Unit
): Instrumentation.ActivityResult? {
    ActivityScenario.launchActivityForResult<T>(intent).use { scenario ->
        scenario.apply(block)
        return scenario.result
    }
}

context(ActivityScenario<MainActivity>)
fun ComposeTestRule.getCurrentLensFacing(): LensFacing {
    var needReturnFromQuickSettings = false
    onNodeWithContentDescription(R.string.quick_settings_dropdown_closed_description).apply {
        if (isDisplayed()) {
            performClick()
            needReturnFromQuickSettings = true
        }
    }

    onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description).assertExists(
        "LensFacing can only be retrieved from PreviewScreen or QuickSettings screen"
    )

    try {
        return onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON).fetchSemanticsNode(
            "Flip camera button is not visible when expected."
        ).let { node ->
            node.config[SemanticsProperties.ContentDescription].any { description ->
                when (description) {
                    getResString(R.string.quick_settings_front_camera_description) ->
                        return@let LensFacing.FRONT

                    getResString(R.string.quick_settings_back_camera_description) ->
                        return@let LensFacing.BACK

                    else -> false
                }
            }
            throw AssertionError("Unable to determine lens facing from quick settings")
        }
    } finally {
        if (needReturnFromQuickSettings) {
            onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description)
                .assertExists()
                .performClick()
        }
    }
}

fun getTestUri(directoryPath: String, timeStamp: Long, suffix: String): Uri {
    return Uri.fromFile(
        File(
            directoryPath,
            "$timeStamp.$suffix"
        )
    )
}

fun deleteFilesInDirAfterTimestamp(
    directoryPath: String,
    instrumentation: Instrumentation,
    timeStamp: Long
): Boolean {
    var hasDeletedFile = false
    for (file in File(directoryPath).listFiles()) {
        if (file.lastModified() >= timeStamp) {
            file.delete()
            if (file.exists()) {
                file.getCanonicalFile().delete()
                if (file.exists()) {
                    instrumentation.targetContext.applicationContext.deleteFile(file.getName())
                }
            }
            hasDeletedFile = true
        }
    }
    return hasDeletedFile
}

fun doesImageFileExist(uri: Uri, prefix: String): Boolean {
    val file = File(uri.path)
    if (file.exists()) {
        val mimeType = URLConnection.guessContentTypeFromName(uri.path)
        return mimeType != null && mimeType.startsWith(prefix)
    }
    return false
}

fun getIntent(uri: Uri, action: String): Intent {
    val intent = Intent(action)
    intent.setComponent(
        ComponentName(
            "com.google.jetpackcamera",
            "com.google.jetpackcamera.MainActivity"
        )
    )
    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
    return intent
}
