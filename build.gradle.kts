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

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.1" apply false
    id("com.android.library") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.0" apply false
    id("com.google.dagger.hilt.android") version "2.44" apply false
    id("com.android.test") version "8.1.1" apply false
}

tasks.register<Copy>("installGitHooks") {
    println("Installing git hooks")
    from(rootProject.rootDir.resolve("hooks/pre-commit"))
    into(rootProject.rootDir.resolve(".git/hooks"))
    fileMode = 7 * 64 + 7 * 8 + 5 // 0775 in decimal
}

gradle.taskGraph.whenReady {
    allTasks.forEach { task ->
        if (task != tasks["installGitHooks"]) {
            task.dependsOn(tasks["installGitHooks"])
        }
    }
}