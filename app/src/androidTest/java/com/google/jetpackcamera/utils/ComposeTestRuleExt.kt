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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.settings.R as SettingsR
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.ui.BACK_BUTTON
import com.google.jetpackcamera.settings.ui.BTN_SWITCH_SETTING_LENS_FACING_TAG
import com.google.jetpackcamera.settings.ui.CLOSE_BUTTON
import com.google.jetpackcamera.settings.ui.SETTINGS_TITLE
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BACKGROUND_FOCUSED
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BACKGROUND_MAIN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
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
 * Fetch a string resources from a [SemanticsNodeInteractionsProvider] context.
 */
fun SemanticsNodeInteractionsProvider.getResString(@StringRes strRes: Int): String =
    ApplicationProvider.getApplicationContext<Context>().getString(strRes)

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

// ////////////////////////////
//
// idles
//
// ////////////////////////////
fun ComposeTestRule.waitForStartup(timeoutMillis: Long = APP_START_TIMEOUT_MILLIS) {
    // Wait for the capture button to be displayed
    waitUntil(timeoutMillis = timeoutMillis) {
        onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
    }
}

fun ComposeTestRule.waitForNodeWithTag(tag: String, timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS) {
    waitUntil(timeoutMillis = timeoutMillis) { onNodeWithTag(tag).isDisplayed() }
}

private fun ComposeTestRule.idleForVideoDuration(
    durationMillis: Long = VIDEO_DURATION_MILLIS,
    earlyExitPredicate: () -> Boolean = {
        // If the video capture fails, there is no point to continue the recording, so stop idling
        onNodeWithTag(VIDEO_CAPTURE_FAILURE_TAG).isDisplayed()
    }
) {
    // TODO: replace with a check for the timestamp UI of the video duration
    try {
        waitUntil(timeoutMillis = durationMillis) {
            earlyExitPredicate()
        }
    } catch (_: ComposeTimeoutException) {
    }
}

fun ComposeTestRule.ensureTagNotAppears(
    componentTag: String,
    timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {
    try {
        waitUntil(timeoutMillis = timeoutMillis) {
            onNodeWithTag(componentTag).isDisplayed()
        }
        throw AssertionError(
            "$componentTag should not be present"
        )
    } catch (e: ComposeTimeoutException) {
        /* Do nothing. we want to time out here. */
    }
}

// ////////////////////////////
//
// capture control
//
// ////////////////////////////

fun ComposeTestRule.pressAndDragToLockVideoRecording() {
    onNodeWithTag(CAPTURE_BUTTON)
        .assertExists()
        .performTouchInput {
            down(center)
            moveBy(delta = Offset(-400f, 0f), delayMillis = VIDEO_DURATION_MILLIS)
            up()
        }
    ensureTagNotAppears(VIDEO_CAPTURE_SUCCESS_TAG, VIDEO_CAPTURE_TIMEOUT_MILLIS)
}

fun ComposeTestRule.longClickForVideoRecording(durationMillis: Long = VIDEO_DURATION_MILLIS) {
    onNodeWithTag(CAPTURE_BUTTON)
        .assertExists()
        .performTouchInput {
            down(center)
        }
    idleForVideoDuration(durationMillis)
    onNodeWithTag(CAPTURE_BUTTON)
        .assertExists()
        .performTouchInput {
            up()
        }
}

fun ComposeTestRule.tapStartLockedVideoRecording() {
    assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)
    onNodeWithTag(CAPTURE_BUTTON)
        .assertExists()
        .performClick()
    idleForVideoDuration()
}

// //////////////////////
//
// check preview state
//
// ///////////////////////

/**
 * checks if the hdr capture mode toggle is enabled
 */
fun ComposeTestRule.isHdrToggleEnabled(): Boolean =
    checkComponentStateDescriptionState<Boolean>(CAPTURE_MODE_TOGGLE_BUTTON) { description ->
        when (description) {
            getResString(R.string.capture_mode_image_capture_content_description),
            getResString(R.string.capture_mode_video_recording_content_description) ->
                return@checkComponentStateDescriptionState true

            getResString(
                R.string.capture_mode_image_capture_content_description_disabled
            ), getResString(
                R.string.capture_mode_video_recording_content_description_disabled
            ) -> return@checkComponentStateDescriptionState false

            else -> false
        }
    }

/**
 * Returns the current state of the capture mode toggle button
 */
fun ComposeTestRule.getHdrToggleState(): CaptureMode =
    checkComponentStateDescriptionState(CAPTURE_MODE_TOGGLE_BUTTON) { description ->
        when (description) {
            getResString(R.string.capture_mode_image_capture_content_description),
            getResString(
                R.string.capture_mode_image_capture_content_description_disabled
            ) ->
                CaptureMode.IMAGE_ONLY

            getResString(R.string.capture_mode_video_recording_content_description),
            getResString(
                R.string.capture_mode_video_recording_content_description_disabled
            ) ->
                CaptureMode.VIDEO_ONLY

            else -> null
        }
    }

// //////////////////////
//
// check current quick settings state
//
// ///////////////////////
inline fun <reified T> ComposeTestRule.checkComponentContentDescriptionState(
    nodeTag: String,
    crossinline block: (String) -> T?
): T {
    waitForNodeWithTag(nodeTag)
    onNodeWithTag(nodeTag).assume(isEnabled())
        .fetchSemanticsNode().let { node ->
            node.config[SemanticsProperties.ContentDescription].any { description ->
                block(description)?.let { result ->
                    // Return the T value if block returns non-null.
                    return@checkComponentContentDescriptionState result
                } ?: false
            }
            throw AssertionError("Unable to determine state from quick settingz")
        }
}

inline fun <reified T> ComposeTestRule.checkComponentStateDescriptionState(
    nodeTag: String,
    crossinline block: (String) -> T?
): T {
    waitForNodeWithTag(nodeTag)
    onNodeWithTag(nodeTag).assume(isEnabled())
        .fetchSemanticsNode().let { node ->
            block(node.config[SemanticsProperties.StateDescription])?.let { result ->
                // Return the T value if block returns non-null.
                return@checkComponentStateDescriptionState result
            } ?: false
            throw AssertionError("Unable to determine state from component")
        }
}

fun ComposeTestRule.isHdrEnabled(): Boolean =
    checkComponentContentDescriptionState<Boolean>(QUICK_SETTINGS_HDR_BUTTON) { description ->
        when (description) {
            getResString(R.string.quick_settings_dynamic_range_hdr_description) -> {
                return@checkComponentContentDescriptionState true
            }

            getResString(R.string.quick_settings_dynamic_range_sdr_description) -> {
                return@checkComponentContentDescriptionState false
            }

            else -> null
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

fun ComposeTestRule.getConcurrentState(): ConcurrentCameraMode = visitQuickSettings {
    onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
        .assertExists()
        .fetchSemanticsNode(
            "Concurrent camera button is not visible when expected."
        ).let { node ->
            node.config[SemanticsProperties.ContentDescription].any { description ->
                when (description) {
                    getResString(R.string.quick_settings_description_concurrent_camera_off) -> {
                        return@let ConcurrentCameraMode.OFF
                    }

                    getResString(
                        R.string.quick_settings_description_concurrent_camera_dual
                    ) ->
                        return@let ConcurrentCameraMode.DUAL

                    else -> false
                }
            }
            throw AssertionError(
                "Unable to determine concurrent camera mode from quick settings"
            )
        }
}

fun ComposeTestRule.getCurrentCaptureMode(): CaptureMode = visitQuickSettings {
    waitUntil(timeoutMillis = 1000) {
        onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE).isDisplayed()
    }
    onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE).fetchSemanticsNode(
        "Capture mode button is not visible when expected."
    ).let { node ->
        node.config[SemanticsProperties.ContentDescription].any { description ->
            // check description is one of the capture modes
            when (description) {
                getResString(R.string.quick_settings_description_capture_mode_standard) ->
                    return@let CaptureMode.STANDARD

                getResString(R.string.quick_settings_description_capture_mode_image_only) ->
                    return@let CaptureMode.IMAGE_ONLY

                getResString(R.string.quick_settings_description_capture_mode_video_only) ->
                    return@let CaptureMode.VIDEO_ONLY

                else -> false
            }
        }
        throw (AssertionError("unable to determine capture mode from quick settings"))
    }
}

// ////////////////////////////
//
// Settings Interactions
//
// ////////////////////////////

/**
 * Interface to ensure settings screen utility functions are only called from SettingsScreenScope
 */
interface SettingsScreenScope : ComposeTestRule

/**
 * Navigates to quick settings if not already there and perform action from provided block.
 * This will return from quick settings if not already there, or remain on quick settings if there.
 */
inline fun <T> ComposeTestRule.visitSettingsScreen(
    crossinline block: SettingsScreenScope.() -> T
): T {
    var needReturnFromSettings = false
    onNodeWithTag(SETTINGS_BUTTON).apply {
        if (isDisplayed()) {
            performClick()
            needReturnFromSettings = true
        }
    }

    onNodeWithTag(SETTINGS_TITLE).assertExists(
        "Settings can only be entered from PreviewScreen or Settings screen"
    )

    try {
        with(object : SettingsScreenScope, ComposeTestRule by this {}) {
            return block()
        }
    } finally {
        if (needReturnFromSettings) {
            onNodeWithTag(BACK_BUTTON)
                .assertExists()
                .performClick()

            waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                onNodeWithTag(SETTINGS_TITLE).isNotDisplayed()
            }
        }
    }
}

/**
 * Selects the supplied lens facing from the settings screen
 */
fun SettingsScreenScope.selectLensFacing(lensFacing: LensFacing) {
    onNodeWithTag(BTN_SWITCH_SETTING_LENS_FACING_TAG)
        .assertExists()
        .apply {
            if (!isDisplayed()) {
                performScrollTo()

                waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                    isDisplayed()
                }
            }

            val expectedContentDescription = when (lensFacing) {
                LensFacing.FRONT -> getResString(
                    SettingsR.string.default_facing_camera_description_front
                )
                LensFacing.BACK -> getResString(
                    SettingsR.string.default_facing_camera_description_back
                )
            }
            if (!hasStateDescription(expectedContentDescription).matches(fetchSemanticsNode())) {
                assume(isEnabled()) { "$lensFacing is not selectable" }
                performClick()
            }

            waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                hasStateDescription(expectedContentDescription).matches(fetchSemanticsNode())
            }
        }
}

/**
 * Navigates to a dialog from the Settings Screen
 */
inline fun <T> SettingsScreenScope.visitSettingDialog(
    settingTestTag: String,
    dialogTestTag: String,
    disabledMessage: String? = null,
    crossinline block: ComposeTestRule.() -> T
): T {
    onNodeWithTag(settingTestTag)
        .assertExists()
        .apply {
            if (!isDisplayed()) {
                performScrollTo()

                waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                    isDisplayed()
                }
            }

            assume(isEnabled()) { disabledMessage ?: "Setting $settingTestTag is not enabled" }
            performClick()
        }

    onNodeWithTag(dialogTestTag).assertExists(
        "Opening setting with tag $settingTestTag did not cause dialog with tag $dialogTestTag to open"
    )

    try {
        return block()
    } finally {
        onNodeWithTag(CLOSE_BUTTON)
            .assertExists()
            .performClick()

        waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
            onNodeWithTag(dialogTestTag).isNotDisplayed()
        }
    }
}

// ////////////////////////////
//
// Quick Settings Interaction
//
// ////////////////////////////

/**
 * Navigates to quick settings if not already there and perform action from provided block.
 * This will return from quick settings if not already there, or remain on quick settings if there.
 */
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

            waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                onNodeWithTag(QUICK_SETTINGS_BACKGROUND_MAIN).isNotDisplayed()
            }
        }
    }
}

/**
 * closes expanded quick setting if open to return to main quick settings menu
 */
fun ComposeTestRule.unFocusQuickSetting() {
    // this will click the center of the composable... which may coincide with another composable.
    // so we offset click input out of the way
    onNodeWithTag(QUICK_SETTINGS_BACKGROUND_FOCUSED)
        .assertExists()
        .assertHasClickAction()
        .performTouchInput { down(centerLeft) }
        .performTouchInput { up() }

    this
        .waitUntil(timeoutMillis = 2_000) {
            onNodeWithTag(QUICK_SETTINGS_BACKGROUND_MAIN).isDisplayed()
        }
}

// ////////////////////////////
//
// Apply Quick Settings
//
// ////////////////////////////
fun ComposeTestRule.setHdrEnabled(enabled: Boolean) {
    visitQuickSettings {
        if (isHdrEnabled() != enabled) {
            onNodeWithTag(QUICK_SETTINGS_HDR_BUTTON)
                .assume(isEnabled()) { "Device does not support HDR." }
                .performClick()
        }
        waitUntil(1000) { isHdrEnabled() == enabled }
    }
}

fun ComposeTestRule.setConcurrentCameraMode(concurrentMode: ConcurrentCameraMode) {
    visitQuickSettings {
        waitForNodeWithTag(tag = QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
        onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
            .assume(isEnabled()) { "Device does not support concurrent camera." }
            .let {
                if (getConcurrentState() != concurrentMode) {
                    it.assertExists().performClick()
                }
            }
        waitUntil(1_000) { getConcurrentState() == concurrentMode }
    }
}

fun ComposeTestRule.setCaptureMode(captureMode: CaptureMode) {
    visitQuickSettings {
        waitUntil(timeoutMillis = 1000) {
            onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE).isDisplayed()
        }
        // check that current capture mode != given capture mode
        if (getCurrentCaptureMode() != captureMode) {
            val optionButtonTag = when (captureMode) {
                CaptureMode.STANDARD -> BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
                CaptureMode.IMAGE_ONLY -> BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY
                CaptureMode.VIDEO_ONLY -> BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_VIDEO_ONLY
            }
            // focus setting
            onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE).assertExists()
                .assume(isEnabled())
                .performClick()

            waitUntil(timeoutMillis = 1_000) {
                onNodeWithTag(optionButtonTag).isDisplayed()
            }

            // click option button
            onNodeWithTag(optionButtonTag).assertExists().performClick()

            unFocusQuickSetting()
        }
        if (getCurrentCaptureMode() != captureMode) {
            throw AssertionError("Unable to set capture mode to $captureMode")
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
