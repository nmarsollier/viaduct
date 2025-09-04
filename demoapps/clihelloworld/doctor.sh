#!/usr/bin/env bash

set -e

echo "Checking prerequisites..."

# Check if gradle is installed
if ! command -v gradle &> /dev/null; then
  echo "❌ Gradle is not installed or not in PATH. Please install Gradle first."  >&2
  exit 1
fi

# Check Gradle version (requires 8.3 or higher)
GRADLE_VERSION=$(gradle --version | grep "Gradle" | awk '{print $2}')
REQUIRED_VERSION="8.3"

# Function to compare version numbers
version_compare() {
    local version1=$1
    local version2=$2

    # Convert versions to comparable format (e.g., 8.3 -> 8003000, 8.10 -> 8010000)
    local v1=$(echo "$version1" | awk -F. '{printf "%d%03d%03d", $1, $2, $3}')
    local v2=$(echo "$version2" | awk -F. '{printf "%d%03d%03d", $1, $2, $3}')

    if [ "$v1" -lt "$v2" ]; then
        return 1  # version1 < version2
    else
        return 0  # version1 >= version2
    fi
}

if ! version_compare "$GRADLE_VERSION" "$REQUIRED_VERSION"; then
    echo "❌ Gradle $REQUIRED_VERSION or higher is required. Found: $GRADLE_VERSION" >&2
    exit 1
fi
echo "✅ Gradle has correct version."

# Determine which java to use
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
elif command -v java &> /dev/null; then
  JAVA_CMD="java"
else
  echo "❌ Java is not installed or not in PATH. Please install Java first or set JAVA_HOME."  >&2
  exit 1
fi

# If version starts with '1', omits 1; otherwise captures major version.
# 1.8.0_452 -> 8
# 11.0.26 -> 11
JAVA_VERSION=$($JAVA_CMD -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)

# Ensure JAVA_VERSION is a valid number
if ! printf '%s\n' "$JAVA_VERSION" | grep -Eq '^[0-9]+$'; then
  echo "❌ Could not parse Java version correctly. Got: $JAVA_VERSION" >&2
  exit 1
fi

# Compare numerically
if [ "$JAVA_VERSION" -lt 21 ]; then
  echo "❌ Java 21 or higher is required. Found: $JAVA_VERSION" >&2
  exit 1
fi
echo "✅ Java has correct version."

echo "✅ Viaduct ready: type ./gradlew -q run in viaduct working directory."
echo "or try ./gradlew -q run --args="'{ author }'""
