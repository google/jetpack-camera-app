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
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.night.NightCaptureSubModeKey
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeFeatureKey
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.settings.model.OptionVisibility
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import javax.inject.Provider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S_V2])
class NightCaptureSubModeModuleTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun nightProvider_withoutCompatibleEffects_hidesCameraEffect() {
        val component = DaggerNightCaptureSubModeModuleTest_DefaultComponent.factory()
            .create(context)

        val provider = component.nightProvider()

        assertThat(provider.featurePolicy.cameraEffect?.visibility)
            .isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun nightProvider_withElementsIntoSetEffects_allowsThoseEffects() {
        val component = DaggerNightCaptureSubModeModuleTest_ContributedEffectsComponent.factory()
            .create(context)

        val provider = component.nightProvider()

        assertThat(provider.featurePolicy.cameraEffect?.visibility)
            .isEqualTo(OptionVisibility.Only(setOf(NONE_EFFECT_ID, TEST_EFFECT_ID)))
    }

    private fun TestComponent.nightProvider(): CaptureSubModeProvider =
        captureSubModeEntries().single { it.key == NightCaptureSubModeKey }.value.get()

    /** Contributes an effect the way an app would, as documented on the qualifier. */
    @Module
    @InstallIn(SingletonComponent::class)
    object ContributedEffectsModule {
        @Provides
        @ElementsIntoSet
        @NightModeCompatibleCameraEffects
        fun provideNightModeEffects(): Set<CameraEffectId> = setOf(TEST_EFFECT_ID)
    }

    interface TestComponent {
        fun captureSubModeEntries():
            Set<Map.Entry<CaptureSubModeFeatureKey, Provider<CaptureSubModeProvider>>>
    }

    @Component(
        modules = [NightCaptureSubModeModule::class, NightModeCompatibleCameraEffectsModule::class]
    )
    interface DefaultComponent : TestComponent {
        @Component.Factory
        interface Factory {
            fun create(@BindsInstance @ApplicationContext context: Context): DefaultComponent
        }
    }

    @Component(
        modules = [
            NightCaptureSubModeModule::class,
            NightModeCompatibleCameraEffectsModule::class,
            ContributedEffectsModule::class
        ]
    )
    interface ContributedEffectsComponent : TestComponent {
        @Component.Factory
        interface Factory {
            fun create(
                @BindsInstance @ApplicationContext context: Context
            ): ContributedEffectsComponent
        }
    }

    private companion object {
        val TEST_EFFECT_ID = CameraEffectId("test_effect")
    }
}
