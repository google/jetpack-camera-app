/*
 * Copyright 2024 The Android Open Source Project
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

#include <android/log.h>

#define GL_GLEXT_PROTOTYPES
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <jni.h>

namespace {
    auto constexpr LOG_TAG = "OpenGLDebugLib";

    void gl_debug_cb(GLenum source, GLenum type, GLuint id, GLenum severity, GLsizei length,
                     const GLchar* message, const void* userParam) {
        if (type == GL_DEBUG_TYPE_ERROR_KHR) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                "GL ERROR:\n %s.",
                                message);
        }
    }
} // namespace

extern "C" {
JNIEXPORT void JNICALL
Java_com_google_jetpackcamera_core_camera_effects_GLDebug_enableES3DebugErrorLogging(
        JNIEnv *env, jobject clazz) {
    glDebugMessageCallbackKHR(gl_debug_cb, nullptr);
    glEnable(GL_DEBUG_OUTPUT_KHR);
}
}
