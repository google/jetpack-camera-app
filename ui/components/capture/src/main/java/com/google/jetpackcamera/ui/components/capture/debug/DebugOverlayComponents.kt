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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.ui.components.capture.BTN_DEBUG_HIDE_COMPONENTS_TAG
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_CAMERA_PROPERTIES_TAG
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_VIDEO_RESOLUTION_TAG
import com.google.jetpackcamera.ui.components.capture.LOGICAL_CAMERA_ID_TAG
import com.google.jetpackcamera.ui.components.capture.PHYSICAL_CAMERA_ID_TAG
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ZOOM_RATIO_TAG
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import kotlin.math.abs

private const val TAG = "DebugOverlayComponents"

@Composable
fun DebugDialogContainerToggle(toggleIsOpen: () -> Unit, modifier: Modifier = Modifier) {
    Button(modifier = modifier.testTag(DEBUG_OVERLAY_BUTTON), onClick = { toggleIsOpen() }) {
        Text(text = stringResource(R.string.debug_overlay_toggle_btn_text))
    }
}

@Composable
private fun LogicalCameraIdText(logicalCameraId: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DebugTextBar(
            title = stringResource(R.string.debug_text_logical_camera_id_prefix),
            value = logicalCameraId ?: "---",
            tag = LOGICAL_CAMERA_ID_TAG
        )
    }
}

@Composable
private fun PhysicalCameraIdText(physicalCameraId: String?) {
    DebugTextBar(
        title = stringResource(R.string.debug_text_physical_camera_id_prefix),
        value = physicalCameraId ?: "---",
        tag = PHYSICAL_CAMERA_ID_TAG
    )
}

@Composable
private fun ZoomRatioText(modifier: Modifier = Modifier, primaryZoomRatio: Float?) {
    DebugTextBar(
        modifier = modifier,
        title = "Zoom Ratio: ",
        value = stringResource(id = R.string.zoom_ratio_text, primaryZoomRatio ?: 1f),
        tag = ZOOM_RATIO_TAG
    )
}

@Composable
private fun DebugTextBar(modifier: Modifier = Modifier, title: String, value: String, tag: String) {
    Row(modifier = modifier) {
        Text(modifier = modifier.background(Color.Black.copy(alpha = .7f)), text = title)
        Text(
            modifier = modifier
                .background(Color.Black.copy(alpha = .4f))
                .testTag(tag),
            text = value
        )
    }
}

@Composable
private fun ToggleVisibilityButton(
    onToggleHidingComponents: () -> Unit,
    isHidingComponents: Boolean
) {
    val stateDescption = if (isHidingComponents) {
        stringResource(id = R.string.debug_hide_components_desc)
    } else {
        stringResource(R.string.debug_show_components_desc)
    }

    IconButton(
        modifier = Modifier
            .safeDrawingPadding()
            .semantics {
                testTag = BTN_DEBUG_HIDE_COMPONENTS_TAG
                stateDescription = stateDescption
            },
        onClick = { onToggleHidingComponents() }
    ) {
        if (isHidingComponents) {
            Icon(Icons.Default.VisibilityOff, contentDescription = null)
        } else {
            Icon(Icons.Default.Visibility, contentDescription = null)
        }
    }
}

@Composable
fun DebugOverlay(
    modifier: Modifier = Modifier,
    onChangeZoomRatio: (Float) -> Unit,
    onSetTestPattern: (TestPattern) -> Unit,
    toggleIsOpen: () -> Unit,
    onToggleHidingComponents: () -> Unit,
    debugUiState: DebugUiState.Enabled,
    vararg extraControls: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            ToggleVisibilityButton(
                onToggleHidingComponents = onToggleHidingComponents,
                isHidingComponents = debugUiState.debugHidingComponents
            )
            if (!debugUiState.debugHidingComponents) {
                DebugConsole(
                    modifier = Modifier.padding(top = 100.dp),
                    debugUiState = debugUiState,
                    onToggleDebugOverlay = toggleIsOpen,
                    extraControls = extraControls
                )
            }
        }
        (debugUiState as? DebugUiState.Enabled.Open)?.let {
            if (!debugUiState.debugHidingComponents) {
                DebugDialogContainer(
                    modifier = Modifier,
                    onChangeZoomRatio = onChangeZoomRatio,
                    onSetTestPattern = onSetTestPattern,
                    toggleIsOpen = toggleIsOpen,
                    debugUiState = it
                )
            }
        }
    }
}

/**
 * A row of components visible at the top of the debug screen.
 * The first button will always be the [DebugDialogContainerToggle], followed by any components passed
 * into [extraControls].
 *
 * @param debugUiState  the current [DebugUiState.Enabled]
 * @param onToggleDebugOverlay a callback to open and hide the [DebugDialogContainer]
 * @param extraControls additional composable functions to be displayed in the debug top row.
 * These should NOT include components intended to be exclusive to the debug screen.
 */
@Composable
private fun DebugConsole(
    debugUiState: DebugUiState.Enabled,
    onToggleDebugOverlay: () -> Unit,
    vararg extraControls: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        FlowRow(
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            // debug menu button
            DebugDialogContainerToggle(toggleIsOpen = onToggleDebugOverlay)
            extraControls.forEach { it() }
        }
        LogicalCameraIdText(debugUiState.currentLogicalCameraId)
        PhysicalCameraIdText(debugUiState.currentPhysicalCameraId)
        ZoomRatioText(
            modifier = Modifier,
            primaryZoomRatio = debugUiState.currentPrimaryZoomRatio
        )
    }
}

// Debug Dialogs

@Composable
private fun DebugDialogContainer(
    modifier: Modifier = Modifier,
    onChangeZoomRatio: (Float) -> Unit,
    onSetTestPattern: (TestPattern) -> Unit,
    toggleIsOpen: () -> Unit,
    debugUiState: DebugUiState.Enabled.Open
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
    ) {
        AnimatedContent(
            modifier = Modifier.safeContentPadding(),
            targetState = selectedDialog,
            transitionSpec = { fadeIn() togetherWith fadeOut() using null }
        ) { dialog ->
            when (dialog) {
                SelectedDialog.None ->
                    DebugDialogOptionsMenuDialog(
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
private fun DebugDialogOptionsMenuDialog(
    debugUiState: DebugUiState.Enabled.Open,
    onMoveToComponent: (SelectedDialog) -> Unit,
    onClose: () -> Unit
) {
    // Buttons
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .noIndicationClickable(onClick = onClose),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        // show camera properties json button
        Button(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON
            ),
            onClick = {
                onMoveToComponent(SelectedDialog.CameraJSON)
            }
        ) {
            Text(text = "Show Camera Properties JSON")
        }

        // set zoom ratio
        Button(
            modifier = Modifier.testTag(
                DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
            ),
            onClick = {
                onMoveToComponent(SelectedDialog.SetZoom)
            }
        ) {
            Text(text = "Set Zoom Ratio")
        }

        // set test pattern
        Button(
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .noIndicationClickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Enter and confirm zoom ratio")
            TextField(
                modifier = Modifier.testTag(DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD),
                value = zoomRatioText.value,
                onValueChange = { zoomRatioText.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                modifier = Modifier.testTag(
                    DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
                ),
                onClick = {
                    try {
                        // no-op if confirmed with empty entry
                        if (zoomRatioText.value.isEmpty()) {
                            onClose()
                        } else {
                            val newRatio = zoomRatioText.value.toFloat()
                            onChangeZoomRatio(newRatio)
                        }
                    } catch (_: NumberFormatException) {
                        Log.d(TAG, "Zoom ratio should be a float")
                    }
                    onClose()
                }
            ) {
                Text(text = "Confirm")
            }
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
        modifier = Modifier
            .fillMaxSize()
            .noIndicationClickable(onClick = onClose)
    ) {
        Text(text = "Select test pattern")
        val sortedTestPatterns = remember(availableTestPatterns) {
            availableTestPatterns.sortedBy { if (it == TestPattern.Off) "" else it.toString() }
        }
        LazyColumn {
            items(sortedTestPatterns) { testPattern ->
                FilledIconToggleButton(
                    modifier = Modifier.fillMaxWidth(),
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
                onClick = { onClose() }
            ) {
                Text(text = "Close")
            }
        }
    }
}

@Composable
private fun Modifier.noIndicationClickable(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = null,
    indication = null,
    onClick = onClick
)

private enum class SelectedDialog {
    None,
    CameraJSON,
    SetZoom,
    SetTestPattern
}
