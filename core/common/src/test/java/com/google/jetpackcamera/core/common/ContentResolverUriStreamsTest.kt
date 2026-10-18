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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileNotFoundException
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class ContentResolverUriStreamsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var uriStreams: UriStreams

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        uriStreams = ContentResolverUriStreams(context)
    }

    @Test
    fun contentUri_servedByThisApp_isRejected() {
        registerOwnProvider(OWN_AUTHORITY)
        val uri = Uri.parse("content://$OWN_AUTHORITY/files/secret.txt")

        assertAllOpensRejected(uri)
    }

    @Test
    fun contentUri_servedByThisAppWithUserPrefix_isRejected() {
        registerOwnProvider(OWN_AUTHORITY)
        val uri = Uri.parse("content://0@$OWN_AUTHORITY/files/secret.txt")

        assertAllOpensRejected(uri)
    }

    @Test
    fun contentUri_servedByAnotherApp_isOpened() {
        val backingFile = tempFolder.newFile("shared.txt").apply { writeText(CONTENT) }
        Robolectric.setupContentProvider(FileBackedProvider::class.java, OTHER_AUTHORITY)
            .backingFile = backingFile
        val uri = Uri.parse("content://$OTHER_AUTHORITY/shared.txt")

        val text = uriStreams.openInputStream(uri)?.use { it.reader().readText() }

        assertThat(text).isEqualTo(CONTENT)
    }

    @Test
    fun fileUri_inAppDataDir_isRejected() {
        val privateFile = File(context.filesDir, "secret.txt").apply { writeText(CONTENT) }

        assertAllOpensRejected(Uri.fromFile(privateFile))
    }

    @Test
    fun fileUri_resolvingIntoAppDataDir_isRejected() {
        val privateFile = File(context.filesDir, "secret.txt").apply { writeText(CONTENT) }
        val depth = tempFolder.root.canonicalPath.count { it == File.separatorChar }
        val traversal = "/..".repeat(depth) + privateFile.canonicalPath
        val uri = Uri.fromFile(File(tempFolder.root, traversal))

        assertAllOpensRejected(uri)
    }

    @Test
    fun fileUri_outsideAppStorage_isOpened() {
        val externalFile = tempFolder.newFile("capture.jpg")
        val uri = Uri.fromFile(externalFile)

        uriStreams.openOutputStream(uri)?.use { it.write(CONTENT.toByteArray()) }

        assertThat(externalFile.readText()).isEqualTo(CONTENT)
    }

    @Test
    fun uriWithUnsupportedScheme_isRejected() {
        assertAllOpensRejected(Uri.parse("https://example.com/capture.jpg"))
        assertAllOpensRejected(Uri.parse("capture.jpg"))
    }

    private fun assertAllOpensRejected(uri: Uri) {
        assertThrows(FileNotFoundException::class.java) { uriStreams.openInputStream(uri) }
        assertThrows(FileNotFoundException::class.java) { uriStreams.openOutputStream(uri) }
        assertThrows(FileNotFoundException::class.java) {
            uriStreams.openFileDescriptor(uri, "rw")
        }
    }

    private fun registerOwnProvider(authority: String) {
        val providerInfo = ProviderInfo().apply {
            this.authority = authority
            packageName = context.packageName
            name = FileBackedProvider::class.java.name
        }
        shadowOf(context.packageManager).addOrUpdateProvider(providerInfo)
    }

    /** A [ContentProvider] that serves a single file for any URI. */
    class FileBackedProvider : ContentProvider() {
        var backingFile: File? = null

        override fun onCreate(): Boolean = true

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor =
            ParcelFileDescriptor.open(
                backingFile ?: throw FileNotFoundException(uri.toString()),
                ParcelFileDescriptor.parseMode(mode)
            )

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int = 0
    }

    private companion object {
        const val OWN_AUTHORITY = "com.google.jetpackcamera.core.common.test.own"
        const val OTHER_AUTHORITY = "com.example.other.provider"
        const val CONTENT = "content"
    }
}
