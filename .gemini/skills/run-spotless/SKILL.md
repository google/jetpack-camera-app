---
name: run-spotless
description: Runs the project's spotless workflow (check and apply) using the correct Java 17 environment. Trigger when the user says "run spotless".
---

# run-spotless

This skill automates the process of checking and applying code formatting using Spotless in the Jetpack Camera App project.

## Workflow

When triggered by "run spotless", execute the following script from the project root:

```bash
./.gemini/skills/run-spotless/scripts/run_spotless.sh
```

Alternatively, you can manually run the following commands with `JAVA_HOME` set to `/usr/lib/jvm/java-17-openjdk-amd64`:

1. `./gradlew spotlessCheck --init-script gradle/init.gradle.kts --parallel --build-cache`
2. `./gradlew spotlessApply --init-script gradle/init.gradle.kts --parallel --build-cache`

## Why use this skill?

- **Environment Consistency**: Ensures `JAVA_HOME` is set to Java 17, which is required as Java 25 currently causes build failures with this Gradle version.
- **Automation**: Combines check and apply steps into a single action.
- **Project Specific**: Uses the correct flags (`--init-script`, `--parallel`, `--build-cache`) required for this project.
