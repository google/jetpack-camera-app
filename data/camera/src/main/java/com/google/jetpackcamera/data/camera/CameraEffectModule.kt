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
package com.google.jetpackcamera.data.camera

import com.google.jetpackcamera.core.camera.CameraEffectProvider
import com.google.jetpackcamera.core.camera.effects.CameraEffectFeatureKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt module to provide the map registry for available camera effects.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CameraEffectModule {
    @Multibinds
    abstract fun cameraEffectProviderEntries(): Set<
        Map.Entry<
            CameraEffectFeatureKey,
            @JvmSuppressWildcards Provider<CameraEffectProvider>
            >
        >

    companion object {
        @Provides
        @Singleton
        fun provideCameraEffectProviderMap(
            entries: @JvmSuppressWildcards Set<
                Map.Entry<
                    CameraEffectFeatureKey,
                    @JvmSuppressWildcards Provider<CameraEffectProvider>
                    >
                >
        ): Map<
            CameraEffectFeatureKey,
            @JvmSuppressWildcards Provider<CameraEffectProvider>
            > =
            entries.associate { it.key to it.value }
    }
}
