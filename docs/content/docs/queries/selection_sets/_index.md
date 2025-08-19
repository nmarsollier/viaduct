---
title: Selection Sets
description: The fields requested in a query.
weight: 2
---

The selection set is the set of fields requested in a query. It is a key part of how Viaduct executes queries, as it determines which data will be returned to the client. This set defines which fields the resolver is expected to retrieve from the data source.

* When implementing a resolver, the required selection set indicates precisely which fields must be included. If the resolver tries to access a field not included within its required selection set, it results in an `UnsetSelectionException` at runtime.
* For instance, in a field resolver for `displayName`, the selection set might necessitate the fields `firstName` and `lastName` to construct the display name dynamically.