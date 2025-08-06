package viaduct.demoapp.tenant1

import org.springframework.stereotype.Component
import viaduct.api.Resolver
import viaduct.demoapp.tenant1.resolverbases.QueryResolvers

@Component
@Resolver
class HelloWorldResolver : QueryResolvers.HelloWorld() {
    override suspend fun resolve(ctx: Context): String {
        return "Hello World!"
    }
}
