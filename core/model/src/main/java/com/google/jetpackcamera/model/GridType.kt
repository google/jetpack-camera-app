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
package com.google.jetpackcamera.model

import com.google.jetpackcamera.model.proto.GridTypeProto

enum class GridType {
    NONE,
    RULE_OF_THIRDS;

    companion object {
        fun GridType.toProto(): GridTypeProto {
            return when (this) {
                NONE -> GridTypeProto.GRID_TYPE_PROTO_NONE
                RULE_OF_THIRDS -> GridTypeProto.GRID_TYPE_PROTO_RULE_OF_THIRDS
            }
        }

        fun fromProto(proto: GridTypeProto): GridType {
            return when (proto) {
                GridTypeProto.GRID_TYPE_PROTO_NONE -> NONE
                GridTypeProto.GRID_TYPE_PROTO_RULE_OF_THIRDS -> RULE_OF_THIRDS
                else -> NONE
            }
        }
    }
}
