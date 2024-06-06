#
# Copyright (C) 2024 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
cmake_minimum_required(VERSION 3.22.1)

project(domain_camera_jni)

add_library(
        opengl_debug_lib
        SHARED
        opengl_debug_jni.cpp
        jni_hooks.cpp
)

find_library(log-lib log)
find_library(opengles3-lib GLESv3)
target_link_libraries(opengl_debug_lib PRIVATE ${log-lib} ${opengles3-lib})
target_link_options(
        opengl_debug_lib
        PRIVATE
        "-Wl,-z,max-page-size=16384"
)
