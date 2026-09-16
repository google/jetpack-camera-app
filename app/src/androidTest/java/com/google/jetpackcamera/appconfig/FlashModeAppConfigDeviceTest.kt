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
package com.google.jetpackcamera.appconfig

import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.AppModule
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppConfig
import com.google.jetpackcamera.settings.model.OptionAvailabilityConfig
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_OFF
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FLASH_OPTION_ON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_FLASH
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getCurrentFlashMode
import com.google.jetpackcamera.utils.getCurrentLensFacing
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FlashModeAppConfigDeviceTest(
    private val lensFacing: LensFacing
) {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        if (lensFacing == LensFacing.FRONT) {
            assume()
                .withMessage("Device does not have a front camera")
                .that(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
                .isTrue()
        } else {
            assume()
                .withMessage("Device does not have a back camera")
                .that(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
                .isTrue()
        }
    }

    @After
    fun tearDown() {
        AppModule.testCameraAppConfig = null
    }

    private fun ComposeTestRule.prepareLens() {
        waitForCaptureButton()
        val currentLens = getCurrentLensFacing()
        if (currentLens != lensFacing) {
            onNodeWithTag(FLIP_CAMERA_BUTTON).assume(isEnabled()) {
                "Device does not have a $lensFacing camera to flip to."
            }.performClick()
            waitUntil(DEFAULT_TIMEOUT_MILLIS) { getCurrentLensFacing() == lensFacing }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section A: Default Values
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun defaultFlashMode_on_startsInFlashOn() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(defaultValue = FlashMode.ON)
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                val onExists = onAllNodesWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON)
                    .fetchSemanticsNodes().isNotEmpty()
                if (onExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).assertIsOn()
                    assertThat(getCurrentFlashMode()).isEqualTo(FlashMode.ON)
                } else {
                    // If the lens does not support flash/screen-flash ON, verify fallback
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).assertDoesNotExist()
                    if (onAllNodesWithTag(
                            ROW_QUICK_SETTINGS_FLASH
                        ).fetchSemanticsNodes().isNotEmpty()
                    ) {
                        onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertIsOn()
                    }
                }
            }
        }
    }

    @Test
    fun defaultFlashMode_auto_startsInAutoOrFallsBackToOff() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(defaultValue = FlashMode.AUTO)
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                val autoExists = onAllNodesWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO)
                    .fetchSemanticsNodes().isNotEmpty()
                if (autoExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO).assertIsOn()
                    assertThat(getCurrentFlashMode()).isEqualTo(FlashMode.AUTO)
                } else {
                    // If AUTO is not supported on this lens, option must not be in view
                    // and active selection must fall back to OFF (if flash row is present)
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO).assertDoesNotExist()
                    if (onAllNodesWithTag(
                            ROW_QUICK_SETTINGS_FLASH
                        ).fetchSemanticsNodes().isNotEmpty()
                    ) {
                        onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertIsOn()
                    }
                }
            }
        }
    }

    @Test
    fun defaultFlashMode_lowLightBoost_startsInLowLightBoostOrFallsBackToOff() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(defaultValue = FlashMode.LOW_LIGHT_BOOST)
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                val llbExists = onAllNodesWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST)
                    .fetchSemanticsNodes().isNotEmpty()
                if (llbExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST).assertIsOn()
                    assertThat(getCurrentFlashMode()).isEqualTo(FlashMode.LOW_LIGHT_BOOST)
                } else {
                    // If Low Light Boost is not supported on this lens, option must not be in view
                    // and active selection must fall back to OFF (if flash row is present)
                    onNodeWithTag(
                        BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST
                    ).assertDoesNotExist()
                    if (onAllNodesWithTag(
                            ROW_QUICK_SETTINGS_FLASH
                        ).fetchSemanticsNodes().isNotEmpty()
                    ) {
                        onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertIsOn()
                    }
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section B: Hidden
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun hiddenFlashMode_removesFlashControlFromQuickSettings() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.Hidden
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_FLASH).assertDoesNotExist()
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section C: OptionsEnabled / Filtering
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun optionsEnabled_offAndOn_displaysOnlyOffAndOnInQuickSettings() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(FlashMode.OFF, FlashMode.ON)
                )
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                // AUTO and LOW_LIGHT_BOOST must never be present
                onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO).assertDoesNotExist()
                onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST).assertDoesNotExist()

                val onExists = onAllNodesWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON)
                    .fetchSemanticsNodes().isNotEmpty()
                if (onExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertExists()
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertIsOn()
                }
            }
        }
    }

    @Test
    fun optionsEnabled_offAndAuto_displaysOnlyAllowedOptionsInQuickSettings() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(FlashMode.OFF, FlashMode.AUTO)
                )
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                // ON and LOW_LIGHT_BOOST must never be present
                onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).assertDoesNotExist()
                onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST).assertDoesNotExist()

                val autoExists = onAllNodesWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO)
                    .fetchSemanticsNodes().isNotEmpty()
                if (autoExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertExists()
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertIsOn()
                }
            }
        }
    }

    @Test
    fun optionsEnabled_offAndLowLightBoost_displaysOnlyAllowedOptionsInQuickSettings() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(FlashMode.OFF, FlashMode.LOW_LIGHT_BOOST)
                )
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.prepareLens()
            composeTestRule.visitQuickSettings {
                // ON and AUTO must never be present
                onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_ON).assertDoesNotExist()
                onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_AUTO).assertDoesNotExist()

                val llbExists = onAllNodesWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_LOW_LIGHT_BOOST)
                    .fetchSemanticsNodes().isNotEmpty()
                if (llbExists) {
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertExists()
                    onNodeWithTag(BTN_QUICK_SETTINGS_FLASH_OPTION_OFF).assertIsOn()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing_{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(LensFacing.BACK),
            arrayOf(LensFacing.FRONT)
        )
    }
}
