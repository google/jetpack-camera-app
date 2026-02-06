/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture

/**
 * A helper class that debounces events, preventing multiple rapid-fire executions.
 */
internal class EventDebouncer {
    private val now: Long
        get() = System.currentTimeMillis()

    private var lastEventTimeMs: Long = 0

    /**
     * Processes an event, executing it only if a specified duration has passed since the last
     * event. This prevents multiple rapid-fire executions of the same event.
     *
     * @param event The lambda function representing the event to be executed.
     */
    fun processEvent(event: () -> Unit) {
        if (now - lastEventTimeMs >= DURATION_BETWEEN_CLICKS_MS) {
            event.invoke()
        }
        lastEventTimeMs = now
    }

    companion object {
        private const val DURATION_BETWEEN_CLICKS_MS = 300L
    }
}
