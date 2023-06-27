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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.quicksettings.R
import kotlin.math.min

/**
 * The UI component for quick settings.
 */
@Composable
fun QuickSettingsScreen(
    modifier: Modifier = Modifier,
    lensFace: CameraLensFace = CameraLensFace.FRONT,
    flashMode: CameraFlashMode = CameraFlashMode.OFF,
    aspectRatio: CameraAspectRatio = CameraAspectRatio.NINE_SIXTEEN,
    timer: CameraTimer = CameraTimer.OFF,
    availableLensFace: List<CameraLensFace> = CameraLensFace.values().asList(),
    availableFlashMode: List<CameraFlashMode> = CameraFlashMode.values().asList(),
    availableAspectRatio: List<CameraAspectRatio> = CameraAspectRatio.values().asList(),
    availableTimer: List<CameraTimer> = CameraTimer.values().asList(),
    onLensFaceClick: (lensFace: CameraLensFace) -> Unit,
    onFlashModeClick: (flashMode: CameraFlashMode) -> Unit,
    onAspectRatioClick: (aspectRatio: CameraAspectRatio) -> Unit,
    onTimerClick: (timer: CameraTimer) -> Unit
) {
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

        if (isOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha = contentAlpha.value),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpandedQuickSettingsUi(
                    lensFace = lensFace,
                    flashMode = flashMode,
                    aspectRatio = aspectRatio,
                    timer = timer,
                    availableLensFace = availableLensFace,
                    availableFlashMode = availableFlashMode,
                    availableAspectRatio = availableAspectRatio,
                    availableTimer = availableTimer,
                    onLensFaceClick = onLensFaceClick,
                    onFlashModeClick = onFlashModeClick,
                    onAspectRatioClick = onAspectRatioClick,
                    onTimerClick = onTimerClick,
                    close = { isOpen = false })
            }
        }
    }
}

/**
 * The UI component for quick settings when it is expanded.
 */
@Composable
private fun ExpandedQuickSettingsUi(
    lensFace: CameraLensFace,
    flashMode: CameraFlashMode,
    aspectRatio: CameraAspectRatio,
    timer: CameraTimer,
    availableLensFace: List<CameraLensFace>,
    availableFlashMode: List<CameraFlashMode>,
    availableAspectRatio: List<CameraAspectRatio>,
    availableTimer: List<CameraTimer>,
    onLensFaceClick: (lensFace: CameraLensFace) -> Unit,
    onFlashModeClick: (flashMode: CameraFlashMode) -> Unit,
    onAspectRatioClick: (aspectRatio: CameraAspectRatio) -> Unit,
    onTimerClick: (timer: CameraTimer) -> Unit,
    close: () -> Unit
) {
    var selectedUiModel by remember {
        mutableStateOf<QuickSettingsUiModel?>(null)
    }
    val quickSettingsUiModels: List<QuickSettingsUiModel> = getQuickSettingsUiModelList(
        lensFace = lensFace,
        flashMode = flashMode,
        aspectRatio = aspectRatio,
        timer = timer,
        availableLensFace = availableLensFace,
        availableFlashMode = availableFlashMode,
        availableAspectRatio = availableAspectRatio,
        availableTimer = availableTimer,
        onLensFaceClick = onLensFaceClick,
        onFlashModeClick = onFlashModeClick,
        onAspectRatioClick = onAspectRatioClick,
        onTimerClick = onTimerClick
    )
    val initialNumOfColumns =
        min(
            quickSettingsUiModels.size,
            ((LocalConfiguration.current.screenWidthDp.dp - (dimensionResource(
                id = R.dimen.quick_settings_ui_horizontal_padding
            ) * 2)) /
                    (dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                            (dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2))).toInt()
        )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.quick_settings_ui_horizontal_padding))
    ) {
        if (selectedUiModel == null) {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Fixed(count = initialNumOfColumns)
            ) {
                items(items = quickSettingsUiModels) { item ->
                    QuickSettingUiItem(
                        modifier = Modifier.clickable(onClick = {
                            selectedUiModel = item
                        }),
                        drawableResId = item.currentQuickSettingEnum.getDrawableResId(),
                        textRes = item.currentQuickSettingEnum.getTextResId(),
                        descriptionRes = item.currentQuickSettingEnum.getDescriptionResId(),
                        isHighLighted = false
                    )

                }
            }
        } else {
            val expandedNumOfColumns =
                min(
                    selectedUiModel!!.availableQuickSettingsEnums.size,
                    ((LocalConfiguration.current.screenWidthDp.dp - (dimensionResource(
                        id = R.dimen.quick_settings_ui_horizontal_padding
                    ) * 2)) /
                            (dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                                    (dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2))).toInt()
                )
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth(),
                columns = GridCells.Fixed(count = expandedNumOfColumns)
            ) {
                itemsIndexed(items = selectedUiModel!!.availableQuickSettingsEnums) { i, _ ->
                    QuickSettingUiItem(
                        modifier = Modifier.clickable(
                            onClick = {
                                selectedUiModel!!.onClick(selectedUiModel!!.availableQuickSettingsEnums[i])
                                close()
                            }
                        ),
                        drawableResId = selectedUiModel!!.availableQuickSettingsEnums[i].getDrawableResId(),
                        textRes = selectedUiModel!!.availableQuickSettingsEnums[i].getTextResId(),
                        descriptionRes = selectedUiModel!!.availableQuickSettingsEnums[i].getDescriptionResId(),
                        isHighLighted = selectedUiModel!!.availableQuickSettingsEnums[i] == selectedUiModel!!.currentQuickSettingEnum
                    )

                }
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.quick_settings_spacer_height)))
    }
}

/**
 * The itemized UI component representing each button in quick settings.
 */
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
            .padding(dimensionResource(id = R.dimen.quick_settings_ui_item_padding)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (isHighLighted) Color.Yellow else Color.White
        Icon(
            painter = painterResource(drawableResId),
            contentDescription = stringResource(descriptionRes),
            tint = tint,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size))
        )

        Text(text = stringResource(textRes), color = tint)
    }
}