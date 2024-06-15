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
package com.google.jetpackcamera.core.common

import android.content.Context
import android.util.Log
import androidx.tracing.Trace
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TraceManagerTag"
private const val FIRST_FRAME_TRACE = "firstFrameTrace"
private const val FIRST_FRAME_COOKIE = 12345
var isBenchmarkBuild = false

/**
 * an injectable trace manager class to facilitate starting and ending traces at various locations
 * within the app
 */
@Singleton
class TraceManager @Inject constructor(@ApplicationContext context: Context) {
    private var firstFrameTraceStatus = TraceStatus.NOT_STARTED

    // if it is benchmark build, multiple attempts to start traces is allowed in different locations.
    // especially important for "hot startup" benchmark tests
    init {
        if (context.resources.getBoolean(R.bool.is_benchmark_build)) {
            isBenchmarkBuild = true
        }
        Log.d(TAG, "is in benchmark mode: $isBenchmarkBuild")
    }

    fun beginFirstFrameTrace(cookie: Int = FIRST_FRAME_COOKIE) {
        if ((isBenchmarkBuild && firstFrameTraceStatus != TraceStatus.IN_PROGRESS) ||
            firstFrameTraceStatus == TraceStatus.NOT_STARTED
        ) {
            beginAsyncSection(FIRST_FRAME_TRACE, cookie)
            firstFrameTraceStatus = TraceStatus.IN_PROGRESS
            Log.d(TAG, "First frame trace $firstFrameTraceStatus with cookie $cookie")
        }
    }
    fun endFirstFrameTrace(cookie: Int = FIRST_FRAME_COOKIE) {
        if (firstFrameTraceStatus == TraceStatus.IN_PROGRESS) {
            endAsyncSection(FIRST_FRAME_TRACE, cookie)
            firstFrameTraceStatus = TraceStatus.COMPLETE
            Log.d(TAG, "First frame trace $firstFrameTraceStatus with cookie $cookie")
        }
    }

    private fun beginAsyncSection(sectionName: String, cookie: Int): () -> Unit {
        Trace.beginAsyncSection(sectionName, cookie)
        return { Trace.endAsyncSection(sectionName, cookie) }
    }

    private fun endAsyncSection(sectionName: String, cookie: Int): () -> Unit {
        Trace.endAsyncSection(sectionName, cookie)
        return { Trace.endAsyncSection(sectionName, cookie) }
    }
}

enum class TraceStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETE
}
