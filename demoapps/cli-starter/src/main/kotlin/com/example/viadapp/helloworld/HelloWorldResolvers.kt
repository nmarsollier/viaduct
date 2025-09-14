package com.example.viadapp.helloworld

import com.example.viadapp.helloworld.resolverbases.QueryResolvers
import viaduct.api.Resolver

@Resolver
class GreetingResolver : QueryResolvers.Greeting() {
    override suspend fun resolve(ctx: Context): String {
        return "Hello, World!"
    }
}

@Resolver
class AuthorResolver : QueryResolvers.Author() {
    override suspend fun resolve(ctx: Context): String {
        return "Brian Kernighan"
    }
}
