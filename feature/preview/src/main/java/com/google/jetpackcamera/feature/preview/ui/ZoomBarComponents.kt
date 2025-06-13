/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.jetpackcamera.feature.preview.ui

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.feature.preview.ZoomControlUiState
import java.math.RoundingMode
import java.text.DecimalFormat


@Composable
fun ZoomButtonRow(
    modifier: Modifier = Modifier,
    zoomControlUiState: ZoomControlUiState.Enabled,
    buttonSize: Dp = 55.dp,
    spacing: Dp = 8.dp,
    onChangeZoom: (Float) -> Unit
) {

    val selectedOptionIndex: Int by remember(zoomControlUiState)
    {
        // if animating towards a value, then that option will be selected
        // otherwise, select the closest option that is less than the current zoom ratio
        val checkValue =
            zoomControlUiState.animatingToValue ?: zoomControlUiState.primaryZoomRatio ?: 1f
        Log.d("checkvalue", "checking... $checkValue")
        derivedStateOf {
            if (checkValue >= 1f)
            // -1 if no index is found
                zoomControlUiState.zoomLevels.indexOfLast { zoomLevelOption ->
                    checkValue >= zoomLevelOption
                }
            else
                0
        }
    }

    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.32f),
                shape = RoundedCornerShape(buttonSize)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                //    .padding(horizontal = spacing)
                .height(intrinsicSize = IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomControlUiState.zoomLevels.forEachIndexed { index, value ->
                ZoomButton(
                    targetZoom = value,
                    currentZoomRatio = { -> (zoomControlUiState.primaryZoomRatio ?: 1f) },
                    isSelected = selectedOptionIndex == index,
                    onChangeZoom = onChangeZoom
                )
            }
        }
    }
}

@Composable
private fun ZoomButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 55.dp,
    targetZoom: Float,
    currentZoomRatio: () -> Float,
    isSelected: Boolean = false,
    onChangeZoom: (Float) -> Unit,
) {
    val selectedFormat = DecimalFormat("#.0")
    val formatter = DecimalFormat("#.#")
    formatter.minimumIntegerDigits = 0
    if (targetZoom >= 1)
        formatter.roundingMode = RoundingMode.DOWN
    else
        formatter.roundingMode = RoundingMode.UP
    val displayText by remember(isSelected, currentZoomRatio) {
        derivedStateOf {
            if (!isSelected)
                formatter.format(targetZoom)
            else
                "${selectedFormat.format(currentZoomRatio())}x"
        }
    }

    Box(
        modifier = Modifier.width(buttonSize * 1.45f),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { onChangeZoom(targetZoom) },
            modifier = modifier
                .height(buttonSize)
                .defaultMinSize(minWidth = buttonSize),
            shape = CircleShape,
            colors = if (isSelected)
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            else ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .16f))
        ) {
            Text(
                modifier = Modifier.animateContentSize(),
                text = displayText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ZoomButtonPreview() {
    Box(
        Modifier
            .padding(16.dp)
            .background(Color.DarkGray)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row {
            ZoomButton(
                targetZoom = 1f,
                onChangeZoom = {},
                isSelected = false,
                currentZoomRatio = { -> 2f }
            )

            ZoomButton(
                targetZoom = 3f,
                onChangeZoom = {},
                currentZoomRatio = { -> 3.3f },
                isSelected = true,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ZoomBarPreviewOneSelected() {
    val sampleValues = listOf(0.5f, 1.0f, 2.0f, 5.0f)
    Box(
        Modifier
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        ZoomButtonRow(
            zoomControlUiState = ZoomControlUiState.Enabled(sampleValues, primaryZoomRatio = 1f),
            onChangeZoom = {
                // In a real app, you would handle the click event here.
                // For the preview, we can just print it.
                println("Clicked value: $it")
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ZoomBarPreviewFractionSelected() {
    val sampleValues = listOf(0.5f, 1.0f, 2.0f, 5.0f, 25.0f)
    Box(
        Modifier
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        ZoomButtonRow(
            zoomControlUiState = ZoomControlUiState.Enabled(sampleValues, primaryZoomRatio = .6f),
            onChangeZoom = {
                // In a real app, you would handle the click event here.
                // For the preview, we can just print it.
                println("Clicked value: $it")
            }
        )
    }
}