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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_dragHandle_hasButtonRoleAndAccessibilityLabel() {
        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.Expanded,
                    skipHiddenState = false
                )
            )
            PreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = {},
                viewfinder = {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .testTag("Viewfinder")
                    )
                },
                captureButton = {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("CaptureButton")
                    )
                },
                imageWell = { Box(modifier = Modifier.size(30.dp)) },
                flipCameraButton = { Box(modifier = Modifier.size(30.dp)) },
                zoomLevelDisplay = { Box(modifier = Modifier.size(20.dp)) },
                elapsedTimeDisplay = { Box(modifier = Modifier.size(20.dp)) },
                quickSettingsButton = { Box(modifier = Modifier.size(30.dp)) },
                indicatorRow = { Box(modifier = Modifier.size(20.dp)) },
                captureModeToggle = { Box(modifier = Modifier.size(20.dp)) },
                quickSettingsOverlay = {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .testTag("QuickSettingsOverlay")
                    )
                },
                debugOverlay = {},
                debugVisibilityWrapper = { it() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
            )

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_clickDragHandle_callsOnDismissQuickSettings() {
        var onDismissCalled = false

        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.Expanded,
                    skipHiddenState = false
                )
            )
            PreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true },
                viewfinder = { Box(modifier = Modifier.size(100.dp)) },
                captureButton = { Box(modifier = Modifier.size(50.dp)) },
                imageWell = { Box(modifier = Modifier.size(30.dp)) },
                flipCameraButton = { Box(modifier = Modifier.size(30.dp)) },
                zoomLevelDisplay = { Box(modifier = Modifier.size(20.dp)) },
                elapsedTimeDisplay = { Box(modifier = Modifier.size(20.dp)) },
                quickSettingsButton = { Box(modifier = Modifier.size(30.dp)) },
                indicatorRow = { Box(modifier = Modifier.size(20.dp)) },
                captureModeToggle = { Box(modifier = Modifier.size(20.dp)) },
                quickSettingsOverlay = { Box(modifier = Modifier.size(200.dp)) },
                debugOverlay = {},
                debugVisibilityWrapper = { it() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE).performClick()
        assertThat(onDismissCalled).isTrue()
    }
}
