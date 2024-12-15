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

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import org.junit.AssumptionViolatedException

/**
 * Allows use of testRule.onNodeWithText that uses an integer string resource
 * rather than a [String] directly.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithText(
    @StringRes strRes: Int
): SemanticsNodeInteraction = onNodeWithText(
    text = getResString(strRes)
)

/**
 * Allows use of testRule.onNodeWithContentDescription that uses an integer string resource
 * rather than a [String] directly.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithContentDescription(
    @StringRes strRes: Int
): SemanticsNodeInteraction = onNodeWithContentDescription(
    label = getResString(strRes)
)

/**
 * Allows searching for a node by [SemanticsProperties.StateDescription] using an integer string
 * resource.
 */
fun SemanticsNodeInteractionsProvider.onNodeWithStateDescription(
    @StringRes strRes: Int
): SemanticsNodeInteraction = onNode(
    SemanticsMatcher.expectValue(
        SemanticsProperties.StateDescription,
        expectedValue = getResString(strRes)
    )
)

/**
 * Fetch a string resources from a [SemanticsNodeInteractionsProvider] context.
 */
fun SemanticsNodeInteractionsProvider.getResString(@StringRes strRes: Int): String {
    return ApplicationProvider.getApplicationContext<Context>().getString(strRes)
}

/**
 * Assumes that the provided [matcher] is satisfied for this node.
 *
 * This is equivalent to [SemanticsNodeInteraction.assert()], but will skip the test rather than
 * fail the test.
 *
 * @param matcher Matcher to verify.
 * @param messagePrefixOnError Prefix to be put in front of an error that gets thrown in case this
 * assert fails. This can be helpful in situations where this assert fails as part of a bigger
 * operation that used this assert as a precondition check.
 *
 * @throws AssumptionViolatedException if the matcher does not match or the node can no
 * longer be found
 */
fun SemanticsNodeInteraction.assume(
    matcher: SemanticsMatcher,
    messagePrefixOnError: (() -> String)? = null
): SemanticsNodeInteraction {
    var errorMessageOnFail = "Failed to assume the following: (${matcher.description})"
    if (messagePrefixOnError != null) {
        errorMessageOnFail = messagePrefixOnError() + "\n" + errorMessageOnFail
    }
    val node = fetchSemanticsNode(errorMessageOnFail)
    if (!matcher.matches(node)) {
        throw AssumptionViolatedException(
            buildGeneralErrorMessage(errorMessageOnFail, this)
        )
    }
    return this
}

fun ComposeTestRule.longClickForVideoRecording() {
    onNodeWithTag(CAPTURE_BUTTON)
        .assertExists()
        .performTouchInput {
            down(center)
        }
    idleForVideoDuration()
    onNodeWithTag(CAPTURE_BUTTON)
        .assertExists()
        .performTouchInput {
            up()
        }
}

fun ComposeTestRule.idleForVideoDuration() {
    // TODO: replace with a check for the timestamp UI of the video duration
    try {
        waitUntil(timeoutMillis = VIDEO_DURATION_MILLIS, condition = { false })
    } catch (_: ComposeTimeoutException) {
    }
}

fun ComposeTestRule.getCurrentLensFacing(): LensFacing = visitQuickSettings {
    onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON).fetchSemanticsNode(
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
}

fun ComposeTestRule.getCurrentFlashMode(): FlashMode = visitQuickSettings {
    onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON).fetchSemanticsNode(
        "Flash button is not visible when expected."
    ).let { node ->
        node.config[SemanticsProperties.ContentDescription].any { description ->
            when (description) {
                getResString(R.string.quick_settings_flash_off_description) ->
                    return@let FlashMode.OFF
                getResString(R.string.quick_settings_flash_on_description) ->
                    return@let FlashMode.ON
                getResString(R.string.quick_settings_flash_auto_description) ->
                    return@let FlashMode.AUTO
                getResString(R.string.quick_settings_flash_llb_description) ->
                    return@let FlashMode.LOW_LIGHT_BOOST
                else -> false
            }
        }
        throw AssertionError("Unable to determine flash mode from quick settings")
    }
}

// Navigates to quick settings if not already there and perform action from provided block.
// This will return from quick settings if not already there, or remain on quick settings if there.
inline fun <T> ComposeTestRule.visitQuickSettings(crossinline block: ComposeTestRule.() -> T): T {
    var needReturnFromQuickSettings = false
    onNodeWithContentDescription(R.string.quick_settings_dropdown_closed_description).apply {
        if (isDisplayed()) {
            performClick()
            needReturnFromQuickSettings = true
        }
    }

    onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description).assertExists(
        "Quick settings can only be entered from PreviewScreen or QuickSettings screen"
    )

    try {
        return block()
    } finally {
        if (needReturnFromQuickSettings) {
            onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description)
                .assertExists()
                .performClick()
        }
    }
}

fun ComposeTestRule.setFlashMode(flashMode: FlashMode) {
    visitQuickSettings {
        // Click the flash button to switch to ON
        onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON)
            .assertExists()
            .assume(isEnabled()) {
                "Current lens does not support any flash modes"
            }.apply {
                val initialFlashMode = getCurrentFlashMode()
                var currentFlashMode = initialFlashMode
                while (currentFlashMode != flashMode) {
                    performClick()
                    currentFlashMode = getCurrentFlashMode()
                    if (currentFlashMode == initialFlashMode) {
                        throw AssumptionViolatedException(
                            "Current lens does not support $flashMode"
                        )
                    }
                }
            }
    }
}

internal fun buildGeneralErrorMessage(
    errorMessage: String,
    nodeInteraction: SemanticsNodeInteraction
): String {
    val sb = StringBuilder()

    sb.appendLine(errorMessage)

    sb.appendLine("Semantics of the node:")
    sb.appendLine(nodeInteraction.printToString())

    return sb.toString()
}
