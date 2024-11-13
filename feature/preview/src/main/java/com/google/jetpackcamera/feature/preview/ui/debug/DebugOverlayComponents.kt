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
package com.google.jetpackcamera.feature.preview.ui.debug

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.ui.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.feature.preview.ui.DEBUG_OVERLAY_CAMERA_PROPERTIES_TAG
import com.google.jetpackcamera.feature.preview.ui.DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
import com.google.jetpackcamera.feature.preview.ui.DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
import com.google.jetpackcamera.feature.preview.ui.DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD
import com.google.jetpackcamera.feature.preview.ui.DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON

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
    onChangeZoomScale: (Float) -> Unit,
    toggleIsOpen: () -> Unit,
    previewUiState: PreviewUiState.Ready
) {
    val isOpen = previewUiState.debugUiState.isDebugMode &&
        previewUiState.debugUiState.isDebugOverlayOpen
    val backgroundColor =
        animateColorAsState(
            targetValue = Color.Black.copy(alpha = if (isOpen) 0.7f else 0f),
            label = "backgroundColorAnimation"
        )

    val contentAlpha =
        animateFloatAsState(
            targetValue = if (isOpen) 1f else 0f,
            label = "contentAlphaAnimation",
            animationSpec = tween()
        )

    val zoomRatioDialog = remember { mutableStateOf(false) }
    val cameraPropertiesJSONDialog = remember { mutableStateOf(false) }

    if (isOpen) {
        BackHandler(onBack = { toggleIsOpen() })

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = backgroundColor.value)
                .alpha(alpha = contentAlpha.value)
                .clickable(onClick = { toggleIsOpen() })
        ) {
            // Buttons
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    modifier = Modifier.testTag(
                        DEBUG_OVERLAY_SHOW_CAMERA_PROPERTIES_BUTTON
                    ),
                    onClick = {
                        cameraPropertiesJSONDialog.value = true
                    }
                ) {
                    Text(text = "Show Camera Properties JSON")
                }
                TextButton(
                    modifier = Modifier.testTag(
                        DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
                    ),
                    onClick = {
                        zoomRatioDialog.value = true
                    }
                ) {
                    Text(text = "Set Zoom Ratio")
                }
            }

            // Openable contents
            // Show Camera properties
            if (cameraPropertiesJSONDialog.value) {
                CameraPropertiesJSONComponent(previewUiState) {
                    cameraPropertiesJSONDialog.value = false
                }
            }

            // Set zoom ratio
            if (zoomRatioDialog.value) {
                SetZoomRatioComponent(previewUiState, onChangeZoomScale) {
                    zoomRatioDialog.value = false
                }
            }
        }
    }
}

@Composable
private fun CameraPropertiesJSONComponent(
    previewUiState: PreviewUiState.Ready,
    onClose: () -> Unit
) {
    BackHandler(onBack = { onClose() })
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .background(color = Color.Black)
    ) {
        Text(
            modifier = Modifier.testTag(DEBUG_OVERLAY_CAMERA_PROPERTIES_TAG),
            text = previewUiState.debugUiState.cameraPropertiesJSON,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SetZoomRatioComponent(
    previewUiState: PreviewUiState.Ready,
    onChangeZoomScale: (Float) -> Unit,
    onClose: () -> Unit
) {
    var zoomRatioText = remember { mutableStateOf("") }
    BackHandler(onBack = { onClose() })
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = scrollState)
            .background(color = Color.Black)
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
                    val relativeRatio = if (zoomRatioText.value.isEmpty()) {
                        1f
                    } else {
                        zoomRatioText.value.toFloat()
                    }
                    val currentRatio = previewUiState.zoomScale
                    val absoluteRatio = relativeRatio / currentRatio
                    onChangeZoomScale(absoluteRatio)
                } catch (e: NumberFormatException) {
                    Log.d(TAG, "Zoom ratio should be a float")
                }
                onClose()
            }
        ) {
            Text(text = "Set")
        }
    }
}
