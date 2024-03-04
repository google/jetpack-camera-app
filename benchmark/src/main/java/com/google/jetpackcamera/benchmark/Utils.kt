/*
 * Copyright (C) 2024 The Android Open Source Project
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

// test tags
const val CAPTURE_BUTTON = "CaptureButton"
const val QUICK_SETTINGS_DROP_DOWN_BUTTON = "QuickSettingsDropDown"
const val QUICK_SETTINGS_FLASH_BUTTON = "QuickSettingsFlashButton"
const val QUICK_SETTINGS_FLIP_CAMERA_BUTTON = "QuickSettingsFlipCameraButton"
const val IMAGE_CAPTURE_SUCCESS_TOAST = "ImageCaptureSuccessToast"

// test descriptions
const val QUICK_SETTINGS_FLASH_OFF = "QUICK SETTINGS FLASH IS OFF"
const val QUICK_SETTINGS_FLASH_ON = "QUICK SETTINGS FLASH IS ON"
const val QUICK_SETTINGS_FLASH_AUTO = "QUICK SETTINGS FLASH IS AUTO"
const val QUICK_SETTINGS_LENS_FRONT = "QUICK SETTINGS LENS FACING FRONT"

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
    findObjectByRes(device, CAPTURE_BUTTON)!!.click(duration)
}

/**
 * Toggle open or close quick settings menu on a device.
 */
fun toggleQuickSettings(device: UiDevice) {
    findObjectByRes(
        device = device,
        testTag = QUICK_SETTINGS_DROP_DOWN_BUTTON,
        shouldFailIfNotFound = true
    )!!.click()
}

/**
 * Set device direction using quick settings.
 *
 * Quick Settings must first be opened with a call to [toggleQuickSettings]
 *
 *  @param shouldFaceFront the direction the camera should be facing
 */
fun setQuickFrontFacingCamera(shouldFaceFront: Boolean, device: UiDevice) {
    val isFrontFacing = findObjectByDesc(device, QUICK_SETTINGS_LENS_FRONT) != null

    if (isFrontFacing != shouldFaceFront) {
        findObjectByRes(
            device = device,
            testTag = QUICK_SETTINGS_FLIP_CAMERA_BUTTON,
            shouldFailIfNotFound = true
        )!!.click()
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
            FlashMode.AUTO -> By.desc(QUICK_SETTINGS_FLASH_AUTO)
            FlashMode.ON -> By.desc(QUICK_SETTINGS_FLASH_ON)
            FlashMode.OFF -> By.desc(QUICK_SETTINGS_FLASH_OFF)
        }
    while (device.findObject(selector) == null) {
        findObjectByRes(
            device = device,
            testTag = QUICK_SETTINGS_FLASH_BUTTON,
            shouldFailIfNotFound = true
        )!!.click()
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
        if (shouldFailIfNotFound) {
            Assert.fail("Did not find object with id $testDesc in $timeout ms")
        }
        null
    } else {
        device.findObject(selector)
    }
}
