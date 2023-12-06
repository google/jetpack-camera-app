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
package com.example.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice

const val JCA_PACKAGE = "com.google.jetpackcamera"
const val DEFAULT_ITERATIONS = 5

enum class FlashMode {
    ON,
    OFF,
    AUTO
}
//todo: designate "default testing settings" to ensure consistency of benchmarks

// open or close quick settings menu on device
fun toggleQuickSettings(device: UiDevice) {
    device.findObject(By.res("QuickSettingDropDown")).click()
}

// set device direction using quick setting
fun setQuickFrontFacingCamera(shouldFaceFront: Boolean, device: UiDevice) {
    // flash on with front camera will automatically enable screen flash

    val isFrontFacing = device.findObject(By.desc("QuickSetFlip_is_front")) != null
    if (isFrontFacing != shouldFaceFront) {
        device.findObject(By.res("QuickSetFlipCamera")).click()
    }
}

// set device flash mode using quick setting
fun setQuickSetFlash(flashMode: FlashMode, device: UiDevice) {
    val selector =
        when (flashMode) {
            FlashMode.AUTO -> By.desc("QuickSetFlash_is_auto")
            FlashMode.ON -> By.desc("QuickSetFlash_is_on")
            FlashMode.OFF -> By.desc("QuickSetFlash_is_off")
        }

    while (device.findObject(selector) == null) {
        device.findObject(By.res("QuickSetFlash")).click()
    }
}