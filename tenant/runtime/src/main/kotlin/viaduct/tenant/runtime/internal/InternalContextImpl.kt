package viaduct.tenant.runtime.internal

import graphql.schema.GraphQLSchema
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.InternalContext
import viaduct.api.internal.ReflectionLoader

class InternalContextImpl(
    override val schema: GraphQLSchema,
    override val globalIDCodec: GlobalIDCodec,
    override val reflectionLoader: ReflectionLoader
) : InternalContext
