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
package com.google.jetpackcamera.ui.uistate.capture

/**
 * Which groups of capture screen controls are shown.
 *
 * A group that is not visible fades out and stops accepting input as soon as it is hidden, then
 * fades back in when it is visible again. Its space in the layout is kept, so the remaining
 * controls do not move.
 *
 * @param isTopBarVisible Whether the top status indicator bar is shown.
 * @param isQuickSettingsButtonVisible Whether the Quick Settings toggle button is shown.
 * @param isZoomControlsVisible Whether the zoom controls are shown.
 * @param isViewfinderOverlayVisible Whether host-provided content drawn over the viewfinder is
 * shown, such as a close button or a chip in the top corners.
 */
data class CaptureChromeVisibility(
    val isTopBarVisible: Boolean = true,
    val isQuickSettingsButtonVisible: Boolean = true,
    val isZoomControlsVisible: Boolean = true,
    val isViewfinderOverlayVisible: Boolean = true
) {
    companion object {
        /** All groups hidden. */
        val Hidden = CaptureChromeVisibility(
            isTopBarVisible = false,
            isQuickSettingsButtonVisible = false,
            isZoomControlsVisible = false,
            isViewfinderOverlayVisible = false
        )
    }
}
