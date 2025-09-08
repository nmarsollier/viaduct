# Viaduct Spring Demo

## Requirements

- Java JDK 21
- JAVA_HOME environment variable
- java in classpath

## Execution

```bash
./gradlew bootRun
```

From IntelliJ :

Add this line to VM Params in Run Configuration

```
--add-opens java.base/java.lang=ALL-UNNAMED
```

## Test the Hello World

Start the server and access to the following URL to bring graphiql interface up

[http://localhost:8080/graphiql?path=/graphql](http://localhost:8080/graphiql?path=/graphql)

> Note that the default url will load a different graphql schema, this needs to be the same as above

Then, run the following query

```graphql
query HelloWorld {
  helloWorld
}
```

You should see this response

```
{
  "data" {
    "helloWorld": "Hello World!"
  }
}
```
