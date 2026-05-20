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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState

// --- Standard Mode ---

@PreviewTest
@Preview
@Composable
fun IdleStandardCaptureButtonScreenshotPreview() {
    IdleStandardCaptureButtonPreview()
}

@PreviewTest
@Preview
@Composable
fun IdleStandardCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        IdleStandardCaptureButtonPreview()
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledStandardCaptureButtonScreenshotPreview() {
    IdleStandardCaptureButtonDisabledPreview()
}

@PreviewTest
@Preview
@Composable
fun DisabledStandardCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        IdleStandardCaptureButtonDisabledPreview()
    }
}

@PreviewTest
@Preview
@Composable
fun PressedStandardCaptureButtonScreenshotPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Gray, Color.DarkGray)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CaptureButtonRing(
                captureButtonSize = 76f,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size((76f * 0.93f).dp)
                        .clip(CircleShape)
                        .background(Color.White)
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
        MaterialTheme(colorScheme = darkColorScheme()) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Gray, Color.DarkGray)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CaptureButtonRing(
                    captureButtonSize = 76f,
                    color = Color.White
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size((76f * 0.93f).dp)
                            .clip(CircleShape)
                            .background(Color.White)
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
    IdleImageCaptureButtonPreview()
}

@PreviewTest
@Preview
@Composable
fun IdleImageCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        IdleImageCaptureButtonPreview()
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledImageCaptureButtonScreenshotPreview() {
    IdleImageCaptureButtonDisabledPreview()
}

@PreviewTest
@Preview
@Composable
fun DisabledImageCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        IdleImageCaptureButtonDisabledPreview()
    }
}

@PreviewTest
@Preview
@Composable
fun PressedImageCaptureButtonScreenshotPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Gray, Color.DarkGray)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CaptureButtonRing(
                captureButtonSize = 76f,
                color = Color.Transparent
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size((76f * 0.93f).dp)
                        .clip(CircleShape)
                        .background(Color.White)
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
        MaterialTheme(colorScheme = darkColorScheme()) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Gray, Color.DarkGray)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CaptureButtonRing(
                    captureButtonSize = 76f,
                    color = Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size((76f * 0.93f).dp)
                            .clip(CircleShape)
                            .background(Color.White)
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
    IdleVideoOnlyCaptureButtonPreview()
}

@PreviewTest
@Preview
@Composable
fun IdleVideoOnlyCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        IdleVideoOnlyCaptureButtonPreview()
    }
}

@PreviewTest
@Preview
@Composable
fun DisabledVideoOnlyCaptureButtonScreenshotPreview() {
    IdleVideoOnlyCaptureButtonDisabledPreview()
}

@PreviewTest
@Preview
@Composable
fun DisabledVideoOnlyCaptureButtonBlack60ScreenshotPreview() {
    CompositionLocalProvider(LocalShutterBackgroundStyle provides ShutterBackgroundStyle.BLACK_60) {
        IdleVideoOnlyCaptureButtonDisabledPreview()
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
    LockedRecordingPreview()
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
