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

package com.google.jetpackcamera.feature.quicksettings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.jetpackcamera.quicksettings.R
import com.google.jetpackcamera.settings.SettingsViewModel

@Composable
fun QuickSettingsUi(modifier: Modifier = Modifier) {
    var isOpen by remember {
        mutableStateOf(false)
    }
    val backgroundColor = animateColorAsState(
        targetValue = Color.Black.copy(alpha = if (isOpen) 0.7f else 0f),
        label = "backgroundColorAnimation"
    )

    val contentAlpha = animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f, label = "contentAlphaAnimation",
        animationSpec = tween()
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor.value),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_expand_more_72),
                contentDescription = stringResource(R.string.quick_settings_dropdown_description),
                tint = if (isOpen) Color.White else LocalContentColor.current,
                modifier = Modifier
                    .size(72.dp)
                    .clickable {
                        isOpen = !isOpen
                    }
                    .scale(1f, if (isOpen) -1f else 1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha = contentAlpha.value),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isOpen) {
                ExpandedQuickSettingsUi(
                    close = { isOpen = false })
            }
        }
    }
}

@Composable
private fun ExpandedQuickSettingsUi(
    viewModel: SettingsViewModel = hiltViewModel(),
    close: () -> Unit
) {
    var expandedQuickSettingsDataModel by remember {
        mutableStateOf<ExpandedQuickSettingsDataModel?>(null)
    }
    val settingsUiState by viewModel.settingsUiState.collectAsState()
    Column(
        modifier = Modifier
            .wrapContentSize()
    ) {
        if (expandedQuickSettingsDataModel == null) {
            Row(modifier = Modifier.wrapContentSize()) {
                QuickSettingUiItem(
                    // PreviewUiState dependency
                    modifier = Modifier.clickable(onClick = {
                        expandedQuickSettingsDataModel =
                            ExpandedQuickSettingsDataModel(
                                drawableResIds = listOf(
                                    R.drawable.baseline_cameraswitch_72,
                                    R.drawable.baseline_cameraswitch_72
                                ),
                                textResIds = listOf(
                                    R.string.quick_settings_front_camera_text,
                                    R.string.quick_settings_back_camera_text
                                ),
                                descriptionResIds = listOf(
                                    R.string.quick_settings_front_camera_description,
                                    R.string.quick_settings_back_camera_description
                                ),
                                highlightedIndex =
                                if (settingsUiState.settings.default_front_camera) 0 else 1,
                                onClicks = listOf(
                                    {
                                        if (!settingsUiState.settings.default_front_camera) {
                                            viewModel.setDefaultToFrontCamera()
                                        }
                                        close()
                                    },
                                    {
                                        if (settingsUiState.settings.default_front_camera) {
                                            viewModel.setDefaultToFrontCamera()
                                        }
                                        close()
                                    })
                            )
                    }),
                    drawableResId = R.drawable.baseline_cameraswitch_72,
                    textRes = if (settingsUiState.settings.default_front_camera)
                        R.string.quick_settings_front_camera_text else R.string.quick_settings_back_camera_text,
                    descriptionRes = if (settingsUiState.settings.default_front_camera)
                        R.string.quick_settings_front_camera_description else R.string.quick_settings_back_camera_description,
                    isHighLighted = false
                )
            }
        } else {
            Row(modifier = Modifier.wrapContentSize()) {
                for (i in 0 until expandedQuickSettingsDataModel!!.descriptionResIds.size) {
                    QuickSettingUiItem(
                        modifier = Modifier.clickable(
                            onClick = expandedQuickSettingsDataModel!!.onClicks[i]
                        ),
                        drawableResId = expandedQuickSettingsDataModel!!.drawableResIds[i],
                        textRes = expandedQuickSettingsDataModel!!.textResIds[i],
                        descriptionRes = expandedQuickSettingsDataModel!!.descriptionResIds[i],
                        isHighLighted = i == expandedQuickSettingsDataModel!!.highlightedIndex
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(170.dp))
    }
}

@Composable
private fun QuickSettingUiItem(
    modifier: Modifier,
    @DrawableRes drawableResId: Int,
    @StringRes textRes: Int,
    @StringRes descriptionRes: Int,
    isHighLighted: Boolean
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (isHighLighted) Color.Yellow else Color.White
        Icon(
            painter = painterResource(drawableResId),
            contentDescription = stringResource(descriptionRes),
            tint = tint,
            modifier = Modifier
                .size(60.dp)
        )

        Text(text = stringResource(textRes), color = tint)
    }
}