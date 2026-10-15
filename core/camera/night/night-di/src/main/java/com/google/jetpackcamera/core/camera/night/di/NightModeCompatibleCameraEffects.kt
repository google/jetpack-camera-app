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
package com.google.jetpackcamera.core.camera.night.di

import javax.inject.Qualifier

/**
 * Identifies the `Set<CameraEffectId>` of camera effects that Night Mode allows. The set is empty
 * by default, so a selected effect is turned off while Night Mode is active. Whether an effect
 * works with Night Mode depends on the device.
 *
 * Example usage in a Hilt module (use `@ElementsIntoSet`, since `CameraEffectId` is a value class):
 * ```
 * @Provides
 * @ElementsIntoSet
 * @NightModeCompatibleCameraEffects
 * fun provideNightModeEffects(): Set<CameraEffectId> = setOf(SingleStreamEffectKey.id)
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NightModeCompatibleCameraEffects
