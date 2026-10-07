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

import com.google.jetpackcamera.core.camera.submode.CaptureSubModeFeatureKey
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import com.google.jetpackcamera.model.CaptureMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface CaptureSubModeModule {
    @Multibinds
    fun captureSubModeProviderEntries(): Set<
        Map.Entry<
            CaptureSubModeFeatureKey,
            @JvmSuppressWildcards Provider<CaptureSubModeProvider>
            >
        >

    @Multibinds
    fun defaultCaptureSubModes(): Map<
        CaptureMode,
        @JvmSuppressWildcards CaptureSubModeFeatureKey
        >

    @Multibinds
    fun defaultCaptureSubModeEntries(): Set<
        Map.Entry<
            CaptureMode,
            @JvmSuppressWildcards CaptureSubModeFeatureKey
            >
        >

    companion object {
        @Provides
        @Singleton
        fun provideCaptureSubModeProviderMap(
            entries: Set<
                @JvmSuppressWildcards
                Map.Entry<
                    CaptureSubModeFeatureKey,
                    @JvmSuppressWildcards Provider<CaptureSubModeProvider>
                    >
                >
        ): Map<CaptureSubModeFeatureKey, Provider<CaptureSubModeProvider>> =
            entries.associate { it.key to it.value }

        fun resolveDefaultCaptureSubModes(
            multiboundMap: Map<
                CaptureMode,
                @JvmSuppressWildcards CaptureSubModeFeatureKey
                > = emptyMap(),
            entries: Set<
                @JvmSuppressWildcards
                Map.Entry<
                    CaptureMode,
                    @JvmSuppressWildcards CaptureSubModeFeatureKey
                    >
                > = emptySet()
        ): Map<CaptureMode, CaptureSubModeFeatureKey> =
            entries.associate { it.key to it.value } + multiboundMap
    }
}
