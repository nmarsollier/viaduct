package viaduct.tenant.runtime.execution.submutations

import java.util.UUID
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import viaduct.api.Resolver
import viaduct.graphql.test.assertEquals
import viaduct.tenant.runtime.execution.submutations.resolverbases.MutationResolvers
import viaduct.tenant.runtime.fixtures.FeatureAppTestBase

class SubMutationFeatureAppTest : FeatureAppTestBase() {
    override var sdl =
        """
        |#START_SCHEMA
        |extend type Mutation {
        |  exampleMutationSelections(triangleSize: Int!): Int @resolver
        |  multipleValueBooleanMutation(userId: ID!, name: String!): Boolean @resolver
        |  echoObjMutation(foo: FooInput!): Foo @resolver
        |  simpleStaticIntegerMutation(i: Int!): Int @resolver
        |}
        |
        |type Foo{
        |  s: String!
        |}
        |
        |input FooInput{
        | s: String!
        |}
        |
        |extend type Mutation {
        | extendedEchoStringMutation(s: String!): String @resolver
        |}
        |
        |#END_SCHEMA
        """.trimMargin()

    @Resolver
    class Mutation_ExampleMutationSelections : MutationResolvers.ExampleMutationSelections() {
        override suspend fun resolve(ctx: Context): Int? {
            val size = ctx.arguments.triangleSize
            return when (size) {
                1 -> 1
                else -> {
                    try {
                        val mutation = ctx.mutation(
                            ctx.selectionsFor(
                                Mutation.Reflection,
                                "exampleMutationSelections(triangleSize: \$x)",
                                mapOf("x" to (size - 1))
                            )
                        )
                        size + mutation.getExampleMutationSelections()!!
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw e
                    }
                }
            }
        }
    }

    @Resolver
    class Mutation_MultipleValueBooleanMutation : MutationResolvers.MultipleValueBooleanMutation() {
        override suspend fun resolve(ctx: Context): Boolean? {
            val userId = ctx.arguments.userId
            val name = ctx.arguments.name
            return !userId.isEmpty() && !name.isEmpty()
        }
    }

    @Resolver
    class Mutation_EchoObjMutation : MutationResolvers.EchoObjMutation() {
        override suspend fun resolve(ctx: Context): Foo? {
            val fooInput = ctx.arguments.foo
            return Foo.Builder(ctx).s(fooInput.s).build()
        }
    }

    @Resolver
    class Mutation_SimpleStaticIntegerMutation : MutationResolvers.SimpleStaticIntegerMutation() {
        override suspend fun resolve(ctx: Context): Int? {
            return 10
        }
    }

    @Resolver
    class Mutation_ExtendedEchoStringMutation : MutationResolvers.ExtendedEchoStringMutation() {
        override suspend fun resolve(ctx: Context): String? {
            return ctx.arguments.s
        }
    }

    @Test
    @Disabled
    fun `exampleMutationSelections recursively computes triangular number`() {
        execute(
            query = """
            mutation exampleMutationSelectionsTest {
                exampleMutationSelections(triangleSize: 4)
            }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "exampleMutationSelections" to 10 // 4 + 3 + 2 + 1
            }
        }
    }

    @Test
    @Disabled
    fun `exampleMutationSelections test base case`() {
        execute(
            query = """
            mutation exampleMutationSelectionsTest {
                exampleMutationSelections(triangleSize: 1)
            }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "exampleMutationSelections" to 1
            }
        }
    }

    @Test
    @Disabled
    fun `test multi value mutation`() {
        execute(
            query = """
        mutation testingMultiValueMutation {
            multipleValueBooleanMutation(userId: "user123", name: "testName")
        }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "multipleValueBooleanMutation" to true
            }
        }
    }

    @Test
    @Disabled
    fun `test echo mutation using same str`() {
        val rnd = UUID.randomUUID().toString()
        execute(
            query = """
        mutation testEcho(${'$'}input: FooInput!) {
            echoObjMutation(foo: ${'$'}input) {
                s
            }
        }
            """.trimIndent(),
            variables = mapOf(
                "input" to mapOf("s" to rnd)
            )
        ).assertEquals {
            "data" to {
                "echoObjMutation" to {
                    "s" to rnd
                }
            }
        }
    }

    @Test
    @Disabled
    fun `simple static int always returns 10`() {
        execute(
            query = """
        mutation {
            simpleStaticIntegerMutation(i: 123)
        }
            """.trimIndent()
        ).assertEquals {
            "data" to {
                "simpleStaticIntegerMutation" to 10
            }
        }
    }

    @Test
    @Disabled
    fun `extended echo string echoes back the input`() {
        val rnd = UUID.randomUUID().toString()
        execute(
            query = """
        mutation testExtendedEcho(${'$'}input: String!) {
            extendedEchoStringMutation(s: ${'$'}input)
        }
            """.trimIndent(),
            variables = mapOf("input" to rnd)
        ).assertEquals {
            "data" to {
                "extendedEchoStringMutation" to rnd
            }
        }
    }
}
