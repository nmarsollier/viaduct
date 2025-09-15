package viaduct.demoapp.resolvers

import org.springframework.stereotype.Component
import viaduct.api.Resolver
import viaduct.demoapp.resolvers.resolverbases.QueryResolvers

@Component
@Resolver
class HelloWorldResolver : QueryResolvers.HelloWorld() {
    override suspend fun resolve(ctx: Context): String {
        return "Hello World!"
    }
}
