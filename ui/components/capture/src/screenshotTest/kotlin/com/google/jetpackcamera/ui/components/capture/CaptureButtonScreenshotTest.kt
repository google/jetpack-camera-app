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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState

// --- Standard Mode ---

@PreviewTest
@Preview
@Composable
fun IdleStandardCaptureButtonScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
    )
}

@PreviewTest
@Preview
@Composable
fun IdleStandardCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
        )
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledStandardCaptureButtonScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
            CaptureMode.STANDARD,
            isEnabled = false
        )
    )
}

@PreviewTest
@Preview
@Composable
fun DisabledStandardCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
                CaptureMode.STANDARD,
                isEnabled = false
            )
        )
    }
}

@PreviewTest
@Preview
@Composable
fun PressedStandardCaptureButtonScreenshotPreview() {
    androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.DarkGray)
                    )
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CaptureButtonRing(
                captureButtonSize = 76f,
                color = androidx.compose.ui.graphics.Color.White
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = androidx.compose.ui.Modifier
                        .size((76f * 0.93f).dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White)
                ) {}
            }
        }
    }
}

@PreviewTest
@Preview
@Composable
fun PressedStandardCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.DarkGray)
                        )
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CaptureButtonRing(
                    captureButtonSize = 76f,
                    color = androidx.compose.ui.graphics.Color.White
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = androidx.compose.ui.Modifier
                            .size((76f * 0.93f).dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White)
                    ) {}
                }
            }
        }
    }
}

// --- Image Only Mode ---

@PreviewTest
@Preview
@Composable
fun IdleImageCaptureButtonScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
    )
}

@PreviewTest
@Preview
@Composable
fun IdleImageCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
        )
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledImageCaptureButtonScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
            CaptureMode.IMAGE_ONLY,
            isEnabled = false
        )
    )
}

@PreviewTest
@Preview
@Composable
fun DisabledImageCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
                CaptureMode.IMAGE_ONLY,
                isEnabled = false
            )
        )
    }
}

@PreviewTest
@Preview
@Composable
fun PressedImageCaptureButtonScreenshotPreview() {
    androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.DarkGray)
                    )
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CaptureButtonRing(
                captureButtonSize = 76f,
                color = androidx.compose.ui.graphics.Color.Transparent
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                    modifier = androidx.compose.ui.Modifier
                        .size((76f * 0.93f).dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White)
                ) {}
            }
        }
    }
}

@PreviewTest
@Preview
@Composable
fun PressedImageCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.DarkGray)
                        )
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CaptureButtonRing(
                    captureButtonSize = 76f,
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = androidx.compose.ui.Modifier
                            .size((76f * 0.93f).dp)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White)
                    ) {}
                }
            }
        }
    }
}

// --- Video Only Mode ---

@PreviewTest
@Preview
@Composable
fun IdleVideoOnlyCaptureButtonScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
    )
}

@PreviewTest
@Preview
@Composable
fun IdleVideoOnlyCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
        )
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledVideoOnlyCaptureButtonScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
            CaptureMode.VIDEO_ONLY,
            isEnabled = false
        )
    )
}

@PreviewTest
@Preview
@Composable
fun DisabledVideoOnlyCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
                CaptureMode.VIDEO_ONLY,
                isEnabled = false
            )
        )
    }
}

// --- Recording States ---

@PreviewTest
@Preview
@Composable
fun PressedRecordingScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording
    )
}

@PreviewTest
@Preview
@Composable
fun LockedRecordingScreenshotPreview() {
    PreviewCaptureButton(
        captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
    )
}

@PreviewTest
@Preview
@Composable
fun PressedRecordingBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording
        )
    }
}

@PreviewTest
@Preview
@Composable
fun LockedRecordingBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        PreviewCaptureButton(
            captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
        )
    }
}
