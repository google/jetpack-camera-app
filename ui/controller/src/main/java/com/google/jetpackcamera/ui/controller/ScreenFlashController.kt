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
package com.google.jetpackcamera.ui.controller



/**
 * Controller for the screen flash feature.
 *
 * The screen flash feature temporarily brightens the screen to act as a flash
 * when capturing photos or videos (especially in low-light conditions using front-facing camera).
 */
interface ScreenFlashController {
    /**
     * Sets the original screen brightness level to be restored after the screen flash effect completes.
     *
     * This brightness value is stored and subsequently used by the system's clear overlay actions
     * to revert the screen back to its user-configured brightness state.
     *
     * @param brightness The original screen brightness level before the flash sequence was initiated.
     */
    fun setClearUiScreenBrightness(brightness: Float)
}
