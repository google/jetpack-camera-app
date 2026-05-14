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

import android.app.Application
import android.content.Context
import com.google.jetpackcamera.core.camera.CameraXCameraSystem
import com.google.jetpackcamera.core.camera.lowlight.LowLightBoostAvailabilityChecker
import com.google.jetpackcamera.core.camera.lowlight.LowLightBoostEffectProvider
import com.google.jetpackcamera.core.camera.lowlight.LowLightBoostFeatureKey
import com.google.jetpackcamera.core.camera.postprocess.ImagePostProcessor
import com.google.jetpackcamera.core.camera.postprocess.ImagePostProcessorFeatureKey
import com.google.jetpackcamera.core.common.DefaultDispatcher
import com.google.jetpackcamera.core.common.DefaultFilePathGenerator
import com.google.jetpackcamera.core.common.FilePathGenerator
import com.google.jetpackcamera.core.common.IODispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dagger [Module] for camera data layer.
 */
@Module
@InstallIn(SingletonComponent::class)
interface CameraModule {

    @Binds
    @Singleton
    fun bindsCameraSystemRepository(
        repository: CameraXCameraSystemRepository
    ): CameraSystemRepository

    companion object {
        @Provides
        @Singleton
        fun providesCameraXCameraSystem(
            @ApplicationContext context: Context,
            @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
            @IODispatcher ioDispatcher: CoroutineDispatcher,
            @DefaultFilePathGenerator filePathGenerator: FilePathGenerator,
            availabilityCheckers: Map<
                LowLightBoostFeatureKey,
                @JvmSuppressWildcards Provider<LowLightBoostAvailabilityChecker>
                >,
            effectProviders: Map<
                LowLightBoostFeatureKey,
                @JvmSuppressWildcards Provider<LowLightBoostEffectProvider>
                >,
            imagePostProcessors: Map<
                ImagePostProcessorFeatureKey,
                @JvmSuppressWildcards Provider<ImagePostProcessor>
                >
        ): CameraXCameraSystem {
            return CameraXCameraSystem(
                context as Application,
                defaultDispatcher,
                ioDispatcher,
                filePathGenerator,
                availabilityCheckers,
                effectProviders,
                imagePostProcessors
            )
        }
    }
}
