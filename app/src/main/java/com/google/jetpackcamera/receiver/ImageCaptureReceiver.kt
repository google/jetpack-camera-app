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
package com.google.jetpackcamera.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import com.google.jetpackcamera.MainActivity

class ImageCaptureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val activityIntent = Intent(context, MainActivity::class.java)
        val externalContentValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, ContentValues::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        }
        if (externalContentValues != null) {
            activityIntent.putExtra(MediaStore.EXTRA_OUTPUT, externalContentValues)
        }
        activityIntent.putExtra(EXTRA_SHOULD_FINISH_AFTER_CAPTURE, true)
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(activityIntent)
    }

    companion object {
        const val EXTRA_SHOULD_FINISH_AFTER_CAPTURE = "EXTRA_SHOULD_FINISH_AFTER_CAPTURE"
    }
}
