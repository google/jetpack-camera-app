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

package com.google.jetpackcamera.feature.quicksettings

interface QuickSettingsEnum {
    fun getOrdinal() : Int
}
enum class CameraLensFace : QuickSettingsEnum {
    FRONT {
        override fun getOrdinal(): Int {
            return FRONT.ordinal
        }
    },
    BACK {
        override fun getOrdinal(): Int {
            return BACK.ordinal
        }
    }
}

enum class CameraFlashMode : QuickSettingsEnum {
    OFF {
        override fun getOrdinal(): Int {
            return OFF.ordinal
        }
    },
    AUTO {
        override fun getOrdinal(): Int {
            return AUTO.ordinal
        }
    },
    ON {
        override fun getOrdinal(): Int {
            return ON.ordinal
        }
    }
}

enum class CameraAspectRatio : QuickSettingsEnum {
    THREE_FOUR {
        override fun getOrdinal(): Int {
            return THREE_FOUR.ordinal
        }
    },
    NINE_SIXTEEN {
        override fun getOrdinal(): Int {
            return NINE_SIXTEEN.ordinal
        }
    },
    ONE_ONE {
        override fun getOrdinal(): Int {
            return ONE_ONE.ordinal
        }
    },
}

enum class CameraTimer : QuickSettingsEnum {
    OFF {
        override fun getOrdinal(): Int {
            return OFF.ordinal
        }
    },
    THREE_SEC {
        override fun getOrdinal(): Int {
            return THREE_SEC.ordinal
        }
    },
    TEN_SEC {
        override fun getOrdinal(): Int {
            return TEN_SEC.ordinal
        }
    }
}