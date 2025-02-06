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
package com.google.jetpackcamera.utils

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.net.URLConnection
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

const val APP_START_TIMEOUT_MILLIS = 10_000L
const val SCREEN_FLASH_OVERLAY_TIMEOUT_MILLIS = 5_000L
const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 5_000L
const val VIDEO_CAPTURE_TIMEOUT_MILLIS = 5_000L
const val VIDEO_DURATION_MILLIS = 2_000L
const val MESSAGE_DISAPPEAR_TIMEOUT_MILLIS = 15_000L
const val COMPONENT_PACKAGE_NAME = "com.google.jetpackcamera"
const val COMPONENT_CLASS = "com.google.jetpackcamera.MainActivity"
inline fun <reified T : Activity> runMediaStoreAutoDeleteScenarioTest(
    mediaUri: Uri,
    filePrefix: String = "",
    expectedNumFiles: Int = 1,
    fileWaitTimeoutMs: Duration = 10.seconds,
    fileObserverContext: CoroutineContext = Dispatchers.IO,
    crossinline block: ActivityScenario<T>.() -> Unit
) = runBlocking {
    val debugTag = "MediaStoreAutoDelete"
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val insertedMediaStoreEntries = mutableMapOf<String, Uri>()
    val observeFilesJob = launch(fileObserverContext) {
        mediaStoreInsertedFlow(
            mediaUri = mediaUri,
            instrumentation = instrumentation,
            filePrefix = filePrefix
        ).take(expectedNumFiles)
            .collect {
                Log.d(debugTag, "Discovered new media store file: ${it.first}")
                insertedMediaStoreEntries[it.first] = it.second
            }
    }

    var succeeded = false
    try {
        runScenarioTest(block = block)
        succeeded = true
    } finally {
        withContext(NonCancellable) {
            if (!succeeded ||
                withTimeoutOrNull(fileWaitTimeoutMs) {
                    // Wait for normal completion with timeout
                    observeFilesJob.join()
                } == null
            ) {
                // If the test didn't succeed, or we've timed out waiting for files,
                // cancel file observer and ensure job is complete
                observeFilesJob.cancelAndJoin()
            }

            val detectedNumFiles = insertedMediaStoreEntries.size
            // Delete all inserted files that we know about at this point
            insertedMediaStoreEntries.forEach {
                Log.d(debugTag, "Deleting media store file: $it")
                val deletedRows = instrumentation.targetContext.contentResolver.delete(
                    it.value,
                    null,
                    null
                )
                if (deletedRows > 0) {
                    Log.d(debugTag, "Deleted $deletedRows files")
                } else {
                    Log.e(debugTag, "Failed to delete ${it.key}")
                }
            }

            if (succeeded) {
                assertWithMessage("Expected number of saved files does not match detected number")
                    .that(detectedNumFiles).isEqualTo(expectedNumFiles)
            }
        }
    }
}

inline fun <reified T : Activity> runScenarioTest(
    crossinline block: ActivityScenario<T>.() -> Unit
) {
    ActivityScenario.launch(T::class.java).use { scenario ->
        scenario.apply(block)
    }
}

inline fun <reified T : Activity> runScenarioTestForResult(
    intent: Intent,
    crossinline block: ActivityScenario<T>.() -> Unit
): Instrumentation.ActivityResult {
    ActivityScenario.launchActivityForResult<T>(intent).use { scenario ->
        scenario.apply(block)
        return runBlocking { scenario.pollResult() }
    }
}

// Workaround for https://github.com/android/android-test/issues/676
suspend inline fun <reified T : Activity> ActivityScenario<T>.pollResult(
    // Choose timeout to match
    // https://github.com/android/android-test/blob/67fa7cb12b9a14dc790b75947f4241c3063e80dc/runner/monitor/java/androidx/test/internal/platform/app/ActivityLifecycleTimeout.java#L22
    timeout: Duration = 45.seconds
): Instrumentation.ActivityResult = withTimeoutOrNull(timeout) {
    // Poll for the state to be destroyed before we return the result
    while (state != Lifecycle.State.DESTROYED) {
        delay(100)
    }
    checkNotNull(result)
} ?: run {
    throw TimeoutException(
        "Timed out while waiting for activity result. Waited $timeout."
    )
}

fun getTestUri(directoryPath: String, timeStamp: Long, suffix: String): Uri = Uri.fromFile(
    File(
        directoryPath,
        "$timeStamp.$suffix"
    )
)

fun deleteFilesInDirAfterTimestamp(
    directoryPath: String,
    instrumentation: Instrumentation,
    timeStamp: Long
): Boolean {
    var hasDeletedFile = false
    for (file in File(directoryPath).listFiles() ?: emptyArray()) {
        if (file.lastModified() >= timeStamp) {
            file.delete()
            if (file.exists()) {
                file.canonicalFile.delete()
                if (file.exists()) {
                    instrumentation.targetContext.applicationContext.deleteFile(file.name)
                }
            }
            hasDeletedFile = true
        }
    }
    return hasDeletedFile
}

fun doesImageFileExist(uri: Uri, prefix: String): Boolean {
    val file = uri.path?.let { File(it) }
    if (file?.exists() == true) {
        val mimeType = URLConnection.guessContentTypeFromName(uri.path)
        return mimeType != null && mimeType.startsWith(prefix)
    }
    return false
}

fun getSingleImageCaptureIntent(uri: Uri, action: String): Intent {
    val intent = Intent(action)
    intent.setComponent(
        ComponentName(
            COMPONENT_PACKAGE_NAME,
            COMPONENT_CLASS
        )
    )
    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
    return intent
}

fun getMultipleImageCaptureIntent(uriStrings: ArrayList<String>?, action: String): Intent {
    val intent = Intent(action)
    intent.setComponent(
        ComponentName(
            COMPONENT_PACKAGE_NAME,
            COMPONENT_CLASS
        )
    )
    intent.putStringArrayListExtra(MediaStore.EXTRA_OUTPUT, uriStrings)
    return intent
}

fun stateDescriptionMatches(expected: String?) = SemanticsMatcher("stateDescription is $expected") {
    SemanticsProperties.StateDescription in it.config &&
        (it.config[SemanticsProperties.StateDescription] == expected)
}
