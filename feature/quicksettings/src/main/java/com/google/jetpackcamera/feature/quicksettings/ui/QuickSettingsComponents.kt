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

package com.google.jetpackcamera.feature.quicksettings.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.feature.quicksettings.CameraAspectRatio
import com.google.jetpackcamera.feature.quicksettings.CameraFlashMode
import com.google.jetpackcamera.feature.quicksettings.CameraLensFace
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsEnum
import com.google.jetpackcamera.quicksettings.R
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.FlashMode
import kotlin.math.min


// completed components ready to go into preview screen

@Composable
fun ExpandedQuickSetRatio(
    setRatio: (aspectRatio: AspectRatio) -> Unit,
    currentRatio: AspectRatio
) {
    val buttons: Array<@Composable () -> Unit> = arrayOf(
        {
            QuickSetRatio(
                onClick = { setRatio(AspectRatio.THREE_FOUR) },
                ratio = AspectRatio.THREE_FOUR,
                currentRatio = currentRatio,
                isHighlightEnabled = true
            )
        },
        {
            QuickSetRatio(
                onClick = { setRatio(AspectRatio.NINE_SIXTEEN) },
                ratio = AspectRatio.NINE_SIXTEEN,
                currentRatio = currentRatio,
                isHighlightEnabled = true
            )
        },
        {
            QuickSetRatio(
                onClick = { setRatio(AspectRatio.ONE_ONE) },
                ratio = AspectRatio.ONE_ONE,
                currentRatio = currentRatio,
                isHighlightEnabled = true
            )
        }
    )
    ExpandedQuickSetting(quickSettingButtons = buttons)
}

@Composable
fun QuickSetRatio(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    ratio: AspectRatio,
    currentRatio: AspectRatio,
    isHighlightEnabled: Boolean = false
) {
    val enum = when (ratio) {
        AspectRatio.THREE_FOUR -> CameraAspectRatio.THREE_FOUR
        AspectRatio.NINE_SIXTEEN -> CameraAspectRatio.NINE_SIXTEEN
        AspectRatio.ONE_ONE -> CameraAspectRatio.ONE_ONE
        else -> CameraAspectRatio.ONE_ONE
    }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = { onClick() },
        isHighLighted = isHighlightEnabled && (ratio == currentRatio)
    )
}

@Composable
fun QuickSetFlash(
    modifier: Modifier = Modifier,
    onClick: (FlashMode) -> Unit, currentFlashMode: FlashMode
) {
    val enum = when (currentFlashMode) {
        FlashMode.OFF -> CameraFlashMode.OFF
        FlashMode.AUTO -> CameraFlashMode.AUTO
        FlashMode.ON -> CameraFlashMode.ON
    }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        isHighLighted = currentFlashMode == FlashMode.ON,
        onClick =
        {
            when (currentFlashMode) {
                FlashMode.OFF -> onClick(FlashMode.ON)
                FlashMode.ON -> onClick(FlashMode.AUTO)
                FlashMode.AUTO -> onClick(FlashMode.OFF)
            }
        }
    )
}

@Composable
fun QuickFlipCamera(
    modifier: Modifier = Modifier,
    flipCamera: (Boolean) -> Unit,
    currentFacingFront: Boolean
) {
    val enum = when (currentFacingFront) {
        true -> CameraLensFace.FRONT
        false -> CameraLensFace.BACK
    }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = { flipCamera(!currentFacingFront) }
    )
}

@Composable
fun DropDownIcon(
    modifier: Modifier = Modifier,
    toggleDropDown: () -> Unit,
    isOpen: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        //dropdown icon
        Icon(
            painter = painterResource(R.drawable.baseline_expand_more_72),
            contentDescription = stringResource(R.string.quick_settings_dropdown_description),
            tint = Color.White,
            modifier = Modifier
                .size(72.dp)
                .clickable {
                    toggleDropDown()
                }
                .scale(1f, if (isOpen) -1f else 1f)
        )
    }
}

// subcomponents used to build completed components

@Composable
fun QuickSettingUiItem(
    modifier: Modifier = Modifier,
    enum: QuickSettingsEnum,
    onClick: () -> Unit,
    isHighLighted: Boolean = false
) {
    QuickSettingUiItem(
        modifier = modifier,
        drawableResId = enum.getDrawableResId(),
        text = stringResource(id = enum.getTextResId()),
        accessibilityText = stringResource(id = enum.getDescriptionResId()),
        onClick = { onClick() },
        isHighLighted = isHighLighted,
    )
}

/**
 * The itemized UI component representing each button in quick settings.
 */
@Composable
fun QuickSettingUiItem(
    modifier: Modifier = Modifier,
    @DrawableRes drawableResId: Int,
    text: String,
    accessibilityText: String,
    onClick: () -> Unit,
    isHighLighted: Boolean = false
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(dimensionResource(id = R.dimen.quick_settings_ui_item_padding))
            .clickable {
                onClick()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (isHighLighted) Color.Yellow else Color.White
        Icon(
            painter = painterResource(drawableResId),
            contentDescription = accessibilityText,
            tint = tint,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size))
        )

        Text(text = text, color = tint)
    }
}

/**
 * Should you want to have an expanded view of a single quick setting
 */
@Composable
fun ExpandedQuickSetting(
    modifier: Modifier = Modifier,
    vararg quickSettingButtons: @Composable () -> Unit
) {
    val expandedNumOfColumns =
        min(
            quickSettingButtons.size,
            ((LocalConfiguration.current.screenWidthDp.dp - (dimensionResource(
                id = R.dimen.quick_settings_ui_horizontal_padding
            ) * 2)) /
                    (dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                            (dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2))).toInt()
        )
    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(count = expandedNumOfColumns)
    ) {
        items(quickSettingButtons.size) { i ->
            quickSettingButtons[i]()
        }
    }
}

/**
 * Algorithm to determine dimensions of QuickSettings Icon layout
 */
@Composable
fun QuickSettingsGrid(
    modifier: Modifier = Modifier,
    vararg quickSettingsButtons: @Composable () -> Unit
) {
    val initialNumOfColumns =
        min(
            quickSettingsButtons.size,
            ((LocalConfiguration.current.screenWidthDp.dp - (dimensionResource(
                id = R.dimen.quick_settings_ui_horizontal_padding
            ) * 2)) /
                    (dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                            (dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2))).toInt()
        )

    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(count = initialNumOfColumns)
    ) {
        items(quickSettingsButtons.size) { i ->
            quickSettingsButtons[i]()
        }
    }
}
