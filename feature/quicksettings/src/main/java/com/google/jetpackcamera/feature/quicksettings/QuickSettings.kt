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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.google.jetpackcamera.quicksettings.R

@Composable
fun QuickSettings(modifier: Modifier = Modifier) {
    var isOpen by remember {
        mutableStateOf(false)
    }
    val backgroundColor = animateColorAsState(
        targetValue = Color.Black.copy(alpha = if (isOpen) 0.7f else 0f),
        label = "backgroundColorAnimation"
    )

    val contentAlpha = animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f, label = "contentAlphaAnimation"
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
                    .clickable { isOpen = !isOpen }
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
            ExpandedQuickSettings(
                modifier = Modifier
                    .wrapContentSize()
            )
        }
    }
}

@Composable
private fun ExpandedQuickSettings(modifier: Modifier = Modifier) {
    Column(modifier = Modifier.wrapContentSize()) {
        Row(modifier = Modifier.wrapContentSize()) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_timer_off_72),
                    contentDescription = stringResource(R.string.quick_settings_dropdown_description),
                    tint = Color.White,
                    modifier = Modifier
                        .size(60.dp)
                )

                Text(text = "TIMER OFF", color = Color.White)
            }
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_aspect_ratio_72),
                    contentDescription = stringResource(R.string.quick_settings_dropdown_description),
                    tint = Color.White,
                    modifier = Modifier
                        .size(60.dp)
                )

                Text(text = "16:9", color = Color.White)
            }
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_flash_off_72),
                    contentDescription = stringResource(R.string.quick_settings_dropdown_description),
                    tint = Color.White,
                    modifier = Modifier
                        .size(60.dp)
                )

                Text(text = "FLASH OFF", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(170.dp))
    }
}