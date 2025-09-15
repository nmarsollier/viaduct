# Star Wars GraphQL Demo

A comprehensive GraphQL API demo showcasing Viaduct's custom directives, batch resolvers, and performance optimizations using Star Wars data.

## Quick Start

For more information, check out the [Viaduct Getting Started](https://airbnb.io/viaduct/docs/getting_started/) docs

### 1. Start the demo app

```bash
./gradlew bootRun
```

The server will start on `http://localhost:8080`

### 2. Access GraphiQL

Open your browser and go to [http://localhost:8080/graphiql]()

### 3. Try Example Queries

#### Basic Characters Query
```graphql
query {
  allCharacters(limit: 5) {
    id
    name
    homeworld { name }
  }
}
```

#### Complex Query with Batch Resolution
```graphql
query {
  allCharacters(limit: 3) {
    name
    homeworld { name }
    species { name }
    filmCount
    richSummary
  }
}
```

#### Film Query with Characters
```graphql
query {
  allFilms {
    title
    director
    mainCharacters {
      name
      homeworld { name }
    }
  }
}
```

## Technical Deep Dive

For comprehensive technical details, see [DEEPDIVE.md](DEEPDIVE.md).
