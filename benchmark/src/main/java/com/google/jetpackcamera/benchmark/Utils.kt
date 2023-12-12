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
package com.google.jetpackcamera.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert

const val JCA_PACKAGE_NAME = "com.google.jetpackcamera"
const val DEFAULT_TEST_ITERATIONS = 5

// trace tags
const val IMAGE_CAPTURE_TRACE = "JCA Image Capture"

// enums
enum class FlashMode {
    ON,
    OFF,
    AUTO
}
// todo(kimblebee): designate "default testing settings" to ensure consistency of benchmarks

/**
 * function to click capture button on device.
 *
 * @param duration length of the click.
 */
fun clickCaptureButton(device: UiDevice, duration: Long = 0) {
    findObjectByRes(device, "CaptureButton")!!.click(duration)
}

/**
 * Toggle open or close quick settings menu on a device.
 */
fun toggleQuickSettings(device: UiDevice) {
    findObjectByRes(device, "QuickSettingDropDown", true)!!.click()
}

/**
 * Set device direction using quick settings.
 *
 * Quick Settings must first be opened with a call to [toggleQuickSettings]
 *
 *  @param shouldFaceFront the direction the camera should be facing
 *
 *
 */
fun setQuickFrontFacingCamera(shouldFaceFront: Boolean, device: UiDevice) {
    //if object description quicksetflip_is_front exists, then it is currently facing front
    val isFrontFacing = findObjectByDesc(device, "QuickSetFlip_is_front") != null

    if (isFrontFacing != shouldFaceFront) {
        findObjectByRes(device, "QuickSetFlipCamera", true)!!.click()
    }
}

/**
 * Set device flash mode using quick settings.
 * @param flashMode the designated [FlashMode] for the camera
 *
 */

fun setQuickSetFlash(flashMode: FlashMode, device: UiDevice) {
    val selector =
        when (flashMode) {
            FlashMode.AUTO -> By.desc("QuickSetFlash_is_auto")
            FlashMode.ON -> By.desc("QuickSetFlash_is_on")
            FlashMode.OFF -> By.desc("QuickSetFlash_is_off")
        }
    while (device.findObject(selector) == null) {
        findObjectByRes(device, "QuickSetFlash", true)!!.click()
    }
}

/**
 * Find an object by its test tag.
 */
fun findObjectByRes(
    device: UiDevice,
    testTag: String,
    shouldFailIfNotFound: Boolean = false
): UiObject2? {
    val selector = By.res(testTag)

    return if (!device.wait(Until.hasObject(selector), 2_500)) {
        if (shouldFailIfNotFound)
            Assert.fail("Did not find object with id $testTag")
        null
    } else {
        device.findObject(selector)
    }
}

/**
 * Find an object by its test description.
 */
fun findObjectByDesc(
    device: UiDevice,
    testDesc: String,
    timeout: Long = 2_500,
    shouldFailIfNotFound: Boolean = false
): UiObject2? {
    val selector = By.desc(testDesc)

    return if (!device.wait(Until.hasObject(selector), timeout)) {
        if (shouldFailIfNotFound)
            Assert.fail("Did not find object with id $testDesc in $timeout ms")
        null
    } else {
        device.findObject(selector)
    }
}
