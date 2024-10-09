/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.settings

import com.google.jetpackcamera.settings.model.SystemConstraints
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConstraintsRepository {
    val systemConstraints: StateFlow<SystemConstraints?>
}

interface SettableConstraintsRepository : ConstraintsRepository {
    fun updateSystemConstraints(systemConstraints: SystemConstraints)
}

class SettableConstraintsRepositoryImpl @Inject constructor() : SettableConstraintsRepository {

    private val _systemConstraints = MutableStateFlow<SystemConstraints?>(null)
    override val systemConstraints: StateFlow<SystemConstraints?>
        get() = _systemConstraints.asStateFlow()

    override fun updateSystemConstraints(systemConstraints: SystemConstraints) {
        _systemConstraints.value = systemConstraints
    }
}
