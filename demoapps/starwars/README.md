# Star Wars GraphQL Demo

This demo application showcases a comprehensive GraphQL API implementation using Viaduct's custom directives and batch resolvers. The demo features Star Wars characters, films, planets, and other entities with full relationship mapping and optimized N+1 query prevention.

## Custom Directives Demonstrated

### @resolver
**Purpose**: Indicates that a field or object requires custom resolution logic rather than simple property access.

**Usage**: Applied to fields that need runtime computation, database lookups, or complex business logic.

**Fragment Syntax**: The @resolver directive supports two types of fragment syntax for efficient field resolution:

#### 1. Shorthand Fragment Syntax
```kotlin
@Resolver("fieldName")
class MyFieldResolver {
  override suspend fun resolve(ctx: Context): String {
    // Automatically delegates to the specified field
    return ctx.objectValue.getFieldName()
  }
}
```

**Use cases**:
- Creating field aliases
- Simple field transformations
- Computed fields based on single existing fields

#### 2. Full Fragment Syntax
```kotlin
@Resolver(
  """
    fragment _ on MyType {
        field1
        field2
        field3
    }
    """
)
class MyComputedFieldResolver {
  override suspend fun resolve(ctx: Context): String {
    val obj = ctx.objectValue
    // Can access all specified fields
    return "${obj.getField1()} - ${obj.getField2()} (${obj.getField3()})"
  }
}
```

**Use cases**:
- Computed fields requiring multiple source fields
- Performance optimization by specifying exact field requirements
- Complex business logic combining multiple attributes

#### 3. Batch Resolver Fragment Syntax
```kotlin
@Resolver(objectValueFragment = "fragment _ on Character { id name }")
class CharacterBatchResolver : CharacterResolvers.SomeField() {
  override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<String>> {
    // Process multiple contexts efficiently in one batch
    return contexts.map { ctx ->
      val character = ctx.objectValue
      FieldValue.ofValue("${character.getName()} processed in batch")
    }
  }
}
```

**Use cases**:
- Preventing N+1 query problems
- Efficient batch processing of multiple field requests
- Performance optimization for list queries

**Example**:
```graphql
type Character {
  name: String @resolver
  homeworld: Planet @resolver      # Standard resolver (or batch resolver)
  displayName: String @resolver    # Shorthand fragment: @Resolver("name")
  summary: String @resolver        # Full fragment: @Resolver("fragment _ on Character { name birthYear }")
  filmCount: Int @resolver         # Batch resolver for efficient counting
  richSummary: String @resolver    # Batch resolver with complex logic
}
```

### @backingData
**Purpose**: Specifies a backing data class for complex field resolution, particularly useful for connection fields requiring pagination.

**Usage**: Applied to connection fields to specify the implementation class for pagination and filtering.

**Example**:
```graphql
type Character {
  filmConnection(first: Int, after: String): CharacterFilmsConnection
  @resolver
  @backingData(class: "starwars.character.FilmConnection")
}
```

**Implementation**: The specified class provides pagination logic and data transformation for GraphQL connections.

### @scope
**Purpose**: Restricts schema availability to specific tenants or contexts, enabling multi-tenant GraphQL schemas.

**Usage**: Applied to types, interfaces, and query fields to limit access to specific scopes.

**Example**:
```graphql
type Query @scope(to: ["default"]) {
  allCharacters: [Character]
}

type Species @scope(to: ["default", "extras"]) {
  name: String
  culturalNotes: String @scope(to: ["extras"])  # Only available with "extras" scope
}
```

**Implementation**: The demo demonstrates both default scope access and restricted "extras" scope fields.

### @idOf
**Purpose**: Specifies global ID type association and validation for proper type checking.

**Usage**: Applied to ID fields and arguments to associate them with specific GraphQL types.

**Example**:
```graphql
type Query {
  character(id: ID! @idOf(type: "Character")): Character
}

type Character {
  id: ID! @idOf(type: "Character")
}
```

**Implementation**: Enables type-safe GlobalID handling and validation across the GraphQL schema.

### @oneOf
**Purpose**: Ensures input objects have exactly one non-null field, useful for union-like input types.

**Usage**: Applied to input types where only one option should be specified.

**Example**:
```graphql
input CharacterSearchInput @oneOf {
  byName: String
  byId: ID
  byBirthYear: String
}

type Query {
  searchCharacter(search: CharacterSearchInput!): Character
}
```

**Implementation**: Validates that exactly one search criterion is provided, preventing ambiguous queries.

## Batch Resolvers: Performance Optimization

### The N+1 Problem
Without batch resolvers, querying multiple characters with homeworlds results in N+1 database calls:
```
1 query for characters + N queries for homeworlds = inefficient
```

### Batch Resolver Solution
With batch resolvers, the same query becomes highly efficient:
```
1 query for characters + 1 batch query for all homeworlds = optimal
```

### Example Implementation
```kotlin
@Resolver(objectValueFragment = "fragment _ on Character { id }")
class CharacterHomeworldBatchResolver : CharacterResolvers.Homeworld() {
  override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Planet?>> {
    // Extract all character IDs
    val characterIds = contexts.map { it.objectValue.getId().internalID }

    // Single batch lookup for all homeworlds
    val homeworlds = lookupHomeworlds(characterIds)

    // Return results in same order as contexts
    return contexts.map { ctx ->
      val homeworld = homeworlds[ctx.objectValue.getId().internalID]
      FieldValue.ofValue(homeworld)
    }
  }
}
```

## Data Model

The demo includes comprehensive Star Wars data:

### Characters
- Luke Skywalker (Tatooine, Human, pilots X-wing)
- Princess Leia (Alderaan, Human)
- Han Solo (Corellia, Human, pilots Millennium Falcon)
- Darth Vader (Tatooine, Human)
- Obi-Wan Kenobi (Stewjon, Human)

### Films
- A New Hope (Episode IV)
- The Empire Strikes Back (Episode V)
- Return of the Jedi (Episode VI)

### Planets
- Tatooine (desert world)
- Alderaan (destroyed planet)
- Corellia (industrial world)
- Stewjon (Obi-Wan's homeworld)

### Species
- Human (with extras scope: cultural notes, rarity level, special abilities)

### Starships & Vehicles
- Millennium Falcon (Han's ship)
- X-wing (Luke's fighter)
- Speeder bikes

## Architecture

### In-Memory Storage
The demo uses `StarWarsData` object for in-memory data storage with relationship mappings:
```kotlin
val characterFilmRelations = mapOf(
  "1" to listOf("1", "2", "3"), // Luke in all three films
  // ...
)
```

### Single vs Batch Resolvers
The demo includes both patterns:

**Single Resolvers** (traditional approach):
```kotlin
@Resolver("id")
class CharacterHomeworldResolver : CharacterResolvers.Homeworld() {
  override suspend fun resolve(ctx: Context): Planet? {
    // Resolves one character's homeworld
  }
}
```

**Batch Resolvers** (optimized approach):
```kotlin
@Resolver(objectValueFragment = "fragment _ on Character { id }")
class CharacterHomeworldBatchResolver : CharacterResolvers.Homeworld() {
  override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Planet?>> {
    // Resolves multiple characters' homeworlds efficiently
  }
}
```

### Connection Implementation
GraphQL connections are implemented using backing data classes that handle pagination:
- `CharactersConnection` for paginated character queries
- `FilmsConnection` for paginated film queries
- Relationship connections for character-film, character-starship mappings

## Key Features

1. **Batch Resolver Optimization**: Prevents N+1 queries with automatic batching
2. **Multi-tenant Schema**: Demonstrates `@scope` directive for tenant isolation
3. **Type-safe GlobalIDs**: Uses `@idOf` with encoded GlobalID system
4. **Complex Relationships**: Shows related entities with efficient resolution
5. **Pagination Support**: Implements GraphQL connection pattern
6. **Input Validation**: Demonstrates `@oneOf` for exactly-one-field semantics
7. **Fragment Optimization**: Specifies exact field requirements for performance

## Batch Resolver Examples

### Simple Batch Counting
```kotlin
// Efficiently count films for multiple characters
@Resolver(objectValueFragment = "fragment _ on Character { id }")
class CharacterFilmCountResolver : CharacterResolvers.FilmCount() {
  override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Int>> {
    val counts = batchCountFilms(contexts.map { it.objectValue.getId().internalID })
    return contexts.map { FieldValue.ofValue(counts[it.objectValue.getId().internalID] ?: 0) }
  }
}
```

### Complex Multi-Source Batching
```kotlin
// Combine data from multiple sources efficiently
@Resolver(objectValueFragment = "fragment _ on Character { id name birthYear }")
class CharacterRichSummaryResolver : CharacterResolvers.RichSummary() {
  override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<String>> {
    // Batch lookup homeworlds, species, film counts
    val summaries = createRichSummaries(contexts)
    return contexts.map { FieldValue.ofValue(summaries[it]) }
  }
}
```

## Example Queries

### Basic Character Query (with batch optimization)
```graphql
query {
  allCharacters(first: 5) {
    characters {
      name
      homeworld { name }  # Batch resolved efficiently
      filmCount          # Batch calculated
    }
  }
}
```

### Complex Batch Query
```graphql
query {
  allCharacters(first: 10) {
    characters {
      name
      richSummary      # Combines multiple data sources
      homeworld { name }
      species { name }
      filmCount
    }
  }
}
```

### Film with Batch Character Resolution
```graphql
query {
  allFilms {
    films {
      title
      mainCharacters {  # Batch resolved
        name
        homeworld { name }
      }
    }
  }
}
```

### Search with @oneOf
```graphql
query {
  searchCharacter(search: { byName: "Luke" }) {
    name
    birthYear
    homeworld { name }
  }
}
```

### Scoped Species Query
```graphql
# Basic query (default scope)
query {
  species(id: "1") {
    name
    classification
  }
}

# Extended query (requires "extras" scope header)
query {
  species(id: "1") {
    name
    culturalNotes      # Only available with extras scope
    specialAbilities
  }
}
```

## Performance Benefits

### Without Batch Resolvers
```
Query: 100 characters with homeworlds
- 1 query for characters
- 100 individual homeworld queries
Total: 101 database calls 😰
```

### With Batch Resolvers
```
Query: 100 characters with homeworlds
- 1 query for characters
- 1 batch query for homeworlds
Total: 2 database calls 🚀
```

**Result**: 50x performance improvement!

## Building and Running

This demo is part of the Viaduct framework:

```bash
# Build the demo
bazel build //projects/viaduct/oss/demoapps/starwars:starwars_demo

# Run tests
bazel test //projects/viaduct/oss/demoapps/starwars/...

# Run batch resolver tests
bazel test //projects/viaduct/oss/demoapps/starwars:BatchResolverDemoTest
```

## Testing

The demo includes comprehensive tests:
- **Integration tests** for all resolvers
- **Batch resolver performance tests**
- **Scope isolation tests**
- **GlobalID encoding/decoding tests**
- **@oneOf input validation tests**

Key test files:
- `ResolverIntegrationTest.kt` - Tests all standard resolvers
- `BatchResolverDemoTest.kt` - Tests batch resolver efficiency
- `ExtrasScopeTest.kt` - Tests multi-tenant scoping
- `GlobalIDDemoTest.kt` - Tests GlobalID functionality

The implementation demonstrates how Viaduct's custom directives and batch resolvers enable powerful GraphQL schema capabilities while maintaining optimal performance and clean, understandable code organization.
