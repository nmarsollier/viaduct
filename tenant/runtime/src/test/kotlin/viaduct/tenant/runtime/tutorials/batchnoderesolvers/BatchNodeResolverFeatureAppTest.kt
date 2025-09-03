@file:Suppress("unused", "ClassName")

package viaduct.tenant.runtime.tutorials.batchnoderesolvers

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.assertEquals as kotlinAssertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import viaduct.api.FieldValue
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase
import viaduct.tenant.runtime.tutorials.batchnoderesolvers.resolverbases.QueryResolvers

/**
 * Demonstrates Viaduct's Batch Node Resolver feature for efficient object loading.
 *
 * Batch Node Resolvers solve the N+1 problem when fetching multiple objects by ID.
 * Instead of making separate database calls for each product lookup, one batch call
 * can fetch all requested products at once. This is especially useful when a query
 * requests multiple related objects or when fragments cause multiple node lookups.
 *
 * When multiple ctx.nodeFor() calls request the same object type, Viaduct automatically
 * groups them into a single batchResolve() call, similar to DataLoaders but built into
 * the Node Resolver system.
 */
class BatchNodeResolverFeatureAppTest : FeatureAppTestBase() {
    override var sdl = """
        | #START_SCHEMA
        | directive @resolver on FIELD_DEFINITION | OBJECT
        |
        | interface Node {
        |     id: ID!
        | }
        |
        | type Product implements Node @resolver {
        |   id: ID!
        |   name: String!
        |   price: Float!
        |   category: String!
        | }
        |
        | type Query {
        |   products(ids: [String!]!): [Product!]! @resolver
        |   product(id: String!): Product! @resolver
        | }
        | #END_SCHEMA
    """.trimMargin()

    companion object {
        val batchResolveCalls = ConcurrentLinkedQueue<Int>()
    }

    @BeforeEach
    fun setUp() {
        batchResolveCalls.clear()
    }

    /**
     * Batch Node Resolver for Product objects. Instead of calling resolve() separately
     * for each product ID, Viaduct calls batchResolve() once with all requested IDs.
     * This allows for a single optimized database query to fetch all products.
     *
     * batchResolve() receives all contexts that need Product objects and returns
     * corresponding FieldValue results in the same order.
     */
    class ProductNodeResolver : NodeResolvers.Product() {
        override suspend fun batchResolve(contexts: List<Context>): List<FieldValue<Product>> {
            // Extract all internal IDs from the batch of GlobalIDs
            val productIds = contexts.map { ctx -> ctx.id.internalID }

            // Track this batchResolve call - record the batch size
            batchResolveCalls.add(productIds.size)

            // Simulate a single database call that fetches all products at once
            // In reality: SELECT * FROM products WHERE id IN (...)
            val productsData = fetchProductsByIds(productIds)

            // Return results in the same order as input contexts
            return contexts.map { ctx ->
                val productId = ctx.id.internalID
                val productData = productsData[productId]

                if (productData != null) {
                    val product = Product.Builder(ctx)
                        .id(ctx.id)
                        .name(productData.name)
                        .price(productData.price)
                        .category(productData.category)
                        .build()
                    FieldValue.ofValue(product)
                } else {
                    FieldValue.ofError(IllegalArgumentException("Product not found: $productId"))
                }
            }
        }

        // Simulates a single database query for multiple products
        private fun fetchProductsByIds(productIds: List<String>): Map<String, ProductData> {
            // Mock product database - in reality this would be a single DB query
            val allProducts = mapOf(
                "laptop-123" to ProductData("Gaming Laptop", 1299.99, "Electronics"),
                "phone-456" to ProductData("Smartphone", 699.99, "Electronics"),
                "book-789" to ProductData("Kotlin Programming", 49.99, "Books"),
                "chair-101" to ProductData("Office Chair", 299.99, "Furniture"),
                "mouse-202" to ProductData("Wireless Mouse", 29.99, "Electronics")
            )

            // Return only the requested products (simulating WHERE id IN clause)
            return allProducts.filter { it.key in productIds }
        }

        private data class ProductData(
            val name: String,
            val price: Double,
            val category: String
        )
    }

    /**
     * Query resolver that fetches multiple products. Each ctx.nodeFor() call
     * would normally trigger a separate Node Resolver, but with batch resolving
     * they get grouped into a single batchResolve() call automatically.
     */
    @Resolver
    class productsResolver : QueryResolvers.Products() {
        override suspend fun resolve(ctx: Context): List<Product> {
            // Convert each ID to GlobalID and fetch via Node Resolver system
            // All these ctx.nodeFor() calls get batched automatically
            return ctx.arguments.ids.map { id ->
                ctx.nodeFor(ctx.globalIDFor(Product.Reflection, id))
            }
        }
    }

    /**
     * Single product query resolver - also uses Node Resolver system.
     * If called alongside products, this would be included in the same batch.
     */
    @Resolver
    class ProductResolver : QueryResolvers.Product() {
        override suspend fun resolve(ctx: Context): Product {
            return ctx.nodeFor(ctx.globalIDFor(Product.Reflection, ctx.arguments.id))
        }
    }

    @Test
    fun `Batch node resolver efficiently loads multiple products with single database call`() {
        execute(
            query = """
                query {
                    products(ids: ["laptop-123", "phone-456", "book-789"]) {
                        id
                        name
                        price
                        category
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "products" to arrayOf(
                    {
                        "id" to createGlobalIdString(Product.Reflection, "laptop-123")
                        "name" to "Gaming Laptop"
                        "price" to 1299.99
                        "category" to "Electronics"
                    },
                    {
                        "id" to createGlobalIdString(Product.Reflection, "phone-456")
                        "name" to "Smartphone"
                        "price" to 699.99
                        "category" to "Electronics"
                    },
                    {
                        "id" to createGlobalIdString(Product.Reflection, "book-789")
                        "name" to "Kotlin Programming"
                        "price" to 49.99
                        "category" to "Books"
                    }
                )
            }
        }

        // Assert batching efficiency: only 1 batchResolve call for all 3 products
        kotlinAssertEquals(1, batchResolveCalls.size, "Expected exactly 1 batchResolve call for batch loading")
        kotlinAssertEquals(3, batchResolveCalls.first(), "Expected batch size of 3 products in single call")
    }

    @Test
    fun `Batch resolver works with single product`() {
        // Even single product requests use batch resolver (batch size = 1)
        execute(
            query = """
                query {
                    product(id: "chair-101") {
                        name
                        price
                        category
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "product" to {
                    "name" to "Office Chair"
                    "price" to 299.99
                    "category" to "Furniture"
                }
            }
        }

        // Assert: single product still uses batch resolver (batch size 1)
        kotlinAssertEquals(1, batchResolveCalls.size, "Expected exactly 1 batchResolve call for single product")
        kotlinAssertEquals(1, batchResolveCalls.first(), "Expected batch size of 1 product in single call")
    }

    @Test
    @Disabled
    fun `Batch resolver valid and invalid IDs`() {
        execute(
            query = """
                query {
                    products(ids: ["laptop-123", "invalid-id", "phone-456"]) {
                        id
                        name
                        price
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "products" to arrayOf(
                    {
                        "id" to createGlobalIdString(Product.Reflection, "laptop-123")
                        "name" to "Gaming Laptop"
                        "price" to 1299.99
                    },
                    null, // Invalid ID results in null
                    {
                        "id" to createGlobalIdString(Product.Reflection, "phone-456")
                        "name" to "Smartphone"
                        "price" to 699.99
                    }
                )
            }
            "errors" to arrayOf(
                {
                    "message" to "java.lang.IllegalArgumentException: Product not found: invalid-id"
                    "path" to listOf("products", 1)
                }
            )
        }

        // Assert batching even with mixed valid/invalid IDs
        kotlinAssertEquals(1, batchResolveCalls.size, "Expected exactly 1 batchResolve call even with invalid IDs")
        kotlinAssertEquals(3, batchResolveCalls.first(), "Expected batch size of 3 products (including invalid)")
    }

    @Test
    fun `Multiple queries get batched together demonstrating maximum efficiency`() {
        // When multiple queries request products, they all get batched into one call
        execute(
            query = """
                query {
                    product1: product(id: "laptop-123") {
                        name
                        price
                    }
                    product2: product(id: "phone-456") {
                        name
                        category
                    }
                    moreProducts: products(ids: ["book-789", "mouse-202"]) {
                        name
                        price
                    }
                }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "product1" to {
                    "name" to "Gaming Laptop"
                    "price" to 1299.99
                }
                "product2" to {
                    "name" to "Smartphone"
                    "category" to "Electronics"
                }
                "moreProducts" to arrayOf(
                    {
                        "name" to "Kotlin Programming"
                        "price" to 49.99
                    },
                    {
                        "name" to "Wireless Mouse"
                        "price" to 29.99
                    }
                )
            }
        }

        // Assert maximum batching efficiency: all product requests in single call
        // 2 individual products + 2 from products array = 4 total in 1 batch
        kotlinAssertEquals(1, batchResolveCalls.size, "Expected exactly 1 batchResolve call for all product requests")
        kotlinAssertEquals(4, batchResolveCalls.first(), "Expected batch size of 4 products (2 + 2) in single call")
    }
}
