@file:Suppress("unused", "ClassName")

package viaduct.tenant.tutorials.variablesdirective

import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.api.Variable
import viaduct.api.Variables
import viaduct.api.VariablesProvider
import viaduct.api.context.VariablesProviderContext
import viaduct.api.types.Arguments
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.tutorials.variablesdirective.resolverbases.QueryResolvers
import viaduct.tenant.tutorials.variablesdirective.resolverbases.UserResolvers

/**
 * Demonstrates Viaduct's ability to use variables to control GraphQL directives
 * (like @include and @skip) within resolver selection sets. This enables conditional field
 * fetching based on runtime conditions.
 *
 * There are 3 main patterns for controlling directive variables:
 * Pattern 1: Declarative variables from arguments (no code)
 * Pattern 2: VariablesProvider with no arguments access
 * Pattern 3: VariablesProvider with resolver arguments access
 */
class VariablesDirectivesFeatureAppTest : FeatureAppTestBase() {
    // Data Source
    companion object {
        data class UserModel(val id: String, val name: String)

        val USER1 = UserModel("user-123", "John Doe")
        val USER2 = UserModel("user-456", "Jane Smith")
        val REVIEWS_USER_1_ANONYMOUS = listOf("Bad!", "Won't buy again")
        val REVIEWS_USER_1_VERIFIED = listOf("Great product!", "Loved the service", "Will buy again")

        val REVIEWS_USER_2_ANONYMOUS = listOf("Bad Quality", "Fast delivery")
        val REVIEWS_USER_2_VERIFIED = listOf("Defective Product", "Fast delivery")
    }

    override var sdl =
        """
        | #START_SCHEMA
        | type User implements Node @resolver {
        |   id: ID!
        |   name: String!
        |   anonymousReviews: [String!]! @resolver
        |   verifiedReviews: [String!]! @resolver
        |   reviews(anonymous: Boolean!): [String!]! @resolver
        |   computedReviews: [String!]! @resolver
        |   computedReviewsWithArgs(userType: String!): [String!]! @resolver
        | }
        |
        | extend type Query {
        |   user(id: String!): User! @resolver
        | }
        | #END_SCHEMA
        """.trimMargin()

    class UserNodeResolver : NodeResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            val userData = when (val internalID = ctx.id.internalID) {
                USER1.id -> USER1.name
                USER2.id -> USER2.name
                else -> throw IllegalArgumentException("User not found: $internalID")
            }

            return User.Builder(ctx)
                .id(ctx.id)
                .name(userData)
                .build()
        }
    }

    @Resolver
    class QueryUserResolver : QueryResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            return ctx.nodeFor(ctx.globalIDFor(User.Reflection, ctx.arguments.id))
        }
    }

    @Resolver("id")
    class UserAnonymousReviews : UserResolvers.AnonymousReviews() {
        override suspend fun resolve(ctx: Context): List<String> {
            return when (ctx.objectValue.getId().internalID) {
                USER1.id -> REVIEWS_USER_1_ANONYMOUS
                USER2.id -> REVIEWS_USER_2_ANONYMOUS
                else -> emptyList()
            }
        }
    }

    @Resolver("id")
    class UserVerifiedReviews : UserResolvers.VerifiedReviews() {
        override suspend fun resolve(ctx: Context): List<String> {
            return when (ctx.objectValue.getId().internalID) {
                USER1.id -> REVIEWS_USER_1_VERIFIED
                USER2.id -> REVIEWS_USER_2_VERIFIED
                else -> emptyList()
            }
        }
    }

    /**
     * Pattern 1 - Declarative - no code. We can control @include directive using variables
     * populated by the `variables` parameter to `@Resolver`. The variable comes directly
     * from the resolver's arguments.
     */
    @Resolver(
        """
        fragment _ on User {
            anonymousReviews @include(if: ${'$'}anonymousVar)
            verifiedReviews
        }
        """,
        variables = [Variable("anonymousVar", fromArgument = "anonymous")]
    )
    class UserReviewsResolver : UserResolvers.Reviews() {
        override suspend fun resolve(ctx: Context): List<String> {
            return if (ctx.arguments.anonymous) {
                ctx.objectValue.getAnonymousReviews() + ctx.objectValue.getVerifiedReviews()
            } else {
                ctx.objectValue.getVerifiedReviews()
            }
        }
    }

    @Test
    fun `Pattern 1 - Using variables from resolver arguments to control @include directive`() {
        execute("{ user(id: \"${USER1.id}\") { name reviews(anonymous: true) } }").assertEquals {
            "data" to {
                "user" to {
                    "name" to USER1.name
                    "reviews" to REVIEWS_USER_1_ANONYMOUS + REVIEWS_USER_1_VERIFIED
                }
            }
        }

        execute("{ user(id: \"${USER1.id}\") { name reviews(anonymous: false) } }").assertEquals {
            "data" to {
                "user" to {
                    "name" to USER1.name
                    "reviews" to REVIEWS_USER_1_VERIFIED
                }
            }
        }
    }

    /**
     * Pattern 2: VariablesProvider - We can control @include directive using VariablesProvider<Arguments.NoArguments>
     * which computes variables at runtime. This resolver has no arguments, so it uses computed logic
     * to determine directive behavior.
     */
    @Resolver(
        """
        fragment _ on User {
            anonymousReviews @include(if: ${'$'}anonymousVar)
            verifiedReviews
        }
        """
    )
    class UserComputedReviews : UserResolvers.ComputedReviews() {
        override suspend fun resolve(ctx: Context): List<String> {
            return try {
                ctx.objectValue.getAnonymousReviews() + ctx.objectValue.getVerifiedReviews()
            } catch (ex: Exception) {
                ctx.objectValue.getVerifiedReviews()
            }
        }

        @Variables("anonymousVar: Boolean")
        class Vars : VariablesProvider<Arguments.NoArguments> {
            override suspend fun provide(context: VariablesProviderContext<Arguments.NoArguments>): Map<String, Any> {
                return mapOf(
                    "anonymousVar" to false
                )
            }
        }
    }

    @Test
    fun `Pattern 2 - Using VariablesProvider with no arguments to control @include directive`() {
        execute("{ user(id: \"${USER2.id}\") { name computedReviews } }").assertEquals {
            "data" to {
                "user" to {
                    "name" to USER2.name
                    "computedReviews" to REVIEWS_USER_2_VERIFIED
                }
            }
        }
    }

    /**
     * Pattern 3: VariablesProvider - We can control @skip directive using VariablesProvider<Generated_Arguments>
     * which has access to the resolver's input arguments. This allows dynamic directive control
     * based on the arguments passed to the resolver.
     */
    @Resolver(
        """
        fragment _ on User {
            anonymousReviews @skip(if: ${'$'}skipAnonymous)
            verifiedReviews
        }
        """
    )
    class UserComputedReviewsWithArgs : UserResolvers.ComputedReviewsWithArgs() {
        override suspend fun resolve(ctx: Context): List<String> {
            return try {
                ctx.objectValue.getAnonymousReviews() + ctx.objectValue.getVerifiedReviews()
            } catch (ex: Exception) {
                ctx.objectValue.getVerifiedReviews()
            }
        }

        @Variables("skipAnonymous: Boolean")
        class Vars : VariablesProvider<User_ComputedReviewsWithArgs_Arguments> {
            override suspend fun provide(context: VariablesProviderContext<User_ComputedReviewsWithArgs_Arguments>): Map<String, Any> {
                val shouldSkipAnonymous = when (context.args.userType) {
                    "verified" -> true
                    else -> false
                }
                return mapOf("skipAnonymous" to shouldSkipAnonymous)
            }
        }
    }

    @Test
    fun `Pattern 3 - Using VariablesProvider with resolver arguments to control @skip directive`() {
        execute("{ user(id: \"${USER1.id}\") { name computedReviewsWithArgs(userType: \"verified\") } }").assertEquals {
            "data" to {
                "user" to {
                    "name" to USER1.name
                    "computedReviewsWithArgs" to REVIEWS_USER_1_VERIFIED
                }
            }
        }

        execute("{ user(id: \"${USER2.id}\") { name computedReviewsWithArgs(userType: \"guest\") } }").assertEquals {
            "data" to {
                "user" to {
                    "name" to USER2.name
                    "computedReviewsWithArgs" to REVIEWS_USER_2_ANONYMOUS + REVIEWS_USER_2_VERIFIED
                }
            }
        }
    }
}
