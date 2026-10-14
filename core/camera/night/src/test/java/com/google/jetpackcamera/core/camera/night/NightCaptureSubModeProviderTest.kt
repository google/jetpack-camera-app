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
package com.google.jetpackcamera.core.camera.night

import android.content.Context
import android.os.Build
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.concurrent.futures.await
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S_V2])
class NightCaptureSubModeProviderTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val provider = NightCaptureSubModeProvider(context)

    @Test
    fun descriptor_hasImageOnlyParentAndNightId() {
        assertThat(NightCaptureSubModeKey.id).isEqualTo(NIGHT_SUB_MODE_ID)
        assertThat(provider.descriptor.id).isEqualTo(NIGHT_SUB_MODE_ID)
        assertThat(provider.descriptor.parentCaptureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(provider.descriptor.quickSettingsTitleResId)
            .isEqualTo(R.string.quick_settings_title_night_settings)
    }

    @Test
    fun isCompatibleWith_returnsTrueOnlyForImageOnly() {
        val constraints = CameraSystemConstraints()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.IMAGE_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isTrue()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.STANDARD),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.VIDEO_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.IMAGE_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.VideoCapture
            )
        ).isFalse()

        // A selected effect is turned off by the policy rather than preventing Night Mode.
        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(
                    captureMode = CaptureMode.IMAGE_ONLY,
                    selectedCameraEffect = CameraEffectId("custom_effect")
                ),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isTrue()
    }

    @Test
    fun featurePolicy_enforcesSdrStabilizationOffPreservesImageFormatAndExcludesLowLightBoost() {
        assertThat(provider.featurePolicy.captureMode).isNull()
        assertThat(provider.featurePolicy.imageFormat).isNull()
        assertThat(provider.featurePolicy.stabilizationMode).isEqualTo(
            SettingConfig(
                defaultValue = StabilizationMode.OFF,
                visibility = OptionVisibility.Hidden
            )
        )

        val restrictedFromLlb = provider.featurePolicy.enforceRestrictions(
            CameraAppSettings(
                captureMode = CaptureMode.IMAGE_ONLY,
                dynamicRange = DynamicRange.HLG10,
                imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
                stabilizationMode = StabilizationMode.ON,
                flashMode = FlashMode.LOW_LIGHT_BOOST
            )
        )
        assertThat(restrictedFromLlb.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(restrictedFromLlb.dynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(restrictedFromLlb.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(restrictedFromLlb.stabilizationMode).isEqualTo(StabilizationMode.OFF)
        assertThat(restrictedFromLlb.flashMode).isEqualTo(FlashMode.OFF)

        assertThat(
            CameraFeaturePolicy(
                imageFormat = SettingConfig(defaultValue = ImageOutputFormat.JPEG_ULTRA_HDR)
            ).isCompatibleWith(provider.featurePolicy)
        ).isTrue()

        val restrictedWithFlashOn = provider.featurePolicy.enforceRestrictions(
            CameraAppSettings(
                captureMode = CaptureMode.IMAGE_ONLY,
                flashMode = FlashMode.ON
            )
        )
        assertThat(restrictedWithFlashOn.flashMode).isEqualTo(FlashMode.ON)

        val restrictedWithFlashAuto = provider.featurePolicy.enforceRestrictions(
            CameraAppSettings(
                captureMode = CaptureMode.IMAGE_ONLY,
                flashMode = FlashMode.AUTO
            )
        )
        assertThat(restrictedWithFlashAuto.flashMode).isEqualTo(FlashMode.AUTO)
    }

    @Test
    fun featurePolicy_locksCameraEffectToNone() {
        val cameraEffect = provider.featurePolicy.cameraEffect

        assertThat(cameraEffect?.defaultValue).isEqualTo(NONE_EFFECT_ID)
        assertThat(cameraEffect?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun compatibleCameraEffects_areAllowedByPolicyAndCompatibility() {
        val compatibleEffect = CameraEffectId("compatible_effect")
        val providerWithEffect = NightCaptureSubModeProvider(context, setOf(compatibleEffect))
        val constraints = CameraSystemConstraints()

        assertThat(providerWithEffect.featurePolicy.cameraEffect).isEqualTo(
            SettingConfig(
                defaultValue = NONE_EFFECT_ID,
                visibility = OptionVisibility.Only(NONE_EFFECT_ID, compatibleEffect)
            )
        )
        assertThat(
            providerWithEffect.isCompatibleWith(
                settings = CameraAppSettings(
                    captureMode = CaptureMode.IMAGE_ONLY,
                    selectedCameraEffect = compatibleEffect
                ),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isTrue()
        assertThat(
            providerWithEffect.isCompatibleWith(
                settings = CameraAppSettings(
                    captureMode = CaptureMode.IMAGE_ONLY,
                    selectedCameraEffect = CameraEffectId("other_effect")
                ),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isTrue()

        // The compatible effect stays on; any other effect is turned off.
        val policy = providerWithEffect.featurePolicy
        assertThat(
            policy.enforceRestrictions(
                CameraAppSettings(selectedCameraEffect = compatibleEffect)
            ).selectedCameraEffect
        ).isEqualTo(compatibleEffect)
        assertThat(
            policy.enforceRestrictions(
                CameraAppSettings(selectedCameraEffect = CameraEffectId("other_effect"))
            ).selectedCameraEffect
        ).isEqualTo(NONE_EFFECT_ID)
    }

    @Test
    fun featurePolicy_whenHostLocksAnEffect_isIncompatibleUnlessAllowed() {
        val effect = CameraEffectId("locked_effect")
        val hostPolicy = CameraFeaturePolicy(
            cameraEffect = SettingConfig(effect, OptionVisibility.Hidden)
        )

        assertThat(hostPolicy.isCompatibleWith(provider.featurePolicy)).isFalse()
        assertThat(
            hostPolicy.isCompatibleWith(
                NightCaptureSubModeProvider(context, setOf(effect)).featurePolicy
            )
        ).isTrue()
    }

    @Test
    fun isSupportedAndSessionBinding_whenExtensionUnavailable_returnsFalseAndThrows() = runTest {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val cameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

        assertThat(provider.isSupported(context, cameraProvider, cameraInfo)).isFalse()
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                provider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
            }
        }
    }

    @Test
    fun isSupportedAndSessionBinding_whenExtensionAvailable_returnsNightEnabledSelector() =
        runTest {
            val nightSelector = CameraSelector.Builder().build()
            val supportedProvider = NightCaptureSubModeProvider(
                applicationContext = context,
                isNightAvailable = { _, selector ->
                    selector === CameraSelector.DEFAULT_BACK_CAMERA
                },
                enableNightSelector = { _, _ -> nightSelector }
            )
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val backCameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

            assertThat(supportedProvider.isSupported(context, cameraProvider, backCameraInfo))
                .isTrue()
            assertThat(
                supportedProvider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
            ).isSameInstanceAs(nightSelector)
            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking {
                    supportedProvider.sessionBinding.transformCameraSelector(
                        cameraProvider,
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    )
                }
            }
        }

    @Test
    fun isSupportedAndSessionBinding_whenQueryThrows_isSupportedFalseAndBindingPropagates() =
        runTest {
            val throwingProvider = NightCaptureSubModeProvider(
                applicationContext = context,
                isNightAvailable = { _, _ -> throw IllegalStateException("Extension failure") },
                enableNightSelector = { _, selector -> selector }
            )
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val backCameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

            assertThat(
                throwingProvider.isSupported(context, cameraProvider, backCameraInfo)
            ).isFalse()
            assertThrows(IllegalStateException::class.java) {
                kotlinx.coroutines.runBlocking {
                    throwingProvider.sessionBinding.transformCameraSelector(
                        cameraProvider,
                        CameraSelector.DEFAULT_BACK_CAMERA
                    )
                }
            }

            val cancellingProvider = NightCaptureSubModeProvider(
                applicationContext = context,
                isNightAvailable = { _, _ ->
                    throw kotlinx.coroutines.CancellationException("Cancelled")
                },
                enableNightSelector = { _, selector -> selector }
            )
            val isSupportedCancelled = try {
                cancellingProvider.isSupported(context, cameraProvider, backCameraInfo)
            } catch (_: kotlinx.coroutines.CancellationException) {
                true
            }
            assertThat(isSupportedCancelled).isTrue()

            val sessionBindingCancelled = try {
                cancellingProvider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_BACK_CAMERA
                ) !== CameraSelector.DEFAULT_BACK_CAMERA
            } catch (_: kotlinx.coroutines.CancellationException) {
                true
            }
            assertThat(sessionBindingCancelled).isTrue()
        }

    @Test
    fun getExtensionsManager_concurrentCalls_initializesManagerOnlyOnce() = runTest {
        val factoryInvocations = AtomicInteger(0)
        val factoryGate = CompletableDeferred<Unit>()
        val nightSelector = CameraSelector.Builder().build()
        val cachingProvider = NightCaptureSubModeProvider(
            applicationContext = context,
            isNightAvailable = { _, _ -> true },
            enableNightSelector = { _, _ -> nightSelector },
            extensionsManagerFactory = { cameraProvider ->
                factoryInvocations.incrementAndGet()
                // Hold the first initialization until every caller has started.
                factoryGate.await()
                ExtensionsManager.getInstanceAsync(context, cameraProvider).await()
            }
        )
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val backCameraInfo = fakeCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

        val jobs = listOf(
            async { cachingProvider.isSupported(context, cameraProvider, backCameraInfo) },
            async { cachingProvider.isSupported(context, cameraProvider, backCameraInfo) },
            async {
                cachingProvider.sessionBinding.transformCameraSelector(
                    cameraProvider,
                    CameraSelector.DEFAULT_BACK_CAMERA
                )
            }
        )
        // All callers are now waiting; only one may be initializing the manager.
        runCurrent()
        assertThat(factoryInvocations.get()).isEqualTo(1)

        factoryGate.complete(Unit)
        jobs.awaitAll()

        assertThat(factoryInvocations.get()).isEqualTo(1)
    }

    private fun fakeCameraInfo(selector: CameraSelector): CameraInfo = Proxy.newProxyInstance(
        CameraInfo::class.java.classLoader,
        arrayOf(CameraInfo::class.java)
    ) { proxy, method, args ->
        when (method.name) {
            "getCameraSelector" -> selector
            "equals" -> proxy === args?.firstOrNull()
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "FakeCameraInfo($selector)"
            else -> throw UnsupportedOperationException("Unexpected call: ${method.name}")
        }
    } as CameraInfo
}
