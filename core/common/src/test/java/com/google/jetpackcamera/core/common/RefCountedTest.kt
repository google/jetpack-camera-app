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

import com.google.common.truth.Truth.assertThat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class RefCountedTest {

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
    }

    @Test
    fun onRelease_calledAfterRelease() {
        var onReleaseCalled = false
        val refCounted = RefCounted<Unit> {
            onReleaseCalled = true
        }.also {
            it.initialize(Unit)
        }

        refCounted.release()

        assertThat(onReleaseCalled).isTrue()
    }

    @Test
    fun acquireBeforeInitialize_throwsException() {
        val refCounted = RefCounted<Unit> {}
        assertThrows(IllegalStateException::class.java) {
            refCounted.acquire()
        }
    }

    @Test
    fun releaseBeforeInitialize_throwsException() {
        val refCounted = RefCounted<Unit> {}
        assertThrows(IllegalStateException::class.java) {
            refCounted.release()
        }
    }

    @Test
    fun releaseCalledMoreTimesThanAcquire_throwsException() {
        val refCounted = RefCounted<Unit> {}
        refCounted.initialize(Unit)
        refCounted.release()

        assertThrows(IllegalStateException::class.java) {
            refCounted.release()
        }
    }

    @Test
    fun acquireAfterRelease_returnsNull() {
        val refCounted = RefCounted<Unit> {}
        refCounted.initialize(Unit)
        refCounted.release()

        assertThat(refCounted.acquire()).isNull()
    }

    @Test
    fun acquireAfterInitialize_returnsValue() {
        val value = Object()
        val refCounted = RefCounted<Any> {}
        refCounted.initialize(value)

        assertThat(refCounted.acquire()).isEqualTo(value)
    }

    @Test
    fun acquiresWithMatchedRelease_callsOnRelease() = runBlocking {
        val onReleaseCalled = atomic(false)
        val refCounted = RefCounted<Unit> {
            onReleaseCalled.value = true
        }.also {
            it.initialize(Unit)
        }

        // Run many acquire/release pairs in parallel
        // Wrap in `coroutineScope` to ensure all children coroutines
        // have finished before continuing
        coroutineScope {
            for (i in 1..1000) {
                launch(Dispatchers.IO) {
                    refCounted.acquire()
                    delay(5)
                    refCounted.release()
                }
            }
        }

        val onReleaseCalledBeforeFinalRelease = onReleaseCalled.value

        // Call final release to match initialize()
        refCounted.release()

        assertThat(onReleaseCalledBeforeFinalRelease).isFalse()
        assertThat(onReleaseCalled.value).isTrue()
    }
}
