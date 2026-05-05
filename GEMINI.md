# Standalone Jetpack Camera App Rules

This file documents conventions and rules for the standalone Jetpack Camera App (JCA) workspace.

## JDK Version

*   **Rule**: Whenever you execute Gradle commands (`./gradlew`) for the project `~/jca/jetpack-camera-app`, you must use JDK 21 by setting the `JAVA_HOME` environment variable to `/usr/local/google/home/davidjia/.jdks/ms-21.0.10`.

## Spotless Command Interpretation

*   **Command: "run spotless"**
    *   **Rule**: Whenever the user says "run spotless", you must interpret it as a request to run the following commands:
        1. `./gradlew spotlessCheck --init-script gradle/init.gradle.kts --parallel --build-cache`
        2. If the check fails, run `./gradlew spotlessApply --init-script gradle/init.gradle.kts --parallel --build-cache` to apply formatting fixes.
