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

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getResString(@StringRes resId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TestPreviewLayout(
        modifier: Modifier = Modifier,
        scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Hidden,
                skipHiddenState = false
            )
        ),
        onDismissQuickSettings: () -> Unit = {},
        captureButton: @Composable (Modifier) -> Unit = {},
        quickSettingsOverlay: @Composable (Modifier) -> Unit = {},
        viewfinder: @Composable (Modifier) -> Unit = {}
    ) {
        PreviewLayout(
            modifier = modifier,
            scaffoldState = scaffoldState,
            onDismissQuickSettings = onDismissQuickSettings,
            viewfinder = viewfinder,
            captureButton = captureButton,
            imageWell = {},
            flipCameraButton = {},
            zoomLevelDisplay = {},
            elapsedTimeDisplay = {},
            quickSettingsButton = {},
            indicatorRow = {},
            captureModeToggle = {},
            quickSettingsOverlay = quickSettingsOverlay,
            debugOverlay = {},
            debugVisibilityWrapper = { it() },
            screenFlashOverlay = {},
            snackBar = { _, _ -> }
        )
    }

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
            TestPreviewLayout(scaffoldState = scaffoldState)
        }

        val targetDescription = getResString(
            R.string.quick_settings_btn_close_expanded_settings_description
        )

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE)
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
            )
            .assert(
                SemanticsMatcher("onClickLabel equals $targetDescription") { node ->
                    node.config.getOrNull(SemanticsActions.OnClick)?.label == targetDescription
                }
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
            TestPreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE).performClick()
        assertThat(onDismissCalled).isTrue()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_scrim_displayedWhenExpanded_andClickCallsDismiss() {
        var onDismissCalled = false

        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.Expanded,
                    skipHiddenState = false
                )
            )
            TestPreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertIsDisplayed()
            .performClick()

        assertThat(onDismissCalled).isTrue()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_scrim_interceptsTouchEventsFromUnderlyingControls() {
        var underlyingViewfinderClicked = false
        var onDismissCalled = false

        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.Expanded,
                    skipHiddenState = false
                )
            )
            TestPreviewLayout(
                scaffoldState = scaffoldState,
                onDismissQuickSettings = { onDismissCalled = true },
                viewfinder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { underlyingViewfinderClicked = true }
                    )
                }
            )
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).performClick()

        assertThat(underlyingViewfinderClicked).isFalse()
        assertThat(onDismissCalled).isTrue()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_scrim_doesNotExistWhenHidden() {
        composeTestRule.setContent {
            val scaffoldState = rememberBottomSheetScaffoldState(
                bottomSheetState = rememberStandardBottomSheetState(
                    initialValue = SheetValue.Hidden,
                    skipHiddenState = false
                )
            )
            TestPreviewLayout(scaffoldState = scaffoldState)
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
