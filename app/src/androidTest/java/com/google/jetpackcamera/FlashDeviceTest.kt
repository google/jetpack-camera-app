/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.SCREEN_FLASH_OVERLAY
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.settings.model.FlashMode
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FlashDeviceTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var activityScenario: ActivityScenario<MainActivity>? = null
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Before
    fun setUp() {
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
        uiDevice.waitForIdle(2000)
    }

    @Test
    fun set_flash_on() = runTest {
        uiDevice.waitForIdle()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        assert(
            UiTestUtil.getPreviewCameraAppSettings(activityScenario!!).flashMode ==
                FlashMode.ON
        )
    }

    @Test
    fun set_flash_auto() = runTest {
        uiDevice.waitForIdle()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        assert(
            UiTestUtil.getPreviewCameraAppSettings(activityScenario!!).flashMode ==
                FlashMode.AUTO
        )
    }

    @Test
    fun set_flash_off() = runTest {
        uiDevice.waitForIdle()
        assert(
            UiTestUtil.getPreviewCameraAppSettings(activityScenario!!).flashMode ==
                FlashMode.OFF
        )
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        assert(
            UiTestUtil.getPreviewCameraAppSettings(activityScenario!!).flashMode ==
                FlashMode.OFF
        )
    }

    @Test
    fun set_screen_flash_and_capture_successfully() = runTest {
        uiDevice.waitForIdle()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        // flash on with front camera will automatically enable screen flash
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(CAPTURE_BUTTON)).click()
        uiDevice.wait(
            Until.findObject(By.res("ImageCaptureSuccessToast")),
            5000
        )
    }

    @Test
    fun set_screen_flash_and_capture_with_screen_change_overlay_shown() = runTest {
        uiDevice.waitForIdle()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        // flash on with front camera will automatically enable screen flash
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLASH_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(CAPTURE_BUTTON)).click()
        uiDevice.wait(
            Until.findObject(By.res(SCREEN_FLASH_OVERLAY)),
            5000
        )
    }
}
