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
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.google.jetpackcamera.MainActivity
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
import org.junit.Assert.fail
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

val isRanchuApi28: Boolean
    get() = Build.HARDWARE == "ranchu" && Build.VERSION.SDK_INT == 28

val compatMainActivityExtras: Bundle?
    get() = if (isRanchuApi28) {
        // The GMD API 28 emulator's PackageInfo reports it has front and back cameras, but
        // GMD is only configured for a back camera. This causes CameraX to take a long time
        // to initialize. Set the device to use single lens mode to work around this issue.
        Bundle().apply {
            putString(MainActivity.KEY_DEBUG_SINGLE_LENS_MODE, "back")
        }
    } else {
        null
    }

val debugExtra: Bundle = Bundle().apply { putBoolean("KEY_DEBUG_MODE", true) }
val cacheExtra: Bundle = Bundle().apply { putBoolean("KEY_REVIEW_AFTER_CAPTURE", true) }

const val DEFAULT_TIMEOUT_MILLIS = 5_000L
const val APP_START_TIMEOUT_MILLIS = 20_000L
const val ELAPSED_TIME_TEXT_TIMEOUT_MILLIS = 45_000L
const val SCREEN_FLASH_OVERLAY_TIMEOUT_MILLIS = 5_000L
const val IMAGE_CAPTURE_TIMEOUT_MILLIS = 45_000L
const val VIDEO_CAPTURE_TIMEOUT_MILLIS = 15_000L
const val SAVE_MEDIA_TIMEOUT_MILLIS = 15_000L
const val IMAGE_WELL_LOAD_TIMEOUT_MILLIS = 10_000L

const val VIDEO_DURATION_MILLIS = 3_000L
const val MESSAGE_DISAPPEAR_TIMEOUT_MILLIS = 15_000L
const val FOCUS_METERING_INDICATOR_TIMEOUT_MILLIS = 10_000L
const val FILE_PREFIX = "JCA"
const val VIDEO_PREFIX = "video"
const val IMAGE_PREFIX = "image"
const val COMPONENT_PACKAGE_NAME = "com.google.jetpackcamera"
const val COMPONENT_CLASS = "com.google.jetpackcamera.MainActivity"
private const val TAG = "UiTestUtil"

inline fun runMainActivityMediaStoreAutoDeleteScenarioTest(
    mediaUri: Uri,
    filePrefix: String = "",
    expectedNumFiles: Int = 1,
    fileWaitTimeoutMs: Duration = 10.seconds,
    fileObserverContext: CoroutineContext = Dispatchers.IO,
    extras: Bundle? = null,
    crossinline block: ActivityScenario<MainActivity>.() -> Unit
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
        runMainActivityScenarioTest(extras = extras, block = block)
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

inline fun runMainActivityScenarioTest(
    extras: Bundle? = null,
    crossinline block: ActivityScenario<MainActivity>.() -> Unit
) = runScenarioTest<MainActivity>(extras ?: compatMainActivityExtras, block)

inline fun <reified T : Activity> runScenarioTest(
    activityExtras: Bundle? = null,
    crossinline block: ActivityScenario<T>.() -> Unit
) {
    val intent = activityExtras?.let {
        Intent.makeMainActivity(
            ComponentName(
                InstrumentationRegistry.getInstrumentation().targetContext,
                T::class.java
            )
        ).putExtras(it)
    }

    if (intent != null) {
        ActivityScenario.launch<T>(intent).use { scenario ->
            scenario.apply(block)
        }
    } else {
        ActivityScenario.launch(T::class.java).use { scenario ->
            scenario.apply(block)
        }
    }
}

inline fun runMainActivityScenarioTestForResult(
    intent: Intent,
    extras: Bundle? = null,
    crossinline block: ActivityScenario<MainActivity>.() -> Unit
): Instrumentation.ActivityResult {
    val activityExtras = compatMainActivityExtras?.apply { extras?.let { putAll(it) } } ?: extras
    return runScenarioTestForResult<MainActivity>(intent, activityExtras, block)
}

inline fun <reified T : Activity> runScenarioTestForResult(
    intent: Intent,
    activityExtras: Bundle? = null,
    crossinline block: ActivityScenario<T>.() -> Unit
): Instrumentation.ActivityResult {
    activityExtras?.let { intent.putExtras(it) }
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

// Helper function to check if a MediaStore entry exists (Source of Truth)
fun mediaStoreEntryExistsAfterTimestamp(
    instrumentation: Instrumentation,
    mediaUri: Uri, // MediaStore.Images.Media.EXTERNAL_CONTENT_URI or Video.Media...
    timestamp: Long
): Boolean {
    val contentResolver = instrumentation.targetContext.contentResolver

    // MediaStore.MediaColumns.DATE_ADDED is stored in seconds (s), convert ms to s
    val timestampInSeconds = timestamp / 1000

    // Check if any file was added to this collection since the test started
    val selection = "${MediaStore.MediaColumns.DATE_ADDED} >= ?"
    val selectionArgs = arrayOf(timestampInSeconds.toString())

    val cursor = contentResolver.query(
        mediaUri,
        arrayOf(MediaStore.MediaColumns._ID), // Querying for just the ID is efficient
        selection,
        selectionArgs,
        null // No sorting needed
    )

    // Return true if the cursor has any entries
    return cursor?.use { it.count > 0 } ?: false
}

/**
 * @return - true if all eligible files were successfully deleted. False otherwise
 */
@CanIgnoreReturnValue
fun deleteFilesInDirAfterTimestamp(
    directoryPath: String,
    instrumentation: Instrumentation,
    timeStamp: Long
): Boolean {
    val fileStatus = mutableMapOf<String, Boolean>()
    for (file in File(directoryPath).listFiles() ?: emptyArray()) {
        if (file.lastModified() >= timeStamp) {
            fileStatus.put(file.name, file.delete())
            if (file.exists()) {
                fileStatus.put(file.name, file.canonicalFile.delete())
                if (file.exists()) {
                    fileStatus.put(
                        file.name,
                        instrumentation.targetContext.applicationContext.deleteFile(file.name)
                    )
                }
            }
        }
    }
    return fileStatus.keys.all { fileStatus[it] ?: false }
}

fun doesFileExist(uri: Uri): Boolean = uri.path?.let { File(it) }?.exists() == true

fun doesMediaExist(uri: Uri, prefix: String): Boolean {
    require(prefix == IMAGE_PREFIX || prefix == VIDEO_PREFIX) { "Uknown prefix: $prefix" }
    return if (prefix == IMAGE_PREFIX) {
        doesImageExist(uri)
    } else {
        doesVideoExist(uri, prefix)
    }
}

private fun doesImageExist(uri: Uri): Boolean {
    val bitmap = uri.path?.let { path -> BitmapFactory.decodeFile(path) }
    val mimeType = URLConnection.guessContentTypeFromName(uri.path)
    return mimeType != null && mimeType.startsWith(IMAGE_PREFIX) && bitmap != null
}

private fun doesVideoExist(
    uri: Uri,
    prefix: String,
    checkAudio: Boolean = false,
    durationMs: Long? = null
): Boolean {
    require(prefix == VIDEO_PREFIX) {
        "doesVideoExist() only works for videos. Can't handle prefix: $prefix"
    }

    if (!doesFileExist(uri)) {
        return false
    }
    return MediaMetadataRetriever().useAndRelease {
        it.setDataSource(uri.path)

        it.getMimeType().startsWith(prefix) &&
            it.hasVideo() &&
            (!checkAudio || it.hasAudio()) &&
            (durationMs == null || it.getDurationMs() == durationMs)
    } == true
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

/**
 * Rule to specify test methods that will have permissions granted prior to running
 *
 * @param permissions the permissions to be granted
 * @param targetTestNames the names of the tests that this rule will apply to
 */
class IndividualTestGrantPermissionRule(
    private val permissions: Array<String>,
    private val targetTestNames: Array<String>
) : TestRule {
    private lateinit var wrappedRule: GrantPermissionRule

    override fun apply(base: Statement, description: Description): Statement {
        for (targetName in targetTestNames) {
            if (description.methodName == targetName) {
                wrappedRule = GrantPermissionRule.grant(*permissions)
                return wrappedRule.apply(base, description)
            }
        }
        // If no match, return the base statement without granting permissions
        return base
    }
}

// functions for interacting with system permission dialog
fun UiDevice.askEveryTimeDialog() {
    if (Build.VERSION.SDK_INT >= 30) {
        Log.d(TAG, "Searching for Allow Once Button...")

        val askPermission = this.findObjectById(
            resId = "com.android.permissioncontroller:id/permission_allow_one_time_button"
        )

        Log.d(TAG, "Clicking Allow Once Button")

        askPermission?.click()
    }
}

/**
 *  Clicks ALLOW option on an open permission dialog
 */
fun UiDevice.grantPermissionDialog() {
    if (Build.VERSION.SDK_INT >= 23) {
        Log.d(TAG, "Searching for Allow Button...")

        val allowPermission = this.findObjectById(
            resId = when {
                Build.VERSION.SDK_INT <= 29 ->
                    "com.android.packageinstaller:id/permission_allow_button"

                else ->
                    "com.android.permissioncontroller:id/permission_allow_foreground_only_button"
            }
        )
        Log.d(TAG, "Clicking Allow Button")

        allowPermission?.click()
    }
}

/**
 * Clicks the DENY option on an open permission dialog
 */
fun UiDevice.denyPermissionDialog() {
    if (Build.VERSION.SDK_INT >= 23) {
        Log.d(TAG, "Searching for Deny Button...")
        val denyPermission = this.findObjectById(
            resId = when {
                Build.VERSION.SDK_INT <= 29 ->
                    "com.android.packageinstaller:id/permission_deny_button"

                else -> "com.android.permissioncontroller:id/permission_deny_button"
            }
        )
        Log.d(TAG, "Clicking Deny Button")

        denyPermission?.click()
    }
}

/**
 * Finds a system button by its resource ID.
 * fails if not found
 */
private fun UiDevice.findObjectById(
    resId: String,
    timeout: Long = 10000,
    shouldFailIfNotFound: Boolean = true
): UiObject2? {
    val selector = By.res(resId)
    return if (!this.wait(Until.hasObject(selector), timeout)) {
        if (shouldFailIfNotFound) {
            fail("Could not find object with RESOURCE ID: $resId")
        }
        null
    } else {
        this.findObject(selector)
    }
}
