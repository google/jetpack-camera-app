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
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_CAPTURE_MODE_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundDeviceTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    private fun backgroundThenForegroundApp() {
        uiDevice.pressHome()
        uiDevice.waitForIdle(1500)
        uiDevice.pressRecentApps()
        uiDevice.waitForIdle(1500)
        uiDevice.click(uiDevice.displayWidth / 2, uiDevice.displayHeight / 2)
        uiDevice.waitForIdle(1500)
    }

    @Before
    fun setUp() {
        ActivityScenario.launch(MainActivity::class.java)
        uiDevice.waitForIdle(2000)
    }

    @Test
    fun background_foreground() {
        backgroundThenForegroundApp()
    }

    @Test
    fun flipCamera_then_background_foreground() {
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_FLIP_CAMERA_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.waitForIdle(2000)
        backgroundThenForegroundApp()
    }

    @Test
    fun setAspectRatio_then_background_foreground() {
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_RATIO_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_RATIO_1_1_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.waitForIdle(2000)
        backgroundThenForegroundApp()
    }

    @Test
    fun toggleCaptureMode_then_background_foreground() {
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_CAPTURE_MODE_BUTTON)).click()
        uiDevice.findObject(By.res(QUICK_SETTINGS_DROP_DOWN)).click()
        uiDevice.waitForIdle(2000)
        backgroundThenForegroundApp()
    }
}
