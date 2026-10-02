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
package com.google.jetpackcamera.permissions.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists which optional permissions have already been presented to the user, so they are not
 * prompted again across app restarts.
 */
interface PermissionsRepository {
    /**
     * Emits the [com.google.jetpackcamera.permissions.PermissionEnum] names that have been
     * presented to the user.
     */
    val requestedPermissions: Flow<Set<String>>

    /**
     * Records that a permission has been presented to the user.
     *
     * @param permissionName The [com.google.jetpackcamera.permissions.PermissionEnum.name] to
     * record.
     */
    suspend fun markPermissionRequested(permissionName: String)
}

private val Context.permissionsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jca_permissions"
)

private val KEY_REQUESTED_PERMISSIONS = stringSetPreferencesKey("requested_permissions")

/** [PermissionsRepository] backed by a Preferences [DataStore]. */
@Singleton
class DataStorePermissionsRepository @Inject constructor(
    @ApplicationContext context: Context
) : PermissionsRepository {
    private val dataStore = context.permissionsDataStore

    override val requestedPermissions: Flow<Set<String>> =
        dataStore.data.map { prefs -> prefs[KEY_REQUESTED_PERMISSIONS] ?: emptySet() }

    override suspend fun markPermissionRequested(permissionName: String) {
        dataStore.edit { prefs ->
            prefs[KEY_REQUESTED_PERMISSIONS] =
                (prefs[KEY_REQUESTED_PERMISSIONS] ?: emptySet()) + permissionName
        }
    }
}
