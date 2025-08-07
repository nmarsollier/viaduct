# Star Wars GraphQL Demo

This demo application showcases a comprehensive GraphQL API implementation using Viaduct's custom directives. The demo features Star Wars characters, films, planets, and other entities with full relationship mapping.

## Custom Directives Demonstrated

### @resolver
**Purpose**: Indicates that a field or object requires custom resolution logic rather than simple property access.

**Usage**: Applied to fields that need runtime computation, database lookups, or complex business logic.

**Fragment Syntax**: The @resolver directive supports two types of fragment syntax for efficient field resolution:

#### 1. Shorthand Fragment Syntax
```kotlin
@Resolver("fieldName")
class MyFieldResolver {
    fun resolve(obj: MyType): String {
        // Automatically delegates to the specified field
        return obj.fieldName
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
    fun resolve(obj: MyType): String {
        // Can access all specified fields
        return "${obj.field1} - ${obj.field2} (${obj.field3})"
    }
}
```

**Use cases**:
- Computed fields requiring multiple source fields
- Performance optimization by specifying exact field requirements
- Complex business logic combining multiple attributes

**Example**:
```graphql
type Person {
  name: String @resolver
  homeworld: Planet @resolver      # Standard resolver
  displayName: String @resolver    # Shorthand fragment: @Resolver("name")
  summary: String @resolver         # Full fragment: @Resolver("fragment _ on Person { name birthYear }")
  appearanceDescription: String @resolver  # Full fragment with multiple fields
}
```

**Implementation**: Each `@resolver` field has a corresponding resolver class with optional fragment syntax for efficient field access.

### @backingData
**Purpose**: Specifies a backing data class for complex field resolution, particularly useful for connection fields requiring pagination.

**Usage**: Applied to connection fields to specify the implementation class for pagination and filtering.

**Example**:
```graphql
type Person {
  filmConnection(first: Int, after: String): PersonFilmsConnection
    @resolver
    @backingData(class: "starwars.person.FilmConnection")
}
```

**Implementation**: The specified class provides pagination logic and data transformation for GraphQL connections.

### @scope
**Purpose**: Restricts schema availability to specific tenants or contexts, enabling multi-tenant GraphQL schemas.

**Usage**: Applied to types, interfaces, and query fields to limit access to specific scopes.

**Example**:
```graphql
type Query @scope(to: ["starwars"]) {
  allPeople: [Person]
}

type Person @scope(to: ["starwars", "admin"]) {
  name: String
}
```

**Implementation**: The entire demo is scoped to "starwars" tenant, demonstrating isolated schema access.

### @idOf
**Purpose**: Specifies global ID type association and validation for proper type checking.

**Usage**: Applied to ID fields and arguments to associate them with specific GraphQL types.

**Example**:
```graphql
type Query {
  person(id: ID! @idOf(type: "Person")): Person
}

type Person {
  id: ID! @idOf(type: "Person")
}
```

**Implementation**: Enables type-safe ID handling and validation across the GraphQL schema.

### @oneOf
**Purpose**: Ensures input objects have exactly one non-null field, useful for union-like input types.

**Usage**: Applied to input types where only one option should be specified.

**Example**:
```graphql
input PersonSearchInput @oneOf {
  byName: String
  byId: ID
  byBirthYear: String
}

type Query {
  searchPerson(search: PersonSearchInput!): Person
}
```

**Implementation**: Validates that exactly one search criterion is provided, preventing ambiguous queries.

## Data Model

The demo includes comprehensive Star Wars data:

### Characters (People)
- Luke Skywalker
- Princess Leia
- Han Solo
- Darth Vader
- Obi-Wan Kenobi

### Films
- A New Hope (Episode IV)
- The Empire Strikes Back (Episode V)
- Return of the Jedi (Episode VI)

### Planets
- Tatooine
- Alderaan
- Corellia
- Stewjon

### Starships & Vehicles
- Millennium Falcon
- X-wing
- Speeder bikes

## Architecture

### In-Memory Storage
The demo uses `StarWarsData` object for in-memory data storage, making it easy to understand and modify without database dependencies.

### Resolver Pattern
Each `@resolver` field has a dedicated resolver class:
- `PersonNameResolver` for `Person.name`
- `PersonHomeworldResolver` for `Person.homeworld`
- `AllPeopleResolver` for `Query.allPeople`

### Connection Implementation
GraphQL connections are implemented using backing data classes that handle pagination:
- `PeopleConnection` for paginated people queries
- `FilmConnection` for person-film relationships
- `StarshipConnection` for person-starship relationships

## Key Features

1. **Multi-tenant Schema**: Demonstrates `@scope` directive for tenant isolation
2. **Type-safe IDs**: Uses `@idOf` for proper ID validation
3. **Complex Relationships**: Shows related entities with proper resolution
4. **Pagination Support**: Implements GraphQL connection pattern
5. **Input Validation**: Demonstrates `@oneOf` for exactly-one-field semantics
6. **Comprehensive Documentation**: All directives are thoroughly documented

## Example Queries

### Basic Person Query
```graphql
query {
  person(id: "1") {
    name
    birthYear
    homeworld {
      name
    }
  }
}
```

### Paginated People Query
```graphql
query {
  allPeople(first: 5) {
    people {
      name
      eyeColor
    }
    totalCount
    pageInfo {
      hasNextPage
    }
  }
}
```

### Search with @oneOf
```graphql
query {
  searchPerson(search: { byName: "Luke" }) {
    name
    birthYear
  }
}
```

### Complex Relationships
```graphql
query {
  person(id: "1") {
    name
    filmConnection(first: 3) {
      films {
        title
        director
      }
    }
    starshipConnection(first: 2) {
      starships {
        name
        model
      }
    }
  }
}
```

### Fragment Syntax Examples
```graphql
query {
  person(id: "1") {
    # Standard resolved field
    name
    
    # Shorthand fragment syntax - delegates to name field
    displayName
    
    # Full fragment syntax - combines name and birthYear
    displaySummary
    
    # Full fragment syntax - combines multiple appearance fields
    appearanceDescription
  }
}
```

### Film Fragment Examples
```graphql
query {
  allFilms(first: 2) {
    films {
      # Standard fields
      title
      director
      
      # Shorthand fragment - delegates to title
      displayTitle
      
      # Full fragment - combines episode, title, director
      summary
      
      # Full fragment - production details
      productionDetails
      
      # Full fragment with connection data
      characterCountSummary
    }
  }
}
```

## Building and Running

This demo is part of the Treehouse monorepo and uses Bazel for building:

```bash
# Build the demo
bazel build //projects/viaduct/oss/demoapps/starwars:starwars_demo

# Run tests
bazel test //projects/viaduct/oss/demoapps/starwars/...
```

The implementation demonstrates how Viaduct's custom directives enable powerful GraphQL schema capabilities while maintaining clean, understandable code organization.