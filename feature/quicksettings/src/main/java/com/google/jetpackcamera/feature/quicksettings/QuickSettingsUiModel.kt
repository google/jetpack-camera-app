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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * An instance of this class represents the UI of a quick setting feature. A list of instances of
 * this class should be supplied to [QuickSettingsUi] to reflect the UI in the quick settings.
 *
 * @param drawableResIds    resource ids for the icons corresponding to the options of this feature
 * @param textResIds        resource ids for the text corresponding to the options of this feature
 * @param descriptionResIds resource ids for the description corresponding to the options of this
 *                          feature
 * @param highlightedIndex  the index of the highlighted option of this feature
 * @param onClicks          the onClick callbacks for the options of this feature
 */
data class QuickSettingsUiModel(
    @DrawableRes val drawableResIds: List<Int>,
    @StringRes val textResIds: List<Int>,
    @StringRes val descriptionResIds: List<Int>,
    val highlightedIndex: Int,
    val onClicks: List<() -> Unit>)
