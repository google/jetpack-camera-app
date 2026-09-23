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

import android.view.KeyEvent
import android.view.View
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class CaptureButtonVolumeInterceptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val idleUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
    private val dummyView = View(ApplicationProvider.getApplicationContext())

    @Test
    fun captureKeyEventListener_whenEnabled_dispatchesVolumeEvents() {
        var pressedSource: CaptureSource? = null
        var releasedSource: CaptureSource? = null

        val listener = CaptureKeyEventListener(
            isKeyEventsEnabled = { true },
            onPress = { pressedSource = it },
            onRelease = { releasedSource = it }
        )

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)
        val downHandled = listener.onUnhandledKeyEvent(dummyView, downEvent)
        assertThat(downHandled).isTrue()
        assertThat(pressedSource).isEqualTo(CaptureSource.VOLUME_DOWN)

        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
        val upHandled = listener.onUnhandledKeyEvent(dummyView, upEvent)
        assertThat(upHandled).isTrue()
        assertThat(releasedSource).isEqualTo(CaptureSource.VOLUME_DOWN)
    }

    @Test
    fun captureKeyEventListener_volumeUp_whenEnabled_dispatchesVolumeUpSource() {
        var pressedSource: CaptureSource? = null
        var releasedSource: CaptureSource? = null

        val listener = CaptureKeyEventListener(
            isKeyEventsEnabled = { true },
            onPress = { pressedSource = it },
            onRelease = { releasedSource = it }
        )

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val downHandled = listener.onUnhandledKeyEvent(dummyView, downEvent)
        assertThat(downHandled).isTrue()
        assertThat(pressedSource).isEqualTo(CaptureSource.VOLUME_UP)

        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)
        val upHandled = listener.onUnhandledKeyEvent(dummyView, upEvent)
        assertThat(upHandled).isTrue()
        assertThat(releasedSource).isEqualTo(CaptureSource.VOLUME_UP)
    }

    @Test
    fun captureKeyEventListener_whenDisabled_doesNotHandleVolumeEventsAndResetsKeyState() {
        var pressedSource: CaptureSource? = null
        var releasedSource: CaptureSource? = null

        val listener = CaptureKeyEventListener(
            isKeyEventsEnabled = { false },
            onPress = { pressedSource = it },
            onRelease = { releasedSource = it }
        )

        // Volume Down: not consumed (returns false), no capture triggered
        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)
        val downHandled = listener.onUnhandledKeyEvent(dummyView, downEvent)
        assertThat(downHandled).isFalse()
        assertThat(pressedSource).isNull()

        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
        val upHandled = listener.onUnhandledKeyEvent(dummyView, upEvent)
        assertThat(upHandled).isFalse()
        assertThat(releasedSource).isNull()

        // Volume Up: not consumed (returns false), no capture triggered
        val upDownEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val upDownHandled = listener.onUnhandledKeyEvent(dummyView, upDownEvent)
        assertThat(upDownHandled).isFalse()
        assertThat(pressedSource).isNull()

        val upUpEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)
        val upUpHandled = listener.onUnhandledKeyEvent(dummyView, upUpEvent)
        assertThat(upUpHandled).isFalse()
        assertThat(releasedSource).isNull()
    }

    @Test
    fun captureKeyEventListener_repeatedActionDown_isDeduplicated() {
        var pressCount = 0

        val listener = CaptureKeyEventListener(
            isKeyEventsEnabled = { true },
            onPress = { pressCount++ },
            onRelease = {}
        )

        val downEvent1 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)
        val downEvent2 = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)

        val handled1 = listener.onUnhandledKeyEvent(dummyView, downEvent1)
        val handled2 = listener.onUnhandledKeyEvent(dummyView, downEvent2)

        assertThat(handled1).isTrue()
        assertThat(handled2).isTrue()
        // Only 1 press fired because repeated down is deduplicated
        assertThat(pressCount).isEqualTo(1)
    }

    @Test
    fun captureKeyEventListener_nonVolumeKey_isNotIntercepted() {
        val listener = CaptureKeyEventListener(
            isKeyEventsEnabled = { false },
            onPress = {},
            onRelease = {}
        )

        val aEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
        val handled = listener.onUnhandledKeyEvent(dummyView, aEvent)
        assertThat(handled).isFalse()
    }

    @Test
    fun captureButton_rendersCleanlyWithVolumeCaptureConfiguration() {
        composeTestRule.setContent {
            CaptureButton(
                captureButtonUiState = idleUiState,
                isVolumeCaptureEnabled = false
            )
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun captureButton_whenQuickSettingsOpen_derivesDisabledStateFromLocalBottomSheetState() {
        var sheetState: CameraBottomSheetState? = null

        composeTestRule.setContent {
            val state = rememberCameraBottomSheetState()
            sheetState = state

            CompositionLocalProvider(LocalCameraBottomSheetState provides state) {
                BottomSheetScaffold(
                    scaffoldState = state.scaffoldState,
                    sheetPeekHeight = 0.dp,
                    sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                    sheetContent = { Text("Quick Settings Content") },
                    content = {
                        CaptureButton(
                            captureButtonUiState = idleUiState
                        )
                    }
                )
            }
        }

        assertThat(sheetState).isNotNull()
        assertThat(sheetState!!.isOpen).isFalse()

        var pressed = false
        val listener = CaptureKeyEventListener(
            isKeyEventsEnabled = { !sheetState!!.isOpen && !sheetState!!.isVisible },
            onPress = { pressed = true },
            onRelease = {}
        )

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)

        // When closed, volume events are handled
        val handledWhenClosed = listener.onUnhandledKeyEvent(dummyView, downEvent)
        assertThat(handledWhenClosed).isTrue()
        assertThat(pressed).isTrue()

        // Reset
        pressed = false
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
        listener.onUnhandledKeyEvent(dummyView, upEvent)

        // Expand sheet
        composeTestRule.runOnUiThread {
            sheetState!!.expand()
        }
        composeTestRule.waitForIdle()

        assertThat(sheetState!!.isOpen).isTrue()
        assertThat(sheetState!!.isVisible).isTrue()

        // When open, volume events are not handled and not consumed
        val handledWhenOpen = listener.onUnhandledKeyEvent(dummyView, downEvent)
        assertThat(handledWhenOpen).isFalse()
        assertThat(pressed).isFalse()
    }
}
