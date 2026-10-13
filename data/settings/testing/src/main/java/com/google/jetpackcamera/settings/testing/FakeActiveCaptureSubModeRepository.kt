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
package com.google.jetpackcamera.settings.testing

import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.settings.ActiveCaptureSubModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A fake implementation of [ActiveCaptureSubModeRepository] whose active sub-mode can be set
 * during a test.
 *
 * @param initialActiveCaptureSubModeId The sub-mode [activeCaptureSubModeId] starts with.
 */
class FakeActiveCaptureSubModeRepository(
    initialActiveCaptureSubModeId: CaptureSubModeId = CaptureSubModeId.DEFAULT
) : ActiveCaptureSubModeRepository {
    private val _activeCaptureSubModeId = MutableStateFlow(initialActiveCaptureSubModeId)
    override val activeCaptureSubModeId: StateFlow<CaptureSubModeId> =
        _activeCaptureSubModeId.asStateFlow()

    /**
     * Sets the [CaptureSubModeId] emitted by [activeCaptureSubModeId].
     */
    fun setActiveCaptureSubModeId(activeCaptureSubModeId: CaptureSubModeId) {
        _activeCaptureSubModeId.value = activeCaptureSubModeId
    }
}
