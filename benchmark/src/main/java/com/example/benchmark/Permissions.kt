/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.example.benchmark

import android.Manifest.permission
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import org.junit.Assert

fun MacrobenchmarkScope.allowCamera() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val command = "pm grant $packageName ${permission.CAMERA}"
        val output = device.executeShellCommand(command)
        Assert.assertEquals("", output)
    }
}
