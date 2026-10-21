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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val TOOLTIP_ANCHOR_BUTTON_TAG = "tooltip_anchor_button"
const val TOOLTIP_CONTENT_TAG = "tooltip_content"

/**
 * Events used to trigger programmatic display or dismissal of tooltips.
 */
sealed interface TooltipTriggerEvent {
    data object Show : TooltipTriggerEvent
    data object Dismiss : TooltipTriggerEvent
}

/**
 * A generic container that wraps content with a styled [RichTooltip].
 *
 * @param tooltipText The text to display inside the tooltip.
 * @param modifier Modifier for the container.
 * @param tooltipTitle Optional title displayed above the tooltip text.
 * @param state State controlling tooltip visibility.
 * @param positioning Anchor position for the tooltip (e.g. Above, Below).
 * @param autoDismissDuration Optional duration after which the tooltip automatically dismisses.
 * @param triggerFlow Optional flow of [TooltipTriggerEvent] to trigger showing or dismissing.
 * @param enableUserInput Whether long-press and hover gestures on the anchor trigger the tooltip.
 * @param onDismissRequest Optional callback invoked when the tooltip is dismissed.
 * @param content The anchor composable wrapped by the tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JcaTooltipBox(
    tooltipText: String,
    modifier: Modifier = Modifier,
    tooltipTitle: String? = null,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    autoDismissDuration: Duration? = null,
    triggerFlow: Flow<TooltipTriggerEvent>? = null,
    enableUserInput: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var triggerRevision by remember { mutableIntStateOf(0) }

    // External flow triggers using collectLatest to prevent deadlock from show() suspension
    if (triggerFlow != null) {
        LaunchedEffect(triggerFlow, state) {
            triggerFlow.collectLatest { event ->
                when (event) {
                    TooltipTriggerEvent.Show -> {
                        triggerRevision++
                        state.show()
                    }
                    TooltipTriggerEvent.Dismiss -> state.dismiss()
                }
            }
        }
    }

    // Auto-dismiss timer keyed on targetState and triggerRevision
    if (autoDismissDuration != null) {
        LaunchedEffect(state.transition.targetState, triggerRevision, autoDismissDuration) {
            if (state.transition.targetState) {
                delay(autoDismissDuration)
                state.dismiss()
                onDismissRequest?.invoke()
            }
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = positioning
        ),
        tooltip = {
            RichTooltip(
                modifier = Modifier
                    .widthIn(min = 140.dp, max = 296.dp)
                    .testTag(TOOLTIP_CONTENT_TAG)
                    .semantics {
                        liveRegion = LiveRegionMode.Assertive
                        paneTitle = tooltipTitle ?: tooltipText
                    },
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
        state = state,
        modifier = modifier,
        enableUserInput = enableUserInput,
        onDismissRequest = onDismissRequest
    ) {
        content()
    }
}

/**
 * An icon button that shows a styled [RichTooltip] when clicked.
 *
 * @param icon The icon to display inside the button.
 * @param tooltipText The text to display inside the tooltip.
 * @param modifier Modifier for the button container.
 * @param tooltipTitle The optional title to display inside the tooltip.
 * @param state State controlling tooltip visibility.
 * @param positioning Anchor position for the tooltip (e.g. Above, Below).
 * @param autoDismissDuration Optional duration after which the tooltip automatically dismisses.
 * @param triggerFlow Optional flow of [TooltipTriggerEvent] to trigger showing or dismissing.
 * @param isOutlined Whether the icon button should be outlined.
 * @param containerColor The background container color for the button.
 * @param contentColor The content/tint color for the icon.
 * @param size The size of the button container, defaulting to [IconButtonDefaults.extraSmallContainerSize] (32.dp).
 * @param onClick Optional callback when the button is clicked.
 * @param onDismissRequest Optional callback invoked when the tooltip is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    tooltipTitle: String? = null,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    autoDismissDuration: Duration? = null,
    triggerFlow: Flow<TooltipTriggerEvent>? = null,
    isOutlined: Boolean = false,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    size: Dp = IconButtonDefaults.extraSmallContainerSize().width,
    onClick: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val toggleTooltip = remember(state, scope, onClick, onDismissRequest) {
        {
            scope.launch {
                if (state.transition.targetState) {
                    state.dismiss()
                    onDismissRequest?.invoke()
                } else {
                    state.show()
                }
            }
            onClick()
        }
    }

    JcaTooltipBox(
        tooltipText = tooltipText,
        modifier = modifier,
        tooltipTitle = tooltipTitle,
        state = state,
        positioning = positioning,
        autoDismissDuration = autoDismissDuration,
        triggerFlow = triggerFlow,
        enableUserInput = false,
        onDismissRequest = onDismissRequest
    ) {
        val buttonModifier = Modifier
            .size(size)
            .testTag(TOOLTIP_ANCHOR_BUTTON_TAG)

        if (isOutlined) {
            OutlinedIconButton(
                onClick = toggleTooltip,
                modifier = buttonModifier,
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                icon()
            }
        } else {
            IconButton(
                onClick = toggleTooltip,
                modifier = buttonModifier,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            ) {
                icon()
            }
        }
    }
}
