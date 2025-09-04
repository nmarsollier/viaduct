---
title: Viaduct Roadmap
linkTitle: Roadmap
menu: {main: {weight: 30}}
---

{{% blocks/cover title="Viaduct Roadmap" image_anchor="bottom" height="auto" %}}

Feature Support in the Engine and API.
{.mt-5}

{{% /blocks/cover %}}
{{% blocks/section color="white" %}}
## Feature Support

| Name                                | Status            | Description                                                                                                                                                                                                                                                                                             |
|-------------------------------------| ----------------- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Resolvers MVP                       | Released          | [/docs/resolvers](/docs/resolvers)                                                                                                                                                                                                                                                                      |
| Subqueries                          | Released          | [/docs/queries/subqueries](/docs/resolvers/subqueries)                                                                                                                                                                                                                                                  |
| Observability                       | Released          | [/docs/observability](/docs/observability)                                                                                                                                                                                                                                                              |
| Batch resolvers                     | Preview           | Optimized way to fetch multiple fields at once                                                                                                                                                                                                                                                          |
| Multi-tenancy/ multi module support | Released          | [/docs/multiple_modules/](/docs/multiple_modules/)                                                                                                                                                                                                                                                      |
| Object Mapping                      | Under Development | Object mapping allows the mapping of a Thrift object to a GraphQL type                                                                                                                                                                                                                                  |
| Factory Types                       | Planned for Q3    | Factory types are a straight-forward way for tenants to share functions in a Kotlin-native manner without breaking our principle of interacting “only through the graph.”  More specifically, a factory type defines one or more factory functions that can be used by other modules to construct GRTs. |
| Named Fragments                     | Planned for Q3    | Reusable part of a GraphQL query that you can define once and use in multiple required selection sets.                                                                                                                                                                                                  |
| Visibility                          | Planned for Q4    | Implement a @visibility directive that controls what internal module code can see.                                                                                                                                                                                                                      |

{{% /blocks/section %}}
