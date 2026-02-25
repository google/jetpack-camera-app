/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.uistate.capture

/**
 * Defines the UI state for the screen flash effect.
 *
 * This state is used to control the screen flash, which brightens the screen to illuminate the
 * subject when taking a photo, typically with the front-facing camera in low-light conditions.
 *
 * @param enabled Indicates whether the screen flash is currently active. When `true`, the UI
 * should display a bright, typically white, overlay.
 * @param onChangeComplete A callback to be invoked when the screen flash effect is complete. This
 * is used to signal that the capture process can proceed and to reset the UI state.
 * @param screenBrightnessToRestore The original screen brightness level before the flash was
 * activated. This value is used to restore the brightness to its previous state after the flash
 * effect is complete. A `null` value indicates that the brightness has not been changed or does not
 * need to be restored.
 */
data class ScreenFlashUiState(
    val enabled: Boolean = false,
    val onChangeComplete: () -> Unit = {},
    val screenBrightnessToRestore: Float? = null
) {
    companion object
}
