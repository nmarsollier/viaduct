# GraphQL Schema Validator Tool

Command line utility (Kotlin + `graphql-java`) to statically validate GraphQL schema definitions (\*.graphqls) in a
single file or an entire directory (merged).

## Features

- Validates SDL syntax and type wiring constructability
- Accepts file or directory (`--schema=...`)
- Aggregates all `.graphqls` files in a directory (recursive)
- Provides a Gradle task for convenience

## Build

```bash
./gradlew :tools:build
```

Artifacts:

- Compiled jar: `tools/build/libs/`
- Sources jar: `tools/build/libs/*-sources.jar`

## Run (Gradle Task)

```bash
./gradlew :tools:validateSchema --args="--schema=path/to/schema.graphqls"
./gradlew :tools:validateSchema --args="--schema=path/to/schema/dir"
```

## Run (Direct JVM)

```bash
java -cp tools/build/libs/tools-<version>.jar viaduct.cli.validation.schema.ViaductSchemaValidatorCLIKt --schema=schemas/schema.graphqls
```

## Arguments

| Argument          | Required | Description                                                                            |
|-------------------|----------|----------------------------------------------------------------------------------------|
| `--schema=<path>` | Yes      | Path to a single `.graphqls` file or a directory containing multiple `.graphqls` files |

## Exit Codes

| Code | Meaning                                                |
|------|--------------------------------------------------------|
| 0    | Schema valid                                           |
| 1    | Missing argument, file not found, or validation errors |

## Example Output (Success)

```
Resolving from absolute path: /abs/path/schema.graphqls
Attempting to validate schema.graphqls...
Schema validation successful!
Found 12 types defined.
Schema is valid
```

## Example Output (Failure)

```
Attempting to validate directory /abs/path/schemas
Reading file /abs/path/schemas/User.graphqls
Reading file /abs/path/schemas/Query.graphqls
Invalid type 'UserX' referenced in field 'users'
```

## Use in CI

```bash
./gradlew :tools:validateSchema --args="--schema=schema/"
```

Fail the pipeline if exit code != 0.

## Implementation Notes

- Uses `SchemaParser` + `SchemaGenerator` from `graphql-java`
- Builds an executable schema with empty runtime wiring (structural validation only)
- Recursively concatenates all `.graphqls` files when directory input is used

## Limitations

- No resolver wiring validation (runtime behavior not checked)
- Does not validate schema against operations/documents
- No SDL linting rules beyond structural validity

