#!/bin/bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "Running spotlessCheck..."
./gradlew spotlessCheck --init-script gradle/init.gradle.kts --parallel --build-cache
CHECK_STATUS=$?

echo "Running spotlessApply..."
./gradlew spotlessApply --init-script gradle/init.gradle.kts --parallel --build-cache
APPLY_STATUS=$?

if [ $APPLY_STATUS -eq 0 ]; then
    echo "Spotless completed successfully."
    exit 0
else
    echo "Spotless failed."
    exit 1
fi
