---
title: Responsibility Sets
description: Also known as "responsibility selection sets", these are the fields a resolver is responsible for returning.
weight: 2
---

“Responsibility sets” (also known as “responsibility selection sets”) are an important concept in the Viaduct Modern API.  Every resolver is “responsible” for returning the fields in its responsibility set.  This is all fields, including nested fields, that themselves do not have a resolver.  In our example, the node resolver for `User` is responsible for returning the `id`, `firstName`, and `lastName` fields, but not the `displayName` field, because that field has its own resolver.  If the `User` type had a field whose type is a `Node` – for example, a `listings: [Listing]` field, the `User`’s node resolver would *not* be responsible for resolving that field, because node values are resolved by their node resolvers.

In our example, the `displayName` field has a scalar type, so the responsibility set for that resolver is just that scalar value.  If `displayName` returned a (non-node) GraphQL object type, then the `displayName` resolver would be responsible for all fields (recursively) in that object type that do not have their own resolvers.