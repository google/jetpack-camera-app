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
package com.google.jetpackcamera.core.camera.concurrent.di

import com.google.jetpackcamera.core.camera.concurrent.ConcurrentCameraSubModeKey
import com.google.jetpackcamera.core.camera.concurrent.ConcurrentCameraSubModeProvider
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeFeatureKey
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.util.AbstractMap
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
internal object ConcurrentCameraSubModeModule {
    @Provides
    @IntoSet
    fun provideConcurrentCameraSubModeEntry(): Map.Entry<
        CaptureSubModeFeatureKey,
        @JvmSuppressWildcards Provider<CaptureSubModeProvider>
        > =
        AbstractMap.SimpleImmutableEntry(
            ConcurrentCameraSubModeKey,
            Provider { ConcurrentCameraSubModeProvider() }
        )
}
