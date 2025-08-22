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
**Purpose**: Specifies a backing data class for complex field resolution.

**Usage**: Applied to fields to specify the implementation class for data transformation.

**Example**:
```graphql
type Character {
  films(limit: Int): [Film]
  @resolver
  @backingData(class: "starwars.character.FilmConnection")
}
```

**Implementation**: The specified class provides data transformation logic for GraphQL fields.

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

### Variables and Variable Providers
**Purpose**: Enable dynamic field selection and conditional GraphQL queries through runtime variable computation.

**Usage**: Variables can be bound to resolver arguments or computed dynamically using VariableProvider classes to control which fields are selected at the GraphQL execution level.

Viaduct supports three approaches for dynamic field resolution:

#### 1. Variables with @Variable and fromArgument
Variables can be bound directly to resolver arguments to control GraphQL directive evaluation:

```kotlin
@Resolver(
    """
    fragment _ on Character {
        name
        birthYear @include(if: $includeDetails)
        height @include(if: $includeDetails)
        mass @include(if: $includeDetails)
    }
    """,
    variables = [Variable("includeDetails", fromArgument = "includeDetails")]
)
class CharacterProfileResolver {
    // When includeDetails=true, all fields are available
    // When includeDetails=false, only name is selected
}
```

**Benefits**: GraphQL-level optimization, declarative field selection, efficient data fetching.

#### 2. Argument-Based Statistics Logic
For practical demo purposes, the character stats use argument-based conditional logic:

```kotlin
@Resolver(
    """
    fragment _ on Character {
        name
        birthYear
        height
        species {
            name
        }
    }
    """
)
class CharacterStatsResolver {
    override suspend fun resolve(ctx: Context): String? {
        val args = ctx.arguments
        return when {
            isValidAgeRange(args.minAge, args.maxAge) -> buildDetailedStats(ctx.objectValue)
            else -> buildBasicStats(ctx.objectValue)
        }
    }
}
```

**Benefits**: Simple implementation, full access to all fields, easy to debug and maintain.

*Note: The full VariableProvider API with dynamic computation is available in the complete Viaduct runtime but simplified here for demo clarity.*

#### 3. Argument-Based Conditional Logic
For simpler cases, traditional argument processing within resolvers:

```kotlin
@Resolver(
    """
    fragment _ on Character {
        name
        birthYear
        eyeColor
        hairColor
    }
    """
)
class CharacterFormattedDescriptionResolver {
    override suspend fun resolve(ctx: Context): String? {
        return when (ctx.arguments.format) {
            "detailed" -> buildDetailedDescription(ctx.objectValue)
            "year-only" -> buildYearOnlyDescription(ctx.objectValue)  
            else -> ctx.objectValue.getName()
        }
    }
}
```

**Benefits**: Simplicity, full Kotlin language features, easy debugging.

**Example Schema**:
```graphql
type Character {
  # Variables with fromArgument - demonstrates GraphQL-level field selection
  characterProfile(includeDetails: Boolean = false): String @resolver
  
  # Argument-based statistics - practical implementation for demos  
  characterStats(minAge: Int, maxAge: Int): String @resolver
  
  # Argument-based conditional logic - flexible formatting
  formattedDescription(format: String = "default"): String @resolver
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

### List Implementation
GraphQL lists are implemented using backing data classes:
- Simple list queries for characters, films, planets, species, vehicles
- Relationship lists for character-film, character-starship mappings
- Optional limit parameter to control the number of returned items

## Key Features

1. **Batch Resolver Optimization**: Prevents N+1 queries with automatic batching
2. **Multi-tenant Schema**: Demonstrates `@scope` directive for tenant isolation
3. **Type-safe GlobalIDs**: Uses `@idOf` with encoded GlobalID system
4. **Complex Relationships**: Shows related entities with efficient resolution
5. **List Support**: Implements simple GraphQL lists with optional limit parameter
6. **Input Validation**: Demonstrates `@oneOf` for exactly-one-field semantics
7. **Variables and Variable Providers**: Dynamic field selection with three different approaches
8. **Fragment Optimization**: Specifies exact field requirements for performance
9. **Comprehensive Documentation**: All directives and features are thoroughly documented

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
  allCharacters(limit: 5) {
    name
    homeworld { name }  # Batch resolved efficiently
    filmCount          # Batch calculated
  }
}
```

### Complex Batch Query
```graphql
query {
  allCharacters(limit: 10) {
    name
    richSummary      # Combines multiple data sources
    homeworld { name }
    species { name }
    filmCount
  }
}
```

### Film with Batch Character Resolution
```graphql
query {
  allFilms {
    title
    mainCharacters {  # Batch resolved
      name
      homeworld { name }
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

### Variables and Variable Providers Examples
```graphql
# Variables with @Variable fromArgument
query BasicProfile {
  person(id: "cGVvcGxlOjE=") {  # Luke Skywalker
    name
    characterProfile(includeDetails: false)
    # Result: "Character Profile: Luke Skywalker (basic info only)"
  }
}

query DetailedProfile {
  person(id: "cGVvcGxlOjE=") {
    name  
    characterProfile(includeDetails: true)
    # Result: "Character Profile: Luke Skywalker, Born: 19BBY, Height: 172cm, Mass: 77.0kg"
  }
}

# VariableProvider with dynamic computation
query CharacterStats {
  person(id: "cGVvcGxlOjEw") {  # Obi-Wan Kenobi
    name
    characterStats(minAge: 25, maxAge: 100)
    # Result: "Stats for Obi-Wan Kenobi (Age range: 25-100), Born: 57BBY, Height: 182cm, Species: Human"
  }
}

# Argument-based conditional logic
query FormattedDescriptions {
  person(id: "cGVvcGxlOjU=") {  # Princess Leia
    name
    detailed: formattedDescription(format: "detailed")
    # Result: "Princess Leia (born 19BBY) - brown eyes, brown hair"
    
    yearOnly: formattedDescription(format: "year-only") 
    # Result: "Princess Leia (born 19BBY)"
    
    default: formattedDescription(format: "default")
    # Result: "Princess Leia"
  }
}

# Combined usage of all three approaches
query CombinedVariablesDemo {
  person(id: "cGVvcGxlOjE=") {  # Luke Skywalker
    name
    
    # @Variable with fromArgument examples
    basicProfile: characterProfile(includeDetails: false)
    detailedProfile: characterProfile(includeDetails: true)
    
    # VariableProvider with dynamic computation
    youngStats: characterStats(minAge: 0, maxAge: 30)
    oldStats: characterStats(minAge: 30, maxAge: 100)
    
    # Argument-based conditional logic
    nameOnly: formattedDescription(format: "default")
    yearOnly: formattedDescription(format: "year-only")
    detailed: formattedDescription(format: "detailed")
  }
}
```

### Variables and Variable Providers Examples
```graphql
# Variables with @Variable fromArgument
query BasicProfile {
  person(id: "cGVvcGxlOjE=") {  # Luke Skywalker
    name
    characterProfile(includeDetails: false)
    # Result: "Character Profile: Luke Skywalker (basic info only)"
  }
}

query DetailedProfile {
  person(id: "cGVvcGxlOjE=") {
    name  
    characterProfile(includeDetails: true)
    # Result: "Character Profile: Luke Skywalker, Born: 19BBY, Height: 172cm, Mass: 77.0kg"
  }
}

# VariableProvider with dynamic computation
query CharacterStats {
  person(id: "cGVvcGxlOjEw") {  # Obi-Wan Kenobi
    name
    characterStats(minAge: 25, maxAge: 100)
    # Result: "Stats for Obi-Wan Kenobi (Age range: 25-100), Born: 57BBY, Height: 182cm, Species: Human"
  }
}

# Argument-based conditional logic
query FormattedDescriptions {
  person(id: "cGVvcGxlOjU=") {  # Princess Leia
    name
    detailed: formattedDescription(format: "detailed")
    # Result: "Princess Leia (born 19BBY) - brown eyes, brown hair"
    
    yearOnly: formattedDescription(format: "year-only") 
    # Result: "Princess Leia (born 19BBY)"
    
    default: formattedDescription(format: "default")
    # Result: "Princess Leia"
  }
}

# Combined usage of all three approaches
query CombinedVariablesDemo {
  person(id: "cGVvcGxlOjE=") {  # Luke Skywalker
    name
    
    # @Variable with fromArgument examples
    basicProfile: characterProfile(includeDetails: false)
    detailedProfile: characterProfile(includeDetails: true)
    
    # VariableProvider with dynamic computation
    youngStats: characterStats(minAge: 0, maxAge: 30)
    oldStats: characterStats(minAge: 30, maxAge: 100)
    
    # Argument-based conditional logic
    nameOnly: formattedDescription(format: "default")
    yearOnly: formattedDescription(format: "year-only")
    detailed: formattedDescription(format: "detailed")
  }
}
```

### Film Fragment Examples
```graphql
query {
  allFilms(limit: 2) {
    # Standard fields
    title
    director
    
    # Shorthand fragment - delegates to title
    displayTitle
    
    # Full fragment - combines episode, title, director
    summary
    
    # Full fragment - production details
    productionDetails
    
    # Full fragment with character data
    characterCountSummary
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
