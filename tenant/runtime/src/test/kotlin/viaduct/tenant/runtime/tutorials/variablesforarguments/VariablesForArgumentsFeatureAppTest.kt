@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.tutorials.variablesforarguments

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.api.Variable
import viaduct.api.Variables
import viaduct.api.VariablesProvider
import viaduct.api.context.VariablesProviderContext
import viaduct.api.types.Arguments
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.runtime.tutorials.variablesforarguments.resolverbases.QueryResolvers

/**
 * Demonstrates Viaduct's powerful ability to use variables to control what arguments to give
 * to fields in required selection sets. There are different patterns that can be implemented
 * to control the arguments of a field.
 *
 * Given a resolver with Arguments, there are 3 main Patterns that can be used to control the arguments that reach
 * that resolver.
 */
class VariablesForArgumentsFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | extend type Query {
        |   getPosts(userId: String!, status: String!): String @resolver
        |   userPosts(userId: String!): String @resolver
        |   latestPosts: String @resolver
        |   dashboardPosts(userType: String!): String @resolver
        | }
        | #END_SCHEMA
        """.trimMargin()

    /**
     * In these tutorials, here's the implementation of a resolver that takes
     * arguments.  We're going to explore different ways you can use
     * this resolver in a required selection set, and in particular how your
     * can use variables to control what arguments this resolver will get.
     */
    @Resolver
    class QueryGetPostsResolver : QueryResolvers.GetPosts() {
        override suspend fun resolve(ctx: Context): String {
            val userId = ctx.arguments.userId
            val status = ctx.arguments.status
            return "Posts for $userId with status $status"
        }
    }

    /**
     * Pattern 1 - Declarative - no code.  We can provide arguments to `getPosts` using either constant
     * values or using a variable populated by the `variables` parameter to `@Resolver`.  No code
     * is needed to populate variables in this way.
     */
    @Resolver(
        """
        fragment _ on Query {
            getPosts(userId: ${'$'}userIdVar, status: "published")
        }
        """,
        variables = [Variable("userIdVar", fromArgument = "userId")]
    )
    class QueryUserPostsResolver : QueryResolvers.UserPosts() {
        override suspend fun resolve(ctx: Context): String = ctx.objectValue.get("getPosts", String::class)
    }

    @Test
    fun `Pattern 1 - Using operation that statically injects the variables`() {
        execute("{ userPosts(userId: \"john\") }").assertEquals {
            "data" to {
                "userPosts" to "Posts for john with status published"
            }
        }
    }

    /**
     * Pattern 2: VariablesProvider - We can provide arguments to `getPosts` using VariablesProvider<Arguments>
     * which populates the `variables` parameter inside the @Resolver from the map inside the overridden provide function.
     * and directly injecting them into the arguments of the default implementation.
     * */
    @Resolver(
        """
        fragment _ on Query {
            getPosts(userId: ${'$'}currentUser, status: "published")
        }
        """
    )
    class QueryLatestPostsResolver : QueryResolvers.LatestPosts() {
        override suspend fun resolve(ctx: Context): String = ctx.objectValue.get("getPosts", String::class)

        // @Variables contains the name of the parameters to be replaced in query within the @Resolver. They should
        // be returned as a Map of String(Parameter Name) to Value
        @Variables("currentUser: String!")
        class LatestPostsProvider : VariablesProvider<Arguments> {
            override suspend fun provide(context: VariablesProviderContext<Arguments>): Map<String, Any?> {
                // Computing the name -- This would typically come from the HTTP request context
                val currentUser = "user"

                return mapOf(
                    "currentUser" to currentUser,
                )
            }
        }
    }

    @Test
    fun `Pattern 2 - variables provided from VariablesProvider - Arguments `() {
        execute("{ latestPosts }").assertEquals {
            "data" to {
                "latestPosts" to "Posts for user with status published"
            }
        }
    }

    /**
     * Pattern 3: VariablesProvider - We can provide arguments to `getPosts` using VariablesProvider<Query_`Field`_Arguments>
     * which populates the `variables` parameter inside the @Resolver from the map, while allowing access to input arguments
     * that were received during the execution.
     */
    @Resolver(
        """
        fragment _ on Query {
            getPosts(userId: ${'$'}targetUser, status: ${'$'}statusFilter)
        }
        """
    )
    class QueryDashboardPostsResolver : QueryResolvers.DashboardPosts() {
        override suspend fun resolve(ctx: Context): String = ctx.objectValue.get("getPosts", String::class)

        /**
         * The VariablesProvider uses the Arguments that Viaduct generates for the Given query. This preserves the context
         * that the query was executed, and gives the ability of using the arguments that has been provided, to compute the
         * variables differently
         */
        @Variables("targetUser: String!, statusFilter: String!")
        class DashboardProvider : VariablesProvider<Query_DashboardPosts_Arguments> {
            override suspend fun provide(context: VariablesProviderContext<Query_DashboardPosts_Arguments>): Map<String, Any?> {
                // Access the userType argument passed to dashboardPosts via the query.
                val userType = context.args.userType

                return when (userType) {
                    "admin" -> mapOf(
                        "targetUser" to "admin",
                        "statusFilter" to "all"
                    )

                    else -> mapOf(
                        "targetUser" to "guest",
                        "statusFilter" to "published"
                    )
                }
            }
        }
    }

    @Test
    fun `Pattern 2 - variables provided from VariablesProvider - Generated Arguments`() {
        execute("{ dashboardPosts(userType: \"admin\") }").assertEquals {
            "data" to {
                "dashboardPosts" to "Posts for admin with status all"
            }
        }

        execute("{ dashboardPosts(userType: \"not known\") }").assertEquals {
            "data" to {
                "dashboardPosts" to "Posts for guest with status published"
            }
        }
    }
}
