#!/usr/bin/env bash

set -e

echo "Checking prerequisites..."

# Ensure we're being run in an empty directory
if [ "$(ls -A .)" ]; then
  echo "Viaduct bootstrapper must be run in an empty directory" >&2
  exit 1
fi

# Check if gradle is installed
if ! command -v gradle &> /dev/null; then
  echo "Gradle is not installed or not in PATH. Please install Gradle first."  >&2
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
    echo "Gradle $REQUIRED_VERSION or higher is required. Found: $GRADLE_VERSION" >&2
    exit 1
fi

# Determine which java to use
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
elif command -v java &> /dev/null; then
  JAVA_CMD="java"
else
  echo "Java is not installed or not in PATH. Please install Java first or set JAVA_HOME."  >&2
  exit 1
fi

# If version starts with '1', omits 1; otherwise captures major version.
# 1.8.0_452 -> 8
# 11.0.26 -> 11
JAVA_VERSION=$($JAVA_CMD -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)

# Ensure JAVA_VERSION is a valid number
if ! printf '%s\n' "$JAVA_VERSION" | grep -Eq '^[0-9]+$'; then
  echo "Could not parse Java version correctly. Got: $JAVA_VERSION" >&2
  exit 1
fi

# Compare numerically
if [ "$JAVA_VERSION" -lt 21 ]; then
  echo "Java 21 or higher is required. Found: $JAVA_VERSION" >&2
  exit 1
fi

echo "Bootstrapping Viaduct project..."

touch build.gradle.kts
gradle wrapper

# Directories
SCHEMA_DIR="schema"
TENANTS_DIR="tenants"
HELLO_DIR="$TENANTS_DIR/helloworld"

#  Create build.gradle.kts
cat > build.gradle.kts <<'EOF'
plugins {
    kotlin("jvm") version "1.9.24"
    id("viaduct-app") version "0.1.0-SNAPSHOT"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    runtimeOnly(project(":tenants:helloworld"))
    implementation(project(":schema"))
    implementation("com.airbnb.viaduct:runtime:0.1.0-SNAPSHOT")
    implementation("ch.qos.logback:logback-classic:1.3.7")
}

application {
    mainClass.set("com.example.viadapp.ViaductApplicationKt")
}
EOF

mkdir -p "$SCHEMA_DIR"
mkdir -p "$HELLO_DIR/src/main/kotlin/com/example/viadapp/helloworld"
mkdir -p "$HELLO_DIR/src/main/resources/schema"
mkdir -p "src/main/kotlin/com/example/viadapp"

# Create root application file
cat > "src/main/kotlin/com/example/viadapp/ViaductApplication.kt" <<'EOF'
package com.example.viadapp

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import viaduct.engine.runtime.execution.withThreadLocalCoroutineContext
import viaduct.service.api.ExecutionInput
import viaduct.service.runtime.StandardViaduct
import viaduct.service.runtime.ViaductSchemaRegistryBuilder
import viaduct.tenant.runtime.bootstrap.TenantPackageFinder
import viaduct.tenant.runtime.bootstrap.ViaductTenantAPIBootstrapper

const val SCHEMA_ID = "publicSchema"

fun main(argv: Array<String>) {
    val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
    rootLogger.level = Level.WARN

    // Create a Viaduct engine
    // Note to reviewers: this is the long-form of building an engine.  We plan on
    // having a shorter form with defaults.
    val viaduct = StandardViaduct.Builder()
        .withTenantAPIBootstrapperBuilder(
            ViaductTenantAPIBootstrapper.Builder()
                .tenantPackagePrefix("com.example.viadapp")
        )
        .withSchemaRegistryBuilder(
            ViaductSchemaRegistryBuilder()
                .withFullSchemaFromResources("", ".*graphqls")
                .registerFullSchema(SCHEMA_ID)
        ).build()

    // Create an execution input
    val executionInput = ExecutionInput(
        query = (argv.getOrNull(0) ?:
                     """
                     query {
                         greeting
                     }
                     """.trimIndent()),
        variables = emptyMap(),
        requestContext = object {},
        schemaId = SCHEMA_ID
    )

    // Run the query
    val result = runBlocking {
        // Note to reviewers: in the future the next two scope functions
        // will go away
        coroutineScope {
            withThreadLocalCoroutineContext {
                viaduct.execute(executionInput)
            }
        }
    }

    // [toSpecification] converts to JSON as described in the GraphQL
    // specification.
    val mapper = ObjectMapper().writerWithDefaultPrettyPrinter()
    println(
        mapper.writeValueAsString(result.toSpecification())
    )
}
EOF

# Create resolvers
cat > "$HELLO_DIR/src/main/kotlin/com/example/viadapp/helloworld/HelloWorldResolvers.kt" <<'EOF'
package com.example.viadapp.helloworld

import viaduct.api.Resolver
import com.example.viadapp.helloworld.resolverbases.QueryResolvers

@Resolver
class GreetingResolver : QueryResolvers.Greeting() {
    override suspend fun resolve(ctx: Context): String {
        return "Hello, World!"
    }
}

@Resolver
class AuthorResolver : QueryResolvers.Author() {
    override suspend fun resolve(ctx: Context): String {
        return "Brian Kernighan"
    }
}
EOF

# Create default schema file
cat > "$HELLO_DIR/src/main/resources/schema/builtin_schema.graphqls" <<'EOF'
directive @resolver on FIELD_DEFINITION | OBJECT
directive @backingData(class: String!) on FIELD_DEFINITION
directive @scope(to: [String!]!) repeatable on OBJECT | INPUT_OBJECT | ENUM | INTERFACE | UNION

type Query @scope(to: ["*"]) {
  _: String @deprecated
}
type Mutation @scope(to: ["*"]) {
  _: String @deprecated
}
type Subscription @scope(to: ["*"]) {

  _: String @deprecated
}
EOF

# Create application schema file
cat > "$HELLO_DIR/src/main/resources/schema/schema.graphqls" <<'EOF'
extend type Query {
  greeting: String @resolver
  author: String @resolver
}
EOF

# Create build.gradle.kts for helloworld tenant
cat > "$HELLO_DIR/build.gradle.kts" <<'EOF'
plugins {
    `java-library`
    id("viaduct-tenant")
    kotlin("jvm")
}

viaductTenant {
    create("helloworld") {
        packageName.set("com.example.viadapp")
        schemaDirectory("${project.rootDir}/tenants/helloworld/src/main/resources/schema")
        schemaProjectPath.set(":schema")
        schemaName.set("schema")
    }
}

dependencies{
    implementation("com.airbnb.viaduct:runtime:0.1.0-SNAPSHOT")
    implementation("ch.qos.logback:logback-classic:1.3.7")
}
EOF

# Create settings.gradle.kts
cat > settings.gradle.kts <<'EOF'
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

include(":schema")
include(":tenants:helloworld")
EOF

echo "✅ Viaduct bootstrap complete, running it now..."
./gradlew run
