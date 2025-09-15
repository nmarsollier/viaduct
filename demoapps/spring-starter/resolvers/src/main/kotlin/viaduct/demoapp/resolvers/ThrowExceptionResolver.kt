package viaduct.demoapp.resolvers

import org.springframework.stereotype.Component
import viaduct.api.Resolver
import viaduct.demoapp.resolvers.resolverbases.QueryResolvers

@Component
@Resolver
class ThrowExceptionResolver : QueryResolvers.ThrowException() {
    override suspend fun resolve(ctx: Context): String {
        throw IllegalStateException("This is a resolver error")
    }
}
