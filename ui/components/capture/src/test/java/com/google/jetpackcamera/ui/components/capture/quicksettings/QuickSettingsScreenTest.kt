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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_IMAGE_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_OFF
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_HDR_OPTION_OFF
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_HDR_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_3_4_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_9_16_BUTTON
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_ASPECT_RATIO
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_FLASH
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_HDR
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.controller.testing.FakeQuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val BOX_SCAFFOLD_WRAPPER_TAG = "box_scaffold_wrapper"

@RunWith(RobolectricTestRunner::class)
class QuickSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeQuickSettingsController = FakeQuickSettingsController()

    private val sampleAvailableUiState = QuickSettingsUiState.Available(
        aspectRatioUiState = AspectRatioUiState.Available(
            selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
            availableAspectRatios = listOf(
                SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE),
                SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN)
            )
        ),
        captureModeUiState = CaptureModeUiState.Available(
            selectedCaptureMode = CaptureMode.STANDARD,
            availableCaptureModes = listOf(
                SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
            )
        ),
        flashModeUiState = FlashModeUiState.Available(
            selectedFlashMode = FlashMode.OFF,
            availableFlashModes = listOf(
                SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                SingleSelectableUiState.SelectableUi(FlashMode.ON),
                SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
            ),
            isLowLightBoostActive = false
        ),
        flipLensUiState = FlipLensUiState.Available(
            selectedLensFacing = LensFacing.BACK,
            availableLensFacings = listOf(
                SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
            )
        ),
        hdrUiState = HdrUiState.Unavailable
    )

    @Test
    fun quickSettingsScaffoldContent_unavailableState_doesNotRender() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = QuickSettingsUiState.Unavailable,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()
    }

    @Test
    fun quickSettingsScaffoldContent_availableState_rendersSettings() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_FLASH).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ROW_QUICK_SETTINGS_ASPECT_RATIO).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).assertIsDisplayed()
    }

    @Test
    fun quickSettingsScaffoldContent_noTitleOverride_usesCaptureModeTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.quick_settings_title_photo_and_video_settings
            )
        ).assertIsDisplayed()
    }

    @Test
    fun quickSettingsScaffoldContent_titleOverride_usesOverrideTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState.copy(
                        titleResId = R.string.quick_settings_title_video_settings
                    ),
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.quick_settings_title_video_settings
            )
        ).assertIsDisplayed()
    }

    @Test
    fun quickSettingsScaffoldContent_flashSelection_callsController() {
        var setFlashCalledWith: FlashMode? = null
        fakeQuickSettingsController.setFlashAction = { setFlashCalledWith = it }

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).performClick()
        assertThat(setFlashCalledWith).isEqualTo(FlashMode.ON)
    }

    @Test
    fun quickSettingsScaffoldContent_moreSettingsButton_triggersCallback() {
        var onNavigateToSettingsCalled = false

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = { onNavigateToSettingsCalled = true },
                    quickSettingsController = fakeQuickSettingsController,
                    showMoreSettingsButton = true
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).performClick()
        assertThat(onNavigateToSettingsCalled).isTrue()
    }

    @Test
    fun quickSettingsScaffoldContent_hideMoreSettingsButton_doesNotRenderButton() {
        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController,
                    showMoreSettingsButton = false
                )
            }
        }

        composeTestRule.onNodeWithTag(SETTINGS_BUTTON).assertDoesNotExist()
    }

    @Test
    fun quickSettingsContent_decouplesBottomSheetPaddingFromInnerColumn() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(modifier = Modifier.testTag(BOX_SCAFFOLD_WRAPPER_TAG)) {
                    QuickSettingsScaffoldContent(
                        quickSettingsUiState = sampleAvailableUiState,
                        onNavigateToSettings = {},
                        quickSettingsController = fakeQuickSettingsController
                    )
                }
            }
        }

        val wrapperHeight = composeTestRule
            .onNodeWithTag(BOX_SCAFFOLD_WRAPPER_TAG)
            .getUnclippedBoundsInRoot()
            .height
        val contentHeight = composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
            .getUnclippedBoundsInRoot()
            .height

        assertThat(wrapperHeight - contentHeight).isAtLeast(24.dp)
    }

    @Test
    fun quickSettingsTitleRes_returnsExpectedStringResForCaptureMode() {
        assertThat(quickSettingsTitleRes(sampleAvailableUiState))
            .isEqualTo(R.string.quick_settings_title_photo_and_video_settings)

        val imageOnlyState = sampleAvailableUiState.copy(
            captureModeUiState = CaptureModeUiState.Available(
                selectedCaptureMode = CaptureMode.IMAGE_ONLY,
                availableCaptureModes = listOf(
                    SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
                )
            )
        )
        assertThat(quickSettingsTitleRes(imageOnlyState))
            .isEqualTo(R.string.quick_settings_title_photo_settings)

        val videoOnlyState = sampleAvailableUiState.copy(
            captureModeUiState = CaptureModeUiState.Available(
                selectedCaptureMode = CaptureMode.VIDEO_ONLY,
                availableCaptureModes = listOf(
                    SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY)
                )
            )
        )
        assertThat(quickSettingsTitleRes(videoOnlyState))
            .isEqualTo(R.string.quick_settings_title_video_settings)

        val unavailableCaptureModeState = sampleAvailableUiState.copy(
            captureModeUiState = CaptureModeUiState.Unavailable
        )
        assertThat(quickSettingsTitleRes(unavailableCaptureModeState))
            .isEqualTo(R.string.quick_settings_title_photo_settings)
    }

    @Test
    fun quickSettingsScaffoldContent_header_hasHeadingSemanticsAcrossCaptureModes() {
        var captureMode by mutableStateOf(CaptureMode.STANDARD)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val standardTitle = context.getString(
            R.string.quick_settings_title_photo_and_video_settings
        )
        val photoTitle = context.getString(R.string.quick_settings_title_photo_settings)
        val videoTitle = context.getString(R.string.quick_settings_title_video_settings)

        composeTestRule.setContent {
            val uiState = sampleAvailableUiState.copy(
                captureModeUiState = CaptureModeUiState.Available(
                    selectedCaptureMode = captureMode,
                    availableCaptureModes = listOf(
                        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                        SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
                    )
                )
            )
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = uiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithText(standardTitle)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { captureMode = CaptureMode.IMAGE_ONLY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(photoTitle)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        composeTestRule.runOnIdle { captureMode = CaptureMode.VIDEO_ONLY }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(videoTitle)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun quickSettingsScaffoldContent_singleSelectRows_exposeRadioButtonAndSelectableGroup() {
        val allRowsState = sampleAvailableUiState.copy(
            hdrUiState = HdrUiState.Available(
                selectedImageFormat = ImageOutputFormat.JPEG,
                selectedDynamicRange = DynamicRange.SDR,
                isSupported = true
            )
        )

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = allRowsState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        val rowTags = listOf(
            ROW_QUICK_SETTINGS_FLASH,
            ROW_QUICK_SETTINGS_CAPTURE_MODE,
            ROW_QUICK_SETTINGS_ASPECT_RATIO,
            ROW_QUICK_SETTINGS_HDR
        )
        rowTags.forEach { rowTag ->
            composeTestRule.onNodeWithTag(rowTag)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
            composeTestRule.onNode(
                hasParent(hasTestTag(rowTag)) and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
            ).assertIsDisplayed()
        }

        val radioMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF)
            .assert(radioMatcher)
            .assertIsOn()
        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON)
            .assert(radioMatcher)
            .assertIsOff()
        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO)
            .assert(radioMatcher)
            .assertIsOff()

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD)
            .assert(radioMatcher)
            .assertIsOn()
        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY)
            .assert(radioMatcher)
            .assertIsOff()
        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_IMAGE_ONLY)
            .assert(radioMatcher)
            .assertIsOff()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_9_16_BUTTON)
            .assert(radioMatcher)
            .assertIsOn()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_3_4_BUTTON)
            .assert(radioMatcher)
            .assertIsOff()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON)
            .assert(radioMatcher)
            .assertIsOff()

        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_HDR_OPTION_OFF)
            .assert(radioMatcher)
            .assertIsOn()
        composeTestRule.onNodeWithTag(BTN_QUICK_SETTINGS_HDR_OPTION_ON)
            .assert(radioMatcher)
            .assertIsOff()
    }

    @Test
    fun quickSettingsScaffoldContent_aspectRatioSubtitle_exposesSpokenContentDescription() {
        var aspectRatio by mutableStateOf(AspectRatio.NINE_SIXTEEN)
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.setContent {
            val uiState = sampleAvailableUiState.copy(
                aspectRatioUiState = AspectRatioUiState.Available(
                    selectedAspectRatio = aspectRatio,
                    availableAspectRatios = listOf(
                        SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                        SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
                    )
                )
            )
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = uiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.quick_settings_aspect_ratio_subtitle_nine_sixteen),
            useUnmergedTree = true
        ).assertContentDescriptionEquals(
            context.getString(R.string.quick_settings_aspect_ratio_9_16_description)
        )

        composeTestRule.runOnIdle { aspectRatio = AspectRatio.THREE_FOUR }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            context.getString(R.string.quick_settings_aspect_ratio_subtitle_three_four),
            useUnmergedTree = true
        ).assertContentDescriptionEquals(
            context.getString(R.string.quick_settings_aspect_ratio_3_4_description)
        )

        composeTestRule.runOnIdle { aspectRatio = AspectRatio.ONE_ONE }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            context.getString(R.string.quick_settings_aspect_ratio_subtitle_one_one),
            useUnmergedTree = true
        ).assertContentDescriptionEquals(
            context.getString(R.string.quick_settings_aspect_ratio_1_1_description)
        )
    }

    @Test
    fun quickSettingsScaffoldContent_aspectRatioSelection_callsController() {
        val selectedRatios = mutableListOf<AspectRatio>()
        fakeQuickSettingsController.setAspectRatioAction = { selectedRatios.add(it) }

        composeTestRule.setContent {
            MaterialTheme {
                QuickSettingsScaffoldContent(
                    quickSettingsUiState = sampleAvailableUiState,
                    onNavigateToSettings = {},
                    quickSettingsController = fakeQuickSettingsController
                )
            }
        }

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_3_4_BUTTON).performClick()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON).performClick()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_9_16_BUTTON).performClick()

        assertThat(selectedRatios).containsExactly(
            AspectRatio.THREE_FOUR,
            AspectRatio.ONE_ONE,
            AspectRatio.NINE_SIXTEEN
        ).inOrder()
    }

    @Test
    fun cameraAspectRatio_returnsDistinctDrawableAndDescriptionForEachRatio() {
        assertThat(CameraAspectRatio.THREE_FOUR.getDrawableResId())
            .isEqualTo(R.drawable.ic_crop_portrait)
        assertThat(CameraAspectRatio.THREE_FOUR.getDescriptionResId())
            .isEqualTo(R.string.quick_settings_aspect_ratio_3_4_description)

        assertThat(CameraAspectRatio.ONE_ONE.getDrawableResId())
            .isEqualTo(R.drawable.ic_crop_square)
        assertThat(CameraAspectRatio.ONE_ONE.getDescriptionResId())
            .isEqualTo(R.string.quick_settings_aspect_ratio_1_1_description)

        assertThat(CameraAspectRatio.NINE_SIXTEEN.getDrawableResId())
            .isEqualTo(R.drawable.ic_crop_9_16)
        assertThat(CameraAspectRatio.NINE_SIXTEEN.getDescriptionResId())
            .isEqualTo(R.string.quick_settings_aspect_ratio_9_16_description)

        val drawables = CameraAspectRatio.entries.map { it.getDrawableResId() }
        assertThat(drawables).containsNoDuplicates()
    }
}
