---
title: Error Handling
description: Monitoring and returning errors in Viaduct.
weight: 2
---

## Configuring Custom Error Reporting

Viaduct provides a flexible mechanism for handling errors that occur during data fetching. By default, Viaduct captures exceptions thrown in resolvers and includes them in the `errors` field of the GraphQL response. However, you can also access these exceptions via a {{< kdoc viaduct.engine.api.execution.ResolverErrorReporter >}}.

Link to an {{<kdoc viaduct.service.api.ExecutionInput >}}.

Link to a tenant thing {{< kdoc viaduct.api.context.ExecutionContext >}}.

## Configuring Custom Error Generation

You can customize how errors are built by implementing the `ResolverErrorBuilder` interface. This allows you to define your own logic for converting exceptions thrown during data fetching into `GraphQLError` instances.
