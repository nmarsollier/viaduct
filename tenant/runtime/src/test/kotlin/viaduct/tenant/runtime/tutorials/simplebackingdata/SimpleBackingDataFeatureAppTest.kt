@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.tutorials.simplebackingdata

import graphql.schema.GraphQLScalarType
import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.Scalars
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.runtime.tutorials.simplebackingdata.resolverbases.QueryResolvers
import viaduct.tenant.runtime.tutorials.simplebackingdata.resolverbases.UserResolvers

/**
 * Demonstrates Viaduct's Backing Data feature for efficient data fetching.
 *
 * Backing Data allows you to store internal Kotlin objects that aren't exposed in the GraphQL schema
 * but can be shared across multiple field resolvers. This prevents redundant database calls when
 * multiple fields need data from the same expensive operation.
 *
 * The backing data object stays in Kotlin - it's never serialized to GraphQL. Field resolvers
 * can extract specific values from it, so one database call can power multiple GraphQL fields.
 */
class SimpleBackingDataFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        | #START_SCHEMA
        | scalar BackingData
        | directive @resolver on FIELD_DEFINITION | OBJECT
        | directive @backingData(class: String!) on FIELD_DEFINITION
        |
        | interface Node {
        |     id: ID!
        | }
        |
        | type Query {
        |   user(id: String!): User! @resolver
        | }
        |
        | type User implements Node @resolver {
        |   id: ID!
        |   name: String!
        |   email: String!
        |   averageStars: Float! @resolver
        |   reviewsCount: Int! @resolver
        |   reviewsData: BackingData
        |     @resolver
        |     @backingData(class: "UserReviewsData")
        | }
        | #END_SCHEMA
        """.trimMargin()

    override var customScalars: List<GraphQLScalarType> = listOf(
        Scalars.BackingData
    )

    // Resolves User nodes by ID, fetching basic user data from a simulated database.
    // This acts as the primary data source for User objects in the graph.
    @Resolver
    class UserNodeResolver : NodeResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            val userData = when (val internalId = ctx.id.internalID) {
                "user-123" -> Pair("John Smith", "john@example.com")
                "user-456" -> Pair("Jane Doe", "jane@example.com")
                else -> throw IllegalArgumentException("User not found: $internalId")
            }

            return User.Builder(ctx)
                .id(ctx.id)
                .name(userData.first)
                .email(userData.second)
                .build()
        }
    }

    // Query resolver that converts string IDs to User objects.
    // Handles the top-level user(id:) query field by delegating to the node resolver.
    @Resolver
    class UserQueryResolver : QueryResolvers.User() {
        override suspend fun resolve(ctx: Context): User {
            return ctx.nodeFor(ctx.globalIDFor(User.Reflection, ctx.arguments.id))
        }
    }

    // Resolver uses the objectValue.getId to get the GlobalID from the node and extract the internal ID from it.
    // The resolve function returns the Kotlin-Object defined in schema, rather than a Generated one.
    // When multiple field-resolvers request data from a BackingData it will run it once and provide the result for both of them.
    @Resolver(
        """fragment _ on User { id }"""
    )
    class UserReviewsDataResolver : UserResolvers.ReviewsData() {
        override suspend fun resolve(ctx: Context): UserReviewsData {
            val userId = ctx.objectValue.getId().internalID
            // This in theory stimulates a call to another Microservice, External API, or other sources.
            return when (userId) {
                "user-123" -> MOCK_USER_123_REVIEWS
                "user-456" -> MOCK_USER_456_REVIEWS
                else -> UserReviewsData(averageRating = 0.0, totalReviews = 0)
            }
        }
    }

    // Extracts the average rating from the shared backing data.
    // Uses the pre-fetched UserReviewsData instead of making a separate service call.
    @Resolver(
        """
        fragment _ on User { reviewsData }
        """
    )
    class UserAverageStarsResolver : UserResolvers.AverageStars() {
        override suspend fun resolve(ctx: Context): Double {
            val reviewsData = ctx.objectValue.get<UserReviewsData>("reviewsData", UserReviewsData::class)
            return reviewsData.averageRating
        }
    }

    // Extracts the review count from the shared backing data.
    // Reuses the same UserReviewsData object that was fetched for averageStars.
    @Resolver(
        """fragment _ on User { reviewsData }"""
    )
    class UserReviewsCountResolver : UserResolvers.ReviewsCount() {
        override suspend fun resolve(ctx: Context): Int {
            val reviewsData = ctx.objectValue.get<UserReviewsData>("reviewsData", UserReviewsData::class)
            return reviewsData.totalReviews
        }
    }

    @Test
    fun `Backing data enables multiple fields to share expensive reviews service call`() {
        execute(
            query = """
                query {
                    user(id: "user-123") {
                        name
                        email
                        averageStars
                        reviewsCount
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "name" to "John Smith"
                    "email" to "john@example.com"
                    "averageStars" to MOCK_USER_123_REVIEWS.averageRating
                    "reviewsCount" to MOCK_USER_123_REVIEWS.totalReviews
                }
            }
        }
    }

    @Test
    fun `Individual review fields can be queried independently`() {
        execute(
            query = """
                query {
                    user(id: "user-456") {
                        averageStars
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "averageStars" to MOCK_USER_456_REVIEWS.averageRating
                }
            }
        }
    }

    @Test
    fun `Main database fields work independently of reviews data`() {
        execute(
            query = """
                query {
                    user(id: "user-123") {
                        name
                        email
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "user" to {
                    "name" to "John Smith"
                    "email" to "john@example.com"
                }
            }
        }
    }

    companion object {
        val MOCK_USER_123_REVIEWS = UserReviewsData(
            averageRating = 4.2,
            totalReviews = 127,
        )

        val MOCK_USER_456_REVIEWS = UserReviewsData(
            averageRating = 3.8,
            totalReviews = 43,
        )
    }
}

data class UserReviewsData(
    val averageRating: Double,
    val totalReviews: Int,
)
