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
package com.google.jetpackcamera

import android.app.Activity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.quicksettings.R
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.LensFacing
import java.util.concurrent.atomic.AtomicReference

const val APP_START_TIMEOUT_MILLIS = 5000L
const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 5000L
object UiTestUtil {
    private fun getActivity(activityScenario: ActivityScenario<MainActivity>): MainActivity {
        val activityRef: AtomicReference<MainActivity> = AtomicReference<MainActivity>()
        activityScenario.onActivity(activityRef::set)
        return activityRef.get()
    }

    fun getPreviewCameraAppSettings(
        activityScenario: ActivityScenario<MainActivity>
    ): CameraAppSettings {
        return getActivity(
            activityScenario
        ).previewViewModel!!.previewUiState.value.currentCameraSettings
    }
}

inline fun <reified T : Activity> runScenarioTest(
    crossinline block: ActivityScenario<T>.() -> Unit
) {
    ActivityScenario.launch(T::class.java).use { scenario ->
        scenario.apply(block)
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
