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
 * Identifies the set of camera effects that Night Mode allows, as a
 * `Set<com.google.jetpackcamera.model.CameraEffectId>` multibinding.
 *
 * The set is empty by default, so a selected camera effect is turned off while Night Mode is
 * active. An app that includes a camera effect and has verified that it works with Night Mode can
 * add it:
 *
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object NightModeEffectsModule {
 *     @Provides
 *     @ElementsIntoSet
 *     @NightModeCompatibleCameraEffects
 *     fun provideNightModeCompatibleCameraEffects(): Set<CameraEffectId> =
 *         setOf(SingleStreamEffectKey.id)
 * }
 * ```
 *
 * Contribute the set with `@ElementsIntoSet` as shown. `CameraEffectId` is a value class, so a
 * `@Provides @IntoSet` function that returns a single `CameraEffectId` gets a JVM name that the
 * dependency injection code generator can't see, and its contribution is silently dropped.
 *
 * Whether an effect works with Night Mode depends on the device's Night Mode extension, so verify
 * it on each class of device the app supports. If binding fails, the camera reports a stream
 * configuration error.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NightModeCompatibleCameraEffects
