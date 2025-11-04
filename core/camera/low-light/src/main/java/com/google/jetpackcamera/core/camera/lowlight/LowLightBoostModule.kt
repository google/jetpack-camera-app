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
package com.google.jetpackcamera.core.camera.lowlight

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LowLightBoostModule {

    @Multibinds
    abstract fun availabilityCheckerEntries(): Set<
        Map.Entry<
            LowLightBoostFeatureKey,
            @JvmSuppressWildcards Provider<LowLightBoostAvailabilityChecker>
            >
        >

    @Multibinds
    abstract fun effectProviderEntries(): Set<
        Map.Entry<
            LowLightBoostFeatureKey,
            @JvmSuppressWildcards Provider<LowLightBoostEffectProvider>
            >
        >

    companion object {
        @Provides
        @Singleton
        fun provideAvailabilityCheckerMap(
            entries: @JvmSuppressWildcards Set<
                Map.Entry<
                    LowLightBoostFeatureKey,
                    @JvmSuppressWildcards Provider<LowLightBoostAvailabilityChecker>
                    >
                >
        ): Map<
            LowLightBoostFeatureKey,
            @JvmSuppressWildcards Provider<LowLightBoostAvailabilityChecker>
            > =
            entries.associate { it.key to it.value }

        @Provides
        @Singleton
        fun provideEffectProviderMap(
            entries: @JvmSuppressWildcards Set<
                Map.Entry<
                    LowLightBoostFeatureKey,
                    @JvmSuppressWildcards Provider<LowLightBoostEffectProvider>
                    >
                >
        ): Map<
            LowLightBoostFeatureKey,
            @JvmSuppressWildcards Provider<LowLightBoostEffectProvider>
            > =
            entries.associate { it.key to it.value }
    }
}
