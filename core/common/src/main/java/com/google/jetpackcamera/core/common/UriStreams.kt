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

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ProviderInfo
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Opens [Uri]s that were supplied by, or are served by, another app, such as an
 * [android.provider.MediaStore.EXTRA_OUTPUT] location from a capture intent or a MediaStore entry.
 *
 * All reads and writes of such URIs go through this interface so that they share one validation
 * policy. Files that this app creates for itself, such as captures in its cache directory, should be
 * accessed with [File] APIs instead.
 *
 * The default implementation is [ContentResolverUriStreams]. Apps that embed these modules and need
 * a different policy can supply their own implementation.
 */
interface UriStreams {
    /**
     * Opens [uri] for reading.
     *
     * @return the stream, or `null` if the content provider returned no stream.
     * @throws FileNotFoundException if [uri] is rejected or cannot be opened.
     */
    @Throws(FileNotFoundException::class)
    fun openInputStream(uri: Uri): InputStream?

    /**
     * Opens [uri] for writing.
     *
     * @return the stream, or `null` if the content provider returned no stream.
     * @throws FileNotFoundException if [uri] is rejected or cannot be opened.
     */
    @Throws(FileNotFoundException::class)
    fun openOutputStream(uri: Uri): OutputStream?

    /**
     * Opens a file descriptor for [uri].
     *
     * @param mode the access mode, as accepted by [ContentResolver.openFileDescriptor].
     * @return the file descriptor, or `null` if the content provider returned none.
     * @throws FileNotFoundException if [uri] is rejected or cannot be opened.
     */
    @Throws(FileNotFoundException::class)
    fun openFileDescriptor(uri: Uri, mode: String): ParcelFileDescriptor?
}

/**
 * [UriStreams] backed by [ContentResolver].
 *
 * Before opening a URI, this rejects URIs that point at this app's own data, so that another app
 * cannot pass one of this app's URIs back to it to read or overwrite its private files. See
 * [Content resolver risks](https://developer.android.com/privacy-and-security/risks/content-resolver).
 * Specifically, it rejects:
 * - `content://` URIs served by one of this app's own content providers.
 * - `file://` URIs that resolve to a path inside this app's data directory.
 * - URIs with any other scheme.
 *
 * These checks are a baseline. Apps that need stronger guarantees can provide their own
 * [UriStreams].
 */
class ContentResolverUriStreams(context: Context) : UriStreams {
    private val context = context.applicationContext

    override fun openInputStream(uri: Uri): InputStream? =
        context.contentResolver.openInputStream(checkNotAppPrivate(uri))

    override fun openOutputStream(uri: Uri): OutputStream? =
        context.contentResolver.openOutputStream(checkNotAppPrivate(uri))

    override fun openFileDescriptor(uri: Uri, mode: String): ParcelFileDescriptor? =
        context.contentResolver.openFileDescriptor(checkNotAppPrivate(uri), mode)

    private fun checkNotAppPrivate(uri: Uri): Uri {
        when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                // An authority may carry a "userId@" prefix; the provider is named by the rest.
                val authority = uri.authority?.substringAfterLast('@')
                    ?: throw FileNotFoundException("URI has no authority: $uri")
                if (resolveContentProvider(authority)?.packageName == context.packageName) {
                    throw FileNotFoundException("URI is served by this app: $uri")
                }
            }

            ContentResolver.SCHEME_FILE -> {
                val path = uri.path ?: throw FileNotFoundException("URI has no path: $uri")
                if (isInAppDataDir(File(path))) {
                    throw FileNotFoundException("URI points to this app's data: $uri")
                }
            }

            else -> throw FileNotFoundException("Unsupported URI scheme: $uri")
        }
        return uri
    }

    // The int-flags overload is deprecated in API 33 in favor of ComponentInfoFlags, but both
    // behave the same with no flags, and this one is available on every supported API level.
    @Suppress("DEPRECATION")
    private fun resolveContentProvider(authority: String): ProviderInfo? =
        context.packageManager.resolveContentProvider(authority, 0)

    private fun isInAppDataDir(file: File): Boolean = try {
        val path = file.canonicalPath
        listOf(context.dataDir, context.createDeviceProtectedStorageContext().dataDir)
            .any { dir ->
                val dirPath = dir.canonicalPath
                path == dirPath || path.startsWith(dirPath + File.separator)
            }
    } catch (e: IOException) {
        // Fail closed: a path that cannot be resolved cannot be shown to be outside the app.
        true
    }
}
