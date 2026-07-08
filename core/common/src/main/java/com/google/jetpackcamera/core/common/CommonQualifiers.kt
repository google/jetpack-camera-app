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
package com.google.jetpackcamera.core.common

import javax.inject.Qualifier

/**
 * Qualifier for the default file path generator.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultFilePathGenerator

/**
 * Qualifier for the default capture mode override in LocalSettingsRepository.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultCaptureModeOverride

/**
 * Qualifier for the default save mode.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultSaveMode

/**
 * Qualifier for the default coroutine dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Qualifier for the IO coroutine dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IODispatcher

/**
 * Qualifier for the default application-level coroutine scope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultCoroutineScope
