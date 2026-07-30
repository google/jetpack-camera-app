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
package com.google.jetpackcamera.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * An icon button that shows a styled RichTooltip when clicked.
 * The tooltip is persistent and stays visible until the user taps outside.
 *
 * @param icon The icon to display inside the button.
 * @param tooltipTitle The optional title to display inside the tooltip.
 * @param tooltipText The text to display inside the tooltip.
 * @param modifier Modifier for the button container.
 * @param isOutlined Whether the icon button should be outlined.
 * @param onClick Optional callback when the button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    tooltipTitle: String? = null,
    tooltipText: String,
    isOutlined: Boolean = false,
    onClick: () -> Unit = {}
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above
        ),
        tooltip = {
            RichTooltip(
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 296.dp),
                title = tooltipTitle?.let {
                    {
                        Text(
                            text = it,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        )
                    }
                },
                text = {
                    Text(
                        text = tooltipText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = if (tooltipTitle != null) 0.dp else 10.dp,
                                start = 10.dp,
                                end = 10.dp,
                                bottom = 10.dp
                            )
                    )
                },
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryFixed,
                    contentColor = MaterialTheme.colorScheme.onSecondaryFixed,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryFixed
                ),
                caretShape = TooltipDefaults.caretShape()
            )
        },
        state = tooltipState,
        modifier = modifier
    ) {
        val showTooltipAction = remember(tooltipState, scope, onClick) {
            {
                scope.launch {
                    tooltipState.show()
                }
                onClick()
            }
        }

        if (isOutlined) {
            OutlinedIconButton(
                onClick = showTooltipAction,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                icon()
            }
        } else {
            IconButton(onClick = showTooltipAction) {
                icon()
            }
        }
    }
}
