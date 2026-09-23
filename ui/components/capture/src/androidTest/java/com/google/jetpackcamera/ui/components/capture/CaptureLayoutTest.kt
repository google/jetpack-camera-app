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
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getResString(@StringRes resId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId)

    @Composable
    private fun TestPreviewLayout(
        modifier: Modifier = Modifier,
        sheetState: CameraBottomSheetState = rememberCameraBottomSheetState(),
        onDismissQuickSettings: () -> Unit = {},
        enableBackHandler: Boolean = true,
        captureButton: @Composable (Modifier) -> Unit = {},
        quickSettingsButton: @Composable (Modifier) -> Unit = {},
        quickSettingsOverlay: @Composable (Modifier) -> Unit = {},
        viewfinder: @Composable (Modifier) -> Unit = {}
    ) {
        CompositionLocalProvider(LocalDisableAnimations provides true) {
            PreviewLayout(
                modifier = modifier,
                sheetState = sheetState,
                onDismissQuickSettings = onDismissQuickSettings,
                enableBackHandler = enableBackHandler,
                viewfinder = viewfinder,
                captureButton = captureButton,
                imageWell = {},
                flipCameraButton = {},
                zoomLevelDisplay = {},
                elapsedTimeDisplay = {},
                quickSettingsButton = quickSettingsButton,
                indicatorRow = {},
                captureModeToggle = {},
                quickSettingsOverlay = quickSettingsOverlay,
                debugOverlay = {},
                debugVisibilityWrapper = { it() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun previewLayout_dragHandle_hasButtonRoleAndAccessibilityLabel() {
        composeTestRule.setContent {
            val sheetState = rememberCameraBottomSheetState(
                initialValue = SheetValue.Expanded
            )
            TestPreviewLayout(sheetState = sheetState)
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
            val sheetState = rememberCameraBottomSheetState(
                initialValue = SheetValue.Expanded
            )
            TestPreviewLayout(
                sheetState = sheetState,
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
            val sheetState = rememberCameraBottomSheetState(
                initialValue = SheetValue.Expanded
            )
            TestPreviewLayout(
                sheetState = sheetState,
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
            val sheetState = rememberCameraBottomSheetState(
                initialValue = SheetValue.Expanded
            )
            TestPreviewLayout(
                sheetState = sheetState,
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
            val sheetState = rememberCameraBottomSheetState(
                initialValue = SheetValue.Hidden
            )
            TestPreviewLayout(sheetState = sheetState)
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewLayout_defaultState_autoDismissesOnDragHandleClick() {
        var sheetStateCaptured: CameraBottomSheetState? = null

        composeTestRule.setContent {
            TestPreviewLayout(
                captureButton = {
                    sheetStateCaptured = LocalCameraBottomSheetState.current
                }
            )
        }

        assertThat(sheetStateCaptured).isNotNull()
        val sheetState = checkNotNull(sheetStateCaptured)
        assertThat(sheetState.isOpen).isFalse()

        // Expand sheet using the internalized state
        composeTestRule.runOnUiThread {
            sheetState.expand()
        }
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isTrue()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Click drag handle - should auto-dismiss via internalized sheetState.hide()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DRAG_HANDLE).performClick()
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isFalse()
        assertThat(sheetState.isVisible).isFalse()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewLayout_defaultState_autoDismissesOnScrimClick() {
        var sheetStateCaptured: CameraBottomSheetState? = null

        composeTestRule.setContent {
            TestPreviewLayout(
                captureButton = {
                    sheetStateCaptured = LocalCameraBottomSheetState.current
                }
            )
        }

        assertThat(sheetStateCaptured).isNotNull()
        val sheetState = checkNotNull(sheetStateCaptured)

        // Expand sheet using the internalized state
        composeTestRule.runOnUiThread {
            sheetState.expand()
        }
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isTrue()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Click scrim - should auto-dismiss via internalized sheetState.hide()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).performClick()
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isFalse()
        assertThat(sheetState.isVisible).isFalse()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewLayout_backPress_whenSheetOpen_hidesSheet() {
        var sheetStateCaptured: CameraBottomSheetState? = null
        var backDispatcher: OnBackPressedDispatcher? = null

        composeTestRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            val sheetState = rememberCameraBottomSheetState()
            sheetStateCaptured = sheetState
            TestPreviewLayout(
                sheetState = sheetState,
                enableBackHandler = true
            )
        }

        assertThat(sheetStateCaptured).isNotNull()
        val sheetState = checkNotNull(sheetStateCaptured)
        composeTestRule.runOnUiThread {
            sheetState.expand()
        }
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isTrue()
        assertThat(sheetState.isVisible).isTrue()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Trigger back handler
        composeTestRule.runOnUiThread {
            backDispatcher?.onBackPressed()
        }
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isFalse()
        assertThat(sheetState.isVisible).isFalse()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun toggleQuickSettingsButton_withDefaultLocalState_togglesSheet() {
        var sheetStateCaptured: CameraBottomSheetState? = null

        composeTestRule.setContent {
            val sheetState = rememberCameraBottomSheetState()
            sheetStateCaptured = sheetState
            TestPreviewLayout(
                sheetState = sheetState,
                quickSettingsButton = { modifier ->
                    ToggleQuickSettingsButton(modifier = modifier)
                }
            )
        }

        val sheetState = checkNotNull(sheetStateCaptured)
        assertThat(sheetState.isOpen).isFalse()

        // Click toggle button to open
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).performClick()
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isTrue()

        // Click toggle button again to close
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).performClick()
        composeTestRule.waitForIdle()

        assertThat(sheetState.isOpen).isFalse()
    }
}
