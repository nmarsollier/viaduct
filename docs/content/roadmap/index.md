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

| Name                                | Status             | Description                                                                                                                                                                                                                                                                                            |
|-------------------------------------|--------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Resolvers MVP                       | Released           | [/docs/resolvers](/docs/resolvers)                                                                                                                                                                                                                                                                     |
| Subqueries                          | Released           | [/docs/queries/subqueries](/docs/resolvers/subqueries)                                                                                                                                                                                                                                                 |
| Observability                       | Released           | [/docs/observability](/docs/observability)                                                                                                                                                                                                                                                             |
| Scopes                              | Releases           | [/docs/scopes](/docs/scopes)                                                                                                                                                                                                                                                                           |
| Multi-tenancy/ multi module support | Released           | [/docs/multiple_modules/](/docs/multiple_modules/)                                                                                                                                                                                                                                                     |
| Batch resolvers                     | Preview            | [/docs/resolvers/batch_resolution](/docs/resolvers/batch_resolution/)                                                                                                                                                                                                                                  |
| Mutations                           | Preview            | [/docs/mutations](/docs/mutations)                                                                                                                                                                                                                                                                     |
| Object Mapping                      | Under Development  | Object mapping allows the mapping of a generic object type (like a Thrift object type) to a GraphQL type                                                                                                                                                                                               |
| Factory Types                       | Planned for Q4 '25 | Factory types are a straight-forward way for tenants to share functions in a Kotlin-native manner without breaking our principle of interacting “only through the graph.” More specifically, a factory type defines one or more factory functions that can be used by other modules to construct GRTs. |
| Named Fragments                     | Planned for Q3 '25 | Reusable part of a GraphQL query that you can define once and use in multiple required selection sets.                                                                                                                                                                                                 |
| Visibility                          | Planned for Q4 '25 | Implement a @visibility directive that controls what internal module code can see.                                                                                                                                                                                                                     |
| Subscriptions                       | Planned for H! '26 | Support for [GraphQL Subscriptions](https://graphql.org/learn/subscriptions/)                                                                                                                                                                                                                          |
| Parent/Child Relationships          | Planned for H1 '26 | In the context of Viaduct, parent-child relationships define hierarchical or associated data relationships across GraphQL types. These relationships allow one type (the parent) to reference or contain another type (the child), enabling structured data querying and retrieval.                    |
| AI generated mock data              | Planned for H1 '26 | When testing Viaduct resolvers, engineers need to manually mock out data for these fragments, which is time-consuming and can eventually lead to mocks getting out of sync with the fragments they implement as resolvers evolve over time. This effort will aid with auto-generating mock data.       |
| Connections                         | Planned for H1 '26 | Support for [GraphQL Connections](https://relay.dev/graphql/connections.htm)                                                                                                                                                                                                                           |

{{% /blocks/section %}}
