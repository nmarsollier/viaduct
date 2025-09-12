---
title: Observability
description: Monitoring and Logging in Viaduct
weight: 20
---

## Metrics

Viaduct provides built-in metrics on fields, field resolvers, node resolvers and access checkers ("measured entities"). These metrics are focused on latency, error rates and attribution. Viaduct offers observability to support the following use cases:

### Latency

* Determine latency (across various percentiles) in aggregate for measured entities
* Figure out the sequence of executions of measured entities happening in a given request that is contributing to latency (critical path)
* Understand why each measured entity is getting called / executed
* Attribute each measured entity running in a request to a specific tenant module

### Error Rate

* Monitor fail states of operations (either partial or full failure) on Viaduct
  * What is the error rate for a given operation?
  * If the error rate is greater than zero, what ‘measured entities’ are causing the error?
  * What is the cause?
* Understand what is causing each operation failure
  * Errors are attributable to the responsible ‘measured entity’(field or resolver) that triggered the exception
  * Errors in resolvers should propagated to the field that resulted in the resolver being called
  * Be able to find exception stack trace (when applicable)
* Monitor operation for error rate
  * Errors are eventually propagated to the top level operation to signify if the operation failed or not

### Attribution

Due to the async nature of Viaduct’s execution strategy and use of caching, this requires special callout. Viaduct’s observability supports the following use cases:

* Developers are able to understand why their field is slow and to attribute the slowness to a specific measure entity.
  * For example, the writer of a DerivedFieldProvider wants to understand why their field is slow.
* Developers are able to understand why an error is thrown and attribute the error to a specific code component.
  * Similarly as the above example, we need a way to help DFP B understand how field A errors are impacting its execution.
* Developers are also able to understand what code component triggered the fetch for their field, and what the frequency is for the fetch.
  * If taking the above example, field A should be able to know DFP B triggers its fetch and the frequency of the fetch.

## Available Metrics

1. viaduct.execution
* Full execution lifecycle metric which measures end-to-end execution time for the entire GraphQL request
  * Measurements:
    * Latency (p50, p75, p90, p95) and count
  * Tags:
    * operation_name: GraphQL operation name (if available)
    * success: true if no throwable and data is present, false otherwise
2. viaduct.operation
* Operation-level metric which measures the time to execute the specific GraphQL operation
  * Measurements:
     * Latency (p50, p75, p90, p95) and count
  * Tags:
    * operation_name: GraphQL operation definition name (if available)
3. viaduct.field
* Field-level metric which measures the time to fetch/resolve individual GraphQL fields
  * Measurements:
    * Latency (p50, p75, p90, p95) and count
  * Tags:
    * operation_name: GraphQL operation name (if available)
    * field: Field path in format ParentType.fieldName or just fieldName for root fields
    * success: true if no exception thrown during field fetch, false otherwise
