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

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class CameraBottomSheetStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rememberCameraBottomSheetScaffoldState_defaultsToHidden() {
        var state: CameraBottomSheetState? = null

        composeTestRule.setContent {
            val scaffoldState = rememberCameraBottomSheetScaffoldState()
            state = rememberCameraBottomSheetState(scaffoldState = scaffoldState)
        }

        assertThat(state).isNotNull()
        assertThat(state!!.scaffoldState.bottomSheetState.currentValue).isEqualTo(SheetValue.Hidden)
        assertThat(state!!.scaffoldState.bottomSheetState.targetValue).isEqualTo(SheetValue.Hidden)
        assertThat(state!!.isOpen).isFalse()
        assertThat(state!!.isVisible).isFalse()
    }

    @Test
    fun cameraBottomSheetState_expandAndHide_updatesState() {
        var state: CameraBottomSheetState? = null

        composeTestRule.setContent {
            val sheetState = rememberCameraBottomSheetState()
            state = sheetState
            BottomSheetScaffold(
                scaffoldState = sheetState.scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = { Text("Sheet Content") },
                content = { Text("Main Content") }
            )
        }

        assertThat(state).isNotNull()
        assertThat(state!!.isOpen).isFalse()

        composeTestRule.runOnUiThread {
            state!!.expand()
        }
        composeTestRule.waitForIdle()

        assertThat(state!!.isOpen).isTrue()
        assertThat(state!!.isVisible).isTrue()

        composeTestRule.runOnUiThread {
            state!!.hide()
        }
        composeTestRule.waitForIdle()

        assertThat(state!!.isOpen).isFalse()
        assertThat(state!!.isVisible).isFalse()
    }

    @Test
    fun cameraBottomSheetState_toggle_switchesState() {
        var state: CameraBottomSheetState? = null

        composeTestRule.setContent {
            val sheetState = rememberCameraBottomSheetState()
            state = sheetState
            BottomSheetScaffold(
                scaffoldState = sheetState.scaffoldState,
                sheetPeekHeight = 0.dp,
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = { Text("Sheet Content") },
                content = { Text("Main Content") }
            )
        }

        assertThat(state).isNotNull()
        assertThat(state!!.isOpen).isFalse()

        // Toggle from hidden to open
        composeTestRule.runOnUiThread {
            state!!.toggle()
        }
        composeTestRule.waitForIdle()

        assertThat(state!!.isOpen).isTrue()
        assertThat(state!!.isVisible).isTrue()

        // Toggle from open to hidden
        composeTestRule.runOnUiThread {
            state!!.toggle()
        }
        composeTestRule.waitForIdle()

        assertThat(state!!.isOpen).isFalse()
        assertThat(state!!.isVisible).isFalse()
    }

    @Test
    fun localCameraBottomSheetState_defaultsToNull_andProvidesValueWhenProvided() {
        var defaultReadExecuted = false
        var defaultLocalState: CameraBottomSheetState? = null
        var createdStateInstance: CameraBottomSheetState? = null
        var providedLocalState: CameraBottomSheetState? = null

        composeTestRule.setContent {
            defaultLocalState = LocalCameraBottomSheetState.current
            defaultReadExecuted = true
            val createdState = rememberCameraBottomSheetState()
            createdStateInstance = createdState
            CompositionLocalProvider(LocalCameraBottomSheetState provides createdState) {
                providedLocalState = LocalCameraBottomSheetState.current
            }
        }

        assertThat(defaultReadExecuted).isTrue()
        assertThat(defaultLocalState).isNull()
        assertThat(providedLocalState).isNotNull()
        assertThat(providedLocalState).isSameInstanceAs(createdStateInstance)
    }
}
