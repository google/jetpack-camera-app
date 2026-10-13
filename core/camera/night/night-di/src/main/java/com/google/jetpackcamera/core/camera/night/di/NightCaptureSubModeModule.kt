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

import android.content.Context
import com.google.jetpackcamera.core.camera.night.NightCaptureSubModeKey
import com.google.jetpackcamera.core.camera.night.NightCaptureSubModeProvider
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeFeatureKey
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import com.google.jetpackcamera.model.CameraEffectId
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import java.util.AbstractMap
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
internal object NightCaptureSubModeModule {
    @Provides
    @IntoSet
    fun provideNightCaptureSubModeEntry(
        @ApplicationContext context: Context,
        @NightModeCompatibleCameraEffects
        compatibleCameraEffects: Set<@JvmSuppressWildcards CameraEffectId>
    ): Map.Entry<
        CaptureSubModeFeatureKey,
        @JvmSuppressWildcards Provider<CaptureSubModeProvider>
        > =
        AbstractMap.SimpleImmutableEntry(
            NightCaptureSubModeKey,
            Provider { NightCaptureSubModeProvider(context, compatibleCameraEffects) }
        )
}

/** Declares the [NightModeCompatibleCameraEffects] set so that it is empty unless an app adds to it. */
@Module
@InstallIn(SingletonComponent::class)
internal interface NightModeCompatibleCameraEffectsModule {
    @Multibinds
    @NightModeCompatibleCameraEffects
    fun nightModeCompatibleCameraEffects(): Set<CameraEffectId>
}
