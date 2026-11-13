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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.Rect as AndroidRect
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInternal
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.ui.uistate.CustomSnackbarVisuals
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import java.lang.reflect.Proxy
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val TEST_COORDINATES = Offset(100f, 100f)

@RunWith(AndroidJUnit4::class)
class CaptureScreenComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val createdSurfaceRequests = mutableListOf<SurfaceRequest>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    @SuppressLint("RestrictedApi")
    @After
    fun tearDown() {
        createdSurfaceRequests.forEach { request ->
            request.willNotProvideSurface()
            request.deferrableSurface.close()
        }
        createdSurfaceRequests.clear()
    }

    @Test
    fun elapsedTimeText_unavailableState_doesNotRender() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Unavailable }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledActive_displaysFormattedTime() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                // 0:30
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Enabled(30_000_000_000L) }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals("0:30")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledActive_setsContentDescription() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                // 1:05
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Enabled(65_000_000_000L) }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG)
            .assertContentDescriptionEquals("Recording time: 1 minute and 5 seconds")
    }

    @Test
    fun elapsedTimeText_enabledPaused_displaysPausedFormattedTime() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = {
                    // PAUSED 0:30
                    ElapsedTimeUiState.Enabled(30_000_000_000L, isPaused = true)
                }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals("PAUSED 0:30")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledPaused_setsContentDescription() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = {
                    // PAUSED 1:05
                    ElapsedTimeUiState.Enabled(65_000_000_000L, isPaused = true)
                }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG)
            .assertContentDescriptionEquals("Recording paused: 1 minute and 5 seconds")
    }

    @Test
    fun focusMeteringIndicator_unspecifiedState_doesNotRender() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Unspecified,
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun focusMeteringIndicator_runningState_renders() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.RUNNING
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_successState_persistsWithoutAutoDismiss() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.SUCCESS
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_failureState_renders() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.FAILURE
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_cancelledState_doesNotRender() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.CANCELLED
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun focusMeteringIndicator_hasAccessibilityContentDescription_whenRunning() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.RUNNING
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG)
            .assertContentDescriptionEquals("Focusing")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_hasAccessibilityContentDescription_whenLocked() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.SUCCESS
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG)
            .assertContentDescriptionEquals("Focused")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun pillSnackbar_dismissButtonClick_callsOnDismissAndUsesStringResource() {
        var dismissed = false
        val dismissDescription =
            ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.snackbar_dismiss_content_description
            )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image capture failed",
                        actionLabel = null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                        isError = true
                    ),
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Image capture failed").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(dismissDescription)
            .assertIsDisplayed()
            .performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun pillSnackbar_actionButtonClick_callsOnAction() {
        var actionPerformed = false

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image capture failed",
                        actionLabel = "Retry",
                        withDismissAction = false,
                        duration = SnackbarDuration.Short,
                        isError = true
                    ),
                    onAction = { actionPerformed = true }
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .performClick()
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun pillSnackbar_statusIcon_hasNoContentDescription() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image saved",
                        actionLabel = null,
                        withDismissAction = false,
                        duration = SnackbarDuration.Short,
                        isError = false
                    )
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Image saved").assertIsDisplayed()
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true
        ).assertCountEquals(0)
    }

    @Test
    fun previewDisplay_loadingStateDarkTheme_passesAccessibilityChecks() {
        val loadingDescription =
            ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.camera_not_ready
            )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    Box(modifier = Modifier.size(width = 300.dp, height = 400.dp)) {
                        PreviewDisplay(
                            previewDisplayUiState = createPreviewDisplayUiState(),
                            onTapToFocus = { _, _ -> },
                            onFlipCamera = {},
                            onScaleZoom = {},
                            onRequestWindowColorMode = {},
                            surfaceRequest = null,
                            focusMeteringUiState = FocusMeteringUiState.Unspecified
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription(loadingDescription)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewDisplay_loadingStateLightTheme_passesAccessibilityChecks() {
        val loadingDescription =
            ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.camera_not_ready
            )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    Box(modifier = Modifier.size(width = 300.dp, height = 400.dp)) {
                        PreviewDisplay(
                            previewDisplayUiState = createPreviewDisplayUiState(),
                            onTapToFocus = { _, _ -> },
                            onFlipCamera = {},
                            onScaleZoom = {},
                            onRequestWindowColorMode = {},
                            surfaceRequest = null,
                            focusMeteringUiState = FocusMeteringUiState.Unspecified
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription(loadingDescription)
            .assertIsDisplayed()
            .assertRangeInfoEquals(ProgressBarRangeInfo.Indeterminate)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun previewDisplay_withSurfaceRequest_passesAccessibilityChecksAndFocusesCenter() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewfinderDescription =
            context.getString(R.string.camera_viewfinder_content_description)
        val density = context.resources.displayMetrics.density
        val widthPx = (300 * density).roundToInt()
        val heightPx = (400 * density).roundToInt()
        val surfaceRequest = createFakeSurfaceRequest(width = widthPx, height = heightPx)
        val recordedTaps = mutableListOf<Offset>()

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    Box(modifier = Modifier.size(width = 300.dp, height = 400.dp)) {
                        PreviewDisplay(
                            previewDisplayUiState = createPreviewDisplayUiState(),
                            onTapToFocus = { x, y -> recordedTaps.add(Offset(x, y)) },
                            onFlipCamera = {},
                            onScaleZoom = {},
                            onRequestWindowColorMode = {},
                            surfaceRequest = surfaceRequest,
                            focusMeteringUiState = FocusMeteringUiState.Unspecified
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val viewfinderNode = composeTestRule.onNodeWithContentDescription(viewfinderDescription)
        viewfinderNode.assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        viewfinderNode.performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(recordedTaps).hasSize(1)
        assertThat(recordedTaps.single().x).isWithin(1f).of(widthPx / 2f)
        assertThat(recordedTaps.single().y).isWithin(1f).of(heightPx / 2f)
    }

    private fun createPreviewDisplayUiState(
        aspectRatio: AspectRatio = AspectRatio.THREE_FOUR
    ): PreviewDisplayUiState = PreviewDisplayUiState(
        aspectRatioUiState = AspectRatioUiState.Available(
            selectedAspectRatio = aspectRatio,
            availableAspectRatios = listOf(
                SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
            )
        )
    )

    @SuppressLint("RestrictedApi")
    private fun createFakeSurfaceRequest(width: Int, height: Int): SurfaceRequest {
        val fakeCamera = Proxy.newProxyInstance(
            CameraInternal::class.java.classLoader,
            arrayOf(CameraInternal::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "FakeCameraInternal"
                "getHasTransform" -> true
                "isFrontFacing" -> false
                "isRemoved" -> false
                else -> when (method.returnType) {
                    Boolean::class.javaPrimitiveType -> false
                    Int::class.javaPrimitiveType -> 0
                    Long::class.javaPrimitiveType -> 0L
                    Float::class.javaPrimitiveType -> 0f
                    Double::class.javaPrimitiveType -> 0.0
                    else -> null
                }
            }
        } as CameraInternal

        return SurfaceRequest(Size(width, height), fakeCamera) {}.also { request ->
            request.updateTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                    AndroidRect(0, 0, width, height),
                    0,
                    Surface.ROTATION_0,
                    true,
                    Matrix(),
                    false
                )
            )
            createdSurfaceRequests.add(request)
        }
    }
}
