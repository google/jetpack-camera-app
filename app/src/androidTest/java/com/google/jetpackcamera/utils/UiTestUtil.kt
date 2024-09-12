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
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.MainActivity
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.settings.model.LensFacing
import java.io.File
import java.net.URLConnection
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

const val APP_START_TIMEOUT_MILLIS = 10_000L
const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 5_000L
const val VIDEO_CAPTURE_TIMEOUT_MILLIS = 5_000L
const val VIDEO_DURATION_MILLIS = 2_000L

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified T : Activity> runMediaStoreAutoDeleteScenarioTest(
    mediaUri: Uri,
    filePrefix: String = "JCA",
    expectedNumFiles: Int = 1,
    fileWaitTimeoutMs: Duration = 10.seconds,
    crossinline block: ActivityScenario<T>.() -> Unit
) = runBlocking {
    val debugTag = "MediaStoreAutoDelete"
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val insertedMediaStoreEntries = mutableMapOf<String, Uri>()
    val fileObserverContext: CoroutineContext = Dispatchers.IO.limitedParallelism(1)
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

    try {
        runScenarioTest(block = block)
    } finally {
        withContext(NonCancellable) {
            withTimeoutOrNull(fileWaitTimeoutMs) {
                // Wait for normal completion with timeout
                observeFilesJob.join()
            } ?: run {
                // If timed out, cancel file observer and ensure job is complete
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

            assertWithMessage("Expected number of saved files does not match detected number")
                .that(detectedNumFiles).isEqualTo(expectedNumFiles)
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

context(ActivityScenario<MainActivity>)
fun ComposeTestRule.getCurrentLensFacing(): LensFacing {
    var needReturnFromQuickSettings = false
    onNodeWithContentDescription(R.string.quick_settings_dropdown_closed_description).apply {
        if (isDisplayed()) {
            performClick()
            needReturnFromQuickSettings = true
        }
    }

    onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description).assertExists(
        "LensFacing can only be retrieved from PreviewScreen or QuickSettings screen"
    )

    try {
        return onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON).fetchSemanticsNode(
            "Flip camera button is not visible when expected."
        ).let { node ->
            node.config[SemanticsProperties.ContentDescription].any { description ->
                when (description) {
                    getResString(R.string.quick_settings_front_camera_description) ->
                        return@let LensFacing.FRONT

                    getResString(R.string.quick_settings_back_camera_description) ->
                        return@let LensFacing.BACK

                    else -> false
                }
            }
            throw AssertionError("Unable to determine lens facing from quick settings")
        }
    } finally {
        if (needReturnFromQuickSettings) {
            onNodeWithContentDescription(R.string.quick_settings_dropdown_open_description)
                .assertExists()
                .performClick()
        }
    }
}

fun getTestUri(directoryPath: String, timeStamp: Long, suffix: String): Uri {
    return Uri.fromFile(
        File(
            directoryPath,
            "$timeStamp.$suffix"
        )
    )
}

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

fun getIntent(uri: Uri, action: String): Intent {
    val intent = Intent(action)
    intent.setComponent(
        ComponentName(
            "com.google.jetpackcamera",
            "com.google.jetpackcamera.MainActivity"
        )
    )
    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
    return intent
}
