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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.round

/**
 * A composable that displays a row of zoom buttons.
 *
 * @param onChangeZoom the callback for when the zoom changes
 * @param modifier the modifier for this component
 * @param zoomControlUiState the [ZoomControlUiState] for this component
 * @param buttonSize the size of the buttons
 * @param spacing the spacing between the buttons
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZoomButtonRow(
    onChangeZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
    zoomControlUiState: ZoomControlUiState,
    buttonSize: Dp = ButtonDefaults.ExtraSmallContainerHeight,
    spacing: Dp = 16.dp
) {
    if (zoomControlUiState is ZoomControlUiState.Enabled) {
        val currentZoomState by rememberUpdatedState(zoomControlUiState)
        val selectedOptionIndex: Int by remember(zoomControlUiState.primaryLensFacing) {
            // todo(kc): checkValue will flash to 1.0 when flipping between camera lenses on API 30+.
            // cameraState (cameraState's zoom value) should have an intermediate, "unknown", state when
            // camera flip is in progress. Then we can ensure that the zoom controls don't flash to 1.0
            // when cameraState is in this intermediate state.

            derivedStateOf {
                // if animating towards a value, then that option will be selected
                // otherwise, select the closest option that is less than the current zoom ratio

                val checkValue =
                    currentZoomState.animatingToValue ?: currentZoomState.primaryZoomRatio ?: 1f
                val roundedCheckValue = round(checkValue * 10f) / 10f
                val index = currentZoomState.zoomLevels.indexOfLast { zoomLevelOption ->
                    val roundedOption = round(zoomLevelOption * 10f) / 10f
                    roundedCheckValue >= roundedOption
                }
                if (index == -1 && currentZoomState.zoomLevels.isNotEmpty()) {
                    0
                } else {
                    index
                }
            }
        }

        Row(
            modifier = modifier
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics {
                    testTag = ZOOM_BUTTON_ROW_TAG
                    stateDescription = zoomControlUiState.primaryZoomRatio.toString()
                }
                .height(buttonSize + 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomControlUiState.zoomLevels.forEachIndexed { index, value ->
                Box(
                    modifier = Modifier.width(buttonSize + spacing + 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomButton(
                        targetZoom = value,
                        zoomRatio = zoomControlUiState.primaryZoomRatio ?: 1f,
                        isSelected = selectedOptionIndex == index,
                        onChangeZoom = onChangeZoom
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ZoomButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = ButtonDefaults.ExtraSmallContainerHeight,
    targetZoom: Float,
    zoomRatio: Float,
    isSelected: Boolean = false,
    onChangeZoom: (Float) -> Unit
) {
    val contentDescriptionSelected = stringResource(id = R.string.zoom_button_selected)
    val contentDescriptionNotSelected = stringResource(id = R.string.zoom_button_not_selected)
    val selectedFormat = remember { DecimalFormat("#.0") }
    val formatter = remember(targetZoom) {
        DecimalFormat("#.#").apply {
            minimumIntegerDigits = 1
            roundingMode = RoundingMode.HALF_UP
        }
    }
    val displayText by remember(isSelected, zoomRatio) {
        derivedStateOf {
            if (!isSelected) {
                formatter.format(targetZoom)
            } else {
                "${selectedFormat.format(zoomRatio)}x"
            }
        }
    }
    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onChangeZoom(targetZoom) },
        modifier = modifier
            .height(buttonSize)
            .widthIn(min = buttonSize + 16.dp)
            .semantics {
                testTag = getZoomButtonTestTag(targetZoom)
                contentDescription = if (isSelected) {
                    contentDescriptionSelected
                } else {
                    contentDescriptionNotSelected
                }
            },
        shapes = ToggleButtonDefaults.shapesFor(buttonSize),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        colors = if (isSelected) {
            ToggleButtonDefaults.toggleButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryFixed,
                contentColor = MaterialTheme.colorScheme.onPrimaryFixed
            )
        } else {
            ToggleButtonDefaults.toggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        }
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFeatureSettings = "tnum"
            ),
            textAlign = TextAlign.Center
        )
    }
}

private fun getZoomButtonTestTag(buttonValue: Float): String {
    return if (buttonValue < 1 && buttonValue > 0) {
        ZOOM_BUTTON_MIN_TAG
    } else if (buttonValue == 1f) {
        ZOOM_BUTTON_1_TAG
    } else if (buttonValue == 2f) {
        ZOOM_BUTTON_2_TAG
    } else if (buttonValue == 5f) {
        ZOOM_BUTTON_5_TAG
    } else {
        TODO("Zoom button with value $buttonValue needs a test tag")
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
                zoomRatio = 2f
            )

            ZoomButton(
                targetZoom = 3f,
                onChangeZoom = {},
                zoomRatio = 3.3f,
                isSelected = true
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
            zoomControlUiState = ZoomControlUiState.Enabled(
                sampleValues,
                primaryLensFacing = LensFacing.FRONT,
                primaryZoomRatio = 1f
            ),
            onChangeZoom = {
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
            zoomControlUiState = ZoomControlUiState.Enabled(
                sampleValues,
                primaryLensFacing = LensFacing.FRONT,
                primaryZoomRatio = .6f
            ),
            onChangeZoom = {
                println("Clicked value: $it")
            }
        )
    }
}
