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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Accessibility Test Framework (ATF) checks and semantics verification
 * for [ZoomBar].
 */
@RunWith(AndroidJUnit4::class)
class ZoomBarTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    @Test
    fun zoomBar_horizontalLtr_black60Colors_passesAccessibilityChecks() {
        composeTestRule.setContent {
            ZoomBarTestHost(
                orientation = Orientation.Horizontal,
                layoutDirection = LayoutDirection.Ltr,
                primaryZoomRatio = 1.0f
            )
        }

        composeTestRule.onNodeWithTag(ZOOM_BUTTON_ROW_TAG).assertExists()
        composeTestRule.onNodeWithTag(ZOOM_BUTTON_MIN_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertContentDescriptionEquals("0.5 times zoom")
        composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG).assertIsSelected()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun zoomBar_horizontalRtl_black60Colors_passesAccessibilityChecks() {
        composeTestRule.setContent {
            ZoomBarTestHost(
                orientation = Orientation.Horizontal,
                layoutDirection = LayoutDirection.Rtl,
                primaryZoomRatio = 0.5f
            )
        }

        composeTestRule.onNodeWithTag(ZOOM_BUTTON_MIN_TAG).assertIsSelected()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun zoomBar_verticalRtl_black60Colors_passesAccessibilityChecks() {
        composeTestRule.setContent {
            ZoomBarTestHost(
                orientation = Orientation.Vertical,
                layoutDirection = LayoutDirection.Rtl,
                primaryZoomRatio = 2.0f
            )
        }

        composeTestRule.onNodeWithTag(ZOOM_BUTTON_2_TAG).assertIsSelected()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun zoomBar_pinchedRatio_retainsClickResetAndPassesAccessibilityChecks() {
        var resetZoom: Float? = null
        composeTestRule.setContent {
            ZoomBarTestHost(
                primaryZoomRatio = 1.4f,
                onChangeZoom = { resetZoom = it }
            )
        }

        composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG)
            .assertIsNotSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Current zoom 1.4 times"
                )
            )
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag(ZOOM_BUTTON_1_TAG)
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(resetZoom).isEqualTo(1.0f)
    }

    @Test
    fun zoomBar_rotatedLabels90DegreesWithExtendedRatio_passesAccessibilityChecks() {
        composeTestRule.setContent {
            ZoomBarTestHost(
                zoomLevels = listOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f),
                primaryZoomRatio = 10.5f,
                orientation = Orientation.Horizontal,
                labelRotationDegrees = 90f
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun zoomBar_white20ColorsOverDarkBackground_passesAccessibilityChecks() {
        composeTestRule.setContent {
            ZoomBarTestHost(
                primaryZoomRatio = 1.0f,
                backgroundColor = Color.Black,
                useWhite20Colors = true
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Composable
    private fun ZoomBarTestHost(
        zoomLevels: List<Float> = listOf(0.5f, 1.0f, 2.0f, 5.0f),
        primaryZoomRatio: Float = 1.0f,
        orientation: Orientation = Orientation.Horizontal,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        labelRotationDegrees: Float = 0f,
        backgroundColor: Color = Color.Black,
        useWhite20Colors: Boolean = false,
        onChangeZoom: (Float) -> Unit = {}
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalDisableAnimations provides true
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomBar(
                        zoomControlUiState = ZoomControlUiState.Enabled(
                            zoomLevels = zoomLevels,
                            primaryLensFacing = LensFacing.BACK,
                            primaryZoomRatio = primaryZoomRatio
                        ),
                        onChangeZoom = onChangeZoom,
                        orientation = orientation,
                        colors = if (useWhite20Colors) {
                            ZoomBarDefaults.white20Colors()
                        } else {
                            ZoomBarDefaults.black60Colors()
                        },
                        labelRotationDegrees = labelRotationDegrees
                    )
                }
            }
        }
    }
}
