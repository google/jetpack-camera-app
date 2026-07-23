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
package com.google.jetpackcamera.di

import javax.inject.Qualifier

/**
 * Identifies the default file path generator instance used for naming and locating media output files.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultFilePathGenerator

/**
 * Identifies the fallback capture mode override when initializing repository settings.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultCaptureModeOverride

/**
 * Identifies the default media saving location policy (e.g. device storage vs app cache).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultSaveMode

/**
 * Identifies the CPU-bound [kotlinx.coroutines.CoroutineDispatcher] used for general computation.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultDispatcher

/**
 * Identifies the I/O-bound [kotlinx.coroutines.CoroutineDispatcher] used for disk and network tasks.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class IODispatcher

/**
 * Identifies the root application-level [kotlinx.coroutines.CoroutineScope] tied to SingletonComponent lifecycle.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DefaultCoroutineScope
