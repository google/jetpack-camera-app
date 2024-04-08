/*
 * Copyright (C) 2023 The Android Open Source Project
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

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger [Module] for settings data layer.
 */
@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {

    @Binds
    fun bindsSettingsRepository(
        localSettingsRepository: LocalSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    fun bindsSettableConstraintsRepository(
        settableConstraintsRepository: SettableConstraintsRepositoryImpl
    ): SettableConstraintsRepository

    /**
     * ConstraintsRepository without setter.
     *
     * This is the same instance as the singleton SettableConstraintsRepository, but does not
     * have the ability to update the constraints.
     */
    @Binds
    fun bindsConstraintsRepository(
        constraintsRepository: SettableConstraintsRepository
    ): ConstraintsRepository
}
