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

/**
 * Declares the multibindings through which optional modules contribute capture sub-modes.
 *
 * All bindings are declared with [Multibinds] so that they resolve to empty collections when no
 * sub-mode module is included in the build.
 */
@Module
@InstallIn(SingletonComponent::class)
interface CaptureSubModeModule {
    /**
     * Set of [CaptureSubModeProvider]s keyed by [CaptureSubModeFeatureKey], contributed by optional
     * sub-mode modules via `@IntoSet`.
     *
     * Each provider is wrapped in a [Provider] so that it is only instantiated when the camera
     * system first needs it.
     */
    @Multibinds
    fun captureSubModeProviderEntries(): Set<
        Map.Entry<
            CaptureSubModeFeatureKey,
            @JvmSuppressWildcards Provider<CaptureSubModeProvider>
            >
        >

    /**
     * Map of [CaptureMode] to the [CaptureSubModeFeatureKey] that the app binds as that mode's
     * default sub-mode, contributed via `@IntoMap` with [DefaultCaptureSubModeFor].
     *
     * Each [CaptureMode] can have at most one default; Dagger rejects duplicate keys at compile
     * time.
     */
    @Multibinds
    fun defaultCaptureSubModes(): Map<
        CaptureMode,
        @JvmSuppressWildcards CaptureSubModeFeatureKey
        >

    companion object {
        /**
         * Collapses [captureSubModeProviderEntries] into a map keyed by
         * [CaptureSubModeFeatureKey]. If two entries share a key, the last one wins.
         */
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
    }
}
