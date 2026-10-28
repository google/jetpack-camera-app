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
package com.google.jetpackcamera.model

/**
 * Whether captured media is mirrored (flipped horizontally) relative to the sensor output.
 *
 * The camera preview is always mirrored for front-facing lenses so that it behaves like a mirror.
 * Captures, however, are not mirrored unless requested, which makes a front-camera capture appear
 * flipped relative to the preview the user just saw. [ON_FRONT_ONLY] keeps captures consistent
 * with the preview.
 *
 * The values correspond to the CameraX `MirrorMode` constants.
 */
enum class MirrorMode {
    /** Captures are never mirrored. */
    OFF,

    /** Captures are mirrored only when the lens is front-facing, matching the preview. */
    ON_FRONT_ONLY;

    /**
     * Whether captures taken with [lensFacing] are mirrored under this mode.
     *
     * Resolves the mode for capture paths that cannot delegate the decision to CameraX, such as
     * `ImageCapture`, which has no mirror-mode API and is mirrored per capture instead.
     *
     * Still-capture mirroring is applied as an EXIF orientation flip rather than to the pixels, so
     * the photo only appears mirrored in viewers that honor EXIF orientation.
     */
    fun appliesTo(lensFacing: LensFacing): Boolean = when (this) {
        OFF -> false
        ON_FRONT_ONLY -> lensFacing == LensFacing.FRONT
    }
}
