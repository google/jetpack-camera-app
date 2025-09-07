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
package com.google.jetpackcamera.ui.components.capture.debug

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_CAMERA_PROPERTIES_TAG
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_TEST_PATTERN_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_VIDEO_RESOLUTION_TAG
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_COLOR_BARS_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_COLOR_BARS_FADE_TO_GRAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_CUSTOM1_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_DIALOG_CLOSE_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_OFF_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_PN9_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_BLACK_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_BLUE_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_DARK_GRAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_GRAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_GREEN_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_RED_BUTTON
import com.google.jetpackcamera.ui.components.capture.TEST_PATTERN_SOLID_COLOR_WHITE_BUTTON
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import kotlin.math.abs

private const val TAG = "DebugOverlayComponents"

@Composable
fun DebugOverlayToggleButton(modifier: Modifier = Modifier, toggleIsOpen: () -> Unit) {
    TextButton(modifier = modifier.testTag(DEBUG_OVERLAY_BUTTON), onClick = { toggleIsOpen() }) {
        Text(text = "Debug")
    }
}

@Composable
fun DebugOverlayComponent(
    modifier: Modifier = Modifier,
    onChangeZoomRatio: (Float) -> Unit,
    onSetTestPattern: (TestPattern) -> Unit,
    toggleIsOpen: () -> Unit,
    debugUiState: DebugUiState.Open
) {
    var selectedDialog by remember { mutableStateOf(SelectedDialog.None) }
    val backgroundColor = Color.Black.copy(
        alpha =
        when (selectedDialog) {
            SelectedDialog.None,
            SelectedDialog.SetTestPattern -> 0.7f
            else -> 0.9f
        }
    )

    BackHandler(onBack = { toggleIsOpen() })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .safeContentPadding()
    ) {
        AnimatedContent(
            targetState = selectedDialog,
            transitionSpec = { fadeIn() togetherWith fadeOut() using null }
        ) { dialog ->
            when (dialog) {
                SelectedDialog.None ->
                    MainDebugOverlay(
                        debugUiState,
                        onMoveToComponent = { selectedDialog = it },
                        onClose = { toggleIsOpen() }
                    )

                SelectedDialog.CameraJSON ->
                    CameraPropertiesJSONDialog(debugUiState.cameraPropertiesJSON) {
                        selectedDialog = SelectedDialog.None
                    }

                SelectedDialog.SetZoom ->
                    SetZoomRatioDialog(onChangeZoomRatio) {
                        selectedDialog = SelectedDialog.None
                    }

                SelectedDialog.SetTestPattern ->
                    SetTestPatternDialog(
                        onSetTestPattern = onSetTestPattern,
                        selectedTestPattern = debugUiState.selectedTestPattern,
                        availableTestPatterns = debugUiState.availableTestPatterns,
                        onClose = { selectedDialog = SelectedDialog.None }
                    )
            }
        }
    }
}

@Composable
private fun MainDebugOverlay(
    debugUiState: DebugUiState.Open,
    onMoveToComponent: (SelectedDialog) -> Unit,
    onClose: () -> Unit
) {
    // Buttons
    Column(
        modifier = Modifier.fillMaxSize()
            .noIndicationClickable(onClick = onClose),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON
            ),
            onClick = {
                onMoveToComponent(SelectedDialog.CameraJSON)
            }
        ) {
            Text(text = "Show Camera Properties JSON")
        }

        Row {
            Text("Video resolution: ")
            val size = debugUiState.videoResolution
            val videoResText = if (size == null) {
                "null"
            } else {
                abs(size.height).toString() + "x" + abs(size.width).toString()
            }
            Text(
                modifier = Modifier.testTag(
                    DEBUG_OVERLAY_VIDEO_RESOLUTION_TAG
                ),
                text = videoResText
            )
        }

        TextButton(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
            ),
            onClick = {
                onMoveToComponent(SelectedDialog.SetZoom)
            }
        ) {
            Text(text = "Set Zoom Ratio")
        }

        TextButton(
            modifier = Modifier.testTag(DEBUG_OVERLAY_SET_TEST_PATTERN_BUTTON),
            enabled = debugUiState.availableTestPatterns.size > 1,
            onClick = {
                onMoveToComponent(SelectedDialog.SetTestPattern)
            }
        ) {
            Text(text = "Set Test Pattern")
        }
    }
}

@Composable
private fun CameraPropertiesJSONDialog(cameraPropertiesJSON: String, onClose: () -> Unit) {
    BackHandler(onBack = { onClose() })
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .noIndicationClickable(onClick = onClose)
    ) {
        Text(
            modifier = Modifier.testTag(DEBUG_OVERLAY_CAMERA_PROPERTIES_TAG),
            text = cameraPropertiesJSON,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SetZoomRatioDialog(onChangeZoomRatio: (Float) -> Unit, onClose: () -> Unit) {
    val zoomRatioText = remember { mutableStateOf("") }
    BackHandler(onBack = { onClose() })
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .noIndicationClickable(onClick = onClose)
    ) {
        Text(text = "Enter and confirm zoom ratio (Absolute not relative)")
        TextField(
            modifier = Modifier.testTag(DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD),
            value = zoomRatioText.value,
            onValueChange = { zoomRatioText.value = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        TextButton(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
            ),
            onClick = {
                try {
                    val newRatio = if (zoomRatioText.value.isEmpty()) {
                        1f
                    } else {
                        zoomRatioText.value.toFloat()
                    }
                    onChangeZoomRatio(newRatio)
                } catch (_: NumberFormatException) {
                    Log.d(TAG, "Zoom ratio should be a float")
                }
                onClose()
            }
        ) {
            Text(text = "Set")
        }
    }
}

@Composable
private fun SetTestPatternDialog(
    onSetTestPattern: (TestPattern) -> Unit,
    selectedTestPattern: TestPattern,
    availableTestPatterns: Set<TestPattern>,
    onClose: () -> Unit
) {
    BackHandler(onBack = { onClose() })
    Column(
        modifier = Modifier.fillMaxSize()
            .noIndicationClickable(onClick = onClose)
    ) {
        Text(text = "Select test pattern")
        val sortedTestPatterns = remember(availableTestPatterns) {
            availableTestPatterns.sortedBy { if (it == TestPattern.Off) "" else it.toString() }
        }
        LazyColumn {
            items(sortedTestPatterns) { testPattern ->
                FilledIconToggleButton(
                    modifier = Modifier.fillMaxWidth()
                        .testTag(getTestPatternTag(testPattern)),
                    checked = testPattern == selectedTestPattern,
                    onCheckedChange = { selected ->
                        if (selected) {
                            onSetTestPattern(testPattern)
                        }
                    }
                ) {
                    Text("$testPattern")
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            TextButton(
                modifier = Modifier.testTag(TEST_PATTERN_DIALOG_CLOSE_BUTTON),
                onClick = { onClose() }
            ) {
                Text(text = "Close")
            }
        }
    }
}

private fun getTestPatternTag(testPattern: TestPattern): String {
    return when (testPattern) {
        TestPattern.Off -> TEST_PATTERN_OFF_BUTTON
        TestPattern.ColorBars -> TEST_PATTERN_COLOR_BARS_BUTTON
        TestPattern.ColorBarsFadeToGray -> TEST_PATTERN_COLOR_BARS_FADE_TO_GRAY_BUTTON
        TestPattern.PN9 -> TEST_PATTERN_PN9_BUTTON
        TestPattern.Custom1 -> TEST_PATTERN_CUSTOM1_BUTTON
        is TestPattern.SolidColor -> when (testPattern) {
            TestPattern.SolidColor.RED -> TEST_PATTERN_SOLID_COLOR_RED_BUTTON
            TestPattern.SolidColor.GREEN -> TEST_PATTERN_SOLID_COLOR_GREEN_BUTTON
            TestPattern.SolidColor.BLUE -> TEST_PATTERN_SOLID_COLOR_BLUE_BUTTON
            TestPattern.SolidColor.WHITE -> TEST_PATTERN_SOLID_COLOR_WHITE_BUTTON
            TestPattern.SolidColor.BLACK -> TEST_PATTERN_SOLID_COLOR_BLACK_BUTTON
            TestPattern.SolidColor.GRAY -> TEST_PATTERN_SOLID_COLOR_GRAY_BUTTON
            TestPattern.SolidColor.DARK_GRAY -> TEST_PATTERN_SOLID_COLOR_DARK_GRAY_BUTTON
            else -> ""
        }
    }
}

@Composable
private fun Modifier.noIndicationClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)

private enum class SelectedDialog {
    None,
    CameraJSON,
    SetZoom,
    SetTestPattern
}
