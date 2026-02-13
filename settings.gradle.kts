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

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            setUrl("https://androidx.dev/snapshots/builds/13672667/artifacts/repository")
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "Jetpack Camera"
include(":app")
include(":feature:preview")
include(":core:camera")
include(":core:camera:low-light")
include(":core:camera:low-light-playservices")
include(":feature:settings")
include(":data:settings")
include(":data:media")
include(":core:common")
include(":benchmark")
include(":feature:permissions")
include(":feature:postcapture")
include(":ui:uistate")
include(":ui:uistateadapter")
include(":ui:uistate:capture")
include(":ui:uistateadapter:capture")
include(":ui:components")
include(":ui:components:capture")
include(":data:model")
include(":core:model")
include(":ui:uistate:postcapture")
include(":ui:uistateadapter:postcapture")
include(":core:camera:postprocess")
include(":data:settings-datastore")
