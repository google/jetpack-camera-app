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

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented window-input tests verifying that real screen-level taps on [TooltipIconButton]
 * cleanly toggle and dismiss the popup without outside-touch reopen races.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class TooltipInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tooltipIconButton_realWindowTapOnAnchor_opensAndClosesTooltip() {
        var anchorCenterOnScreen = Offset.Zero
        var dismissCount = 0

        composeTestRule.setContent {
            MaterialTheme {
                // Center the anchor inside the safe drawing area so injected taps never land on an
                // edge-to-edge system bar or cutout owned by System UI.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    TooltipIconButton(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val pos = coords.positionOnScreen()
                            anchorCenterOnScreen = Offset(
                                x = pos.x + coords.size.width / 2f,
                                y = pos.y + coords.size.height / 2f
                            )
                        },
                        icon = { Box(modifier = Modifier.size(24.dp)) },
                        tooltipText = "Real tap toggle message",
                        onDismissRequest = { dismissCount++ }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()

        // First real window-level tap opens the tooltip
        sendRealScreenTap(anchorCenterOnScreen)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasText("Real tap toggle message") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        assertThat(dismissCount).isEqualTo(0)

        // Second real window-level tap on the anchor triggers Popup outside-touch dismissal on
        // pointer-down and must leave the tooltip closed rather than reopening it on pointer-up,
        // while invoking onDismissRequest exactly once.
        sendRealScreenTap(anchorCenterOnScreen)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
        assertThat(dismissCount).isEqualTo(1)
    }

    private fun sendRealScreenTap(screenPoint: Offset) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(
            downTime,
            downTime,
            MotionEvent.ACTION_DOWN,
            screenPoint.x,
            screenPoint.y,
            0
        ).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        val upEvent = MotionEvent.obtain(
            downTime,
            downTime + 50L,
            MotionEvent.ACTION_UP,
            screenPoint.x,
            screenPoint.y,
            0
        ).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        try {
            instrumentation.sendPointerSync(downEvent)
            instrumentation.sendPointerSync(upEvent)
        } finally {
            downEvent.recycle()
            upEvent.recycle()
        }
    }
}
