import com.github.jk1.license.LicenseReportExtension.ALL
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.ExcludeTransitiveDependenciesFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.filter.ReduceDuplicateLicensesFilter
import com.github.jk1.license.importer.XmlReportImporter
import com.github.jk1.license.render.TextReportRenderer
import com.github.jk1.license.render.XmlReportRenderer

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
    id("com.github.jk1.dependency-license-report") version "2.5"
}

licenseReport {
    // By default this plugin will collect the union of all licenses from
    // the immediate pom and the parent poms. If your legal team thinks this
    // is too liberal, you can restrict collected licenses to only include the
    // those found in the immediate pom file
    // Defaults to: true
    unionParentPomLicenses = false
    // Select projects to examine for dependencies.
    // Defaults to current project and all its subprojects
    projects = arrayOf(project) + project.subprojects
    // Adjust the configurations to fetch dependencies. Default is 'runtimeClasspath'
    // For Android projects use 'releaseRuntimeClasspath' or 'yourFlavorNameReleaseRuntimeClasspath'
    // Use 'ALL' to dynamically resolve all configurations:
    // configurations = ALL
    configurations = ALL

    // Don't include artifacts of project's own group into the report
    excludeOwnGroup = true

    // Set custom report renderer, implementing ReportRenderer.
    // Yes, you can write your own to support any format necessary.
    renderers = arrayOf(TextReportRenderer())

    filters = arrayOf<DependencyFilter>(
        LicenseBundleNormalizer(),
        ReduceDuplicateLicensesFilter(),
//        ExcludeTransitiveDependenciesFilter()
    )
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