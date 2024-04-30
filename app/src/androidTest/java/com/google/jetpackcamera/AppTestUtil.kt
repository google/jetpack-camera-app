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
package com.google.jetpackcamera

import android.os.Build

val APP_REQUIRED_PERMISSIONS: List<String> = buildList {
    add(android.Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT <= 28) {
        add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    if(Build.VERSION.SDK_INT < 32){
        add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    if (Build.VERSION.SDK_INT >= 33) {
        add (android.Manifest.permission.READ_MEDIA_IMAGES)
        add (android.Manifest.permission.READ_MEDIA_VIDEO)
    }
    if (Build.VERSION.SDK_INT >= 34) {
        add(android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    }
}
