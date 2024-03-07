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
import com.google.jetpackcamera.settings.model.CameraAppSettings
import java.util.concurrent.atomic.AtomicReference

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
