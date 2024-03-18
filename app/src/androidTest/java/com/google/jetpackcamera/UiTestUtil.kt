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

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.jetpackcamera.settings.model.CameraAppSettings
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert

object UiTestUtil {
    private fun getActivity(activityScenario: ActivityScenario<MainActivity>): MainActivity {
        val activityRef: AtomicReference<MainActivity> = AtomicReference<MainActivity>()
        activityScenario.onActivity(activityRef::set)
        return activityRef.get()
    }

    fun getPreviewCameraAppSettings(
        activityScenario: ActivityScenario<MainActivity>
    ): CameraAppSettings {
        return getActivity(
            activityScenario
        ).previewViewModel!!.previewUiState.value.currentCameraSettings
    }
}

inline fun <reified T : Activity> runScenarioTest(
    crossinline block: ActivityScenario<T>.() -> Unit
) {
    ActivityScenario.launch(T::class.java).use { scenario ->
        scenario.apply(block)
    }
}

/**
 * Find a composable by its test tag.
 */
fun findObjectByRes(
    device: UiDevice,
    testTag: String,
    timeout: Long = 2_500,
    shouldFailIfNotFound: Boolean = false
): UiObject2? {
    val selector = By.res(testTag)

    return if (!device.wait(Until.hasObject(selector), timeout)) {
        if (shouldFailIfNotFound) {
            Assert.fail("Did not find object with id $testTag")
        }
        null
    } else {
        device.findObject(selector)
    }
}
