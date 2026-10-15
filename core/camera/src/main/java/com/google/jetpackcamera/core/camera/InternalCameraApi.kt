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
package com.google.jetpackcamera.core.camera

/**
 * Marks camera session APIs that are public only so that capture sub-mode modules (for example,
 * a module that runs its own
 * [com.google.jetpackcamera.core.camera.submode.CameraSessionBinding.Custom] session) can build on
 * them.
 *
 * These APIs are not stable and may change without notice. Apps should use [CameraSystem]
 * instead. Using them requires opting in with `@OptIn(InternalCameraApi::class)`.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Internal camera session API intended only for camera session sub-mode modules."
)
@Retention(AnnotationRetention.BINARY)
annotation class InternalCameraApi
