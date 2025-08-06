@file:Suppress("MatchingDeclarationName")

package viaduct.graphql.schema.graphqljava

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import viaduct.graphql.schema.ViaductSchema
import viaduct.invariants.InvariantChecker

private val `schema def plus alternatives` = """
    schema {
       query: Foo
       mutation: Bar
       subscription: Baz
    }
    type Foo { blank: String }
    type Bar { blank: String }
    type Baz { blank: String }
    type AltFoo { blank: String }
    type AltBar { blank: String }
    type AltBaz { blank: String }
    interface BadFoo { blank: String }
    interface BadBar { blank: String }
    interface BadBaz { blank: String }
""".trimIndent()

/** A set of unit tests for the factory methods of
 *  GJSchemaRaw and FilteredSchema to make sure they
 *  correctly set the root type defs.
 */
interface RootTypeFactoryContractForBoth {
    fun makeSchema(
        schema: String,
        queryTypeName: String? = null,
        mutationTypeName: String? = null,
        subscriptionTypeName: String? = null
    ): ViaductSchema

    @Test
    fun `schema def works`() {
        makeSchema(`schema def plus alternatives`)
            .apply {
                assertSame(this.types["Foo"], this.queryTypeDef)
                assertSame(this.types["Bar"], this.mutationTypeDef)
                assertSame(this.types["Baz"], this.subscriptionTypeDef)
            }
    }

    @Test
    fun `parameters override schema def`() {
        makeSchema(`schema def plus alternatives`, "AltFoo", "AltBar", "AltBaz")
            .apply {
                assertSame(this.types["AltFoo"], this.queryTypeDef)
                assertSame(this.types["AltBar"], this.mutationTypeDef)
                assertSame(this.types["AltBaz"], this.subscriptionTypeDef)
            }
    }

    @Test
    fun `non object type from parameters fail`() {
        val check = InvariantChecker()
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def plus alternatives`, queryTypeName = "BadFoo")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def plus alternatives`, mutationTypeName = "BadBar")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def plus alternatives`, subscriptionTypeName = "BadBaz")
        }
        check.assertEmpty("\n")
    }
}

private val `defaults plus alternatives` = """
    type Query { blank: String }
    type Mutation { blank: String }
    type Subscription { blank: String }
    type Foo { blank: String }
    type Bar { blank: String }
    type Baz { blank: String }
""".trimIndent()

private val `schema plus defaults plus alternatives` = """
    schema { query: Foo }
    type Query { blank: String }
    type Mutation { blank: String }
    type Subscription { blank: String }
    type Foo { blank: String }
    type Bar { blank: String }
    type Baz { blank: String }
""".trimIndent()

private val `schema def with missing names` = """
    schema {
      query: Foo
      mutation: Bar
      subscription: Baz
    }
    type AltFoo { blank: String }
    type AltBar { blank: String }
    type AltBaz { blank: String }
""".trimIndent()

private val `schema def with non object types` = """
    schema {
      query: Foo
      mutation: Bar
      subscription: Baz
    }
    interface Foo { blank: String }
    interface Bar { blank: String }
    interface Baz { blank: String }
    type AltFoo { blank: String }
    type AltBar { blank: String }
    type AltBaz { blank: String }
""".trimIndent()

/** GJSchemaRaw has more test cases. */
interface RootTypeFactoryContractForRaw : RootTypeFactoryContractForBoth {
    @Test
    fun `defaults work`() {
        makeSchema(`defaults plus alternatives`)
            .apply {
                assertSame(this.types["Query"], this.queryTypeDef)
                assertSame(this.types["Mutation"], this.mutationTypeDef)
                assertSame(this.types["Subscription"], this.subscriptionTypeDef)
            }
    }

    @Test
    fun `parameters override defaults`() {
        makeSchema(`defaults plus alternatives`, "Foo", "Bar", "Baz")
            .apply {
                assertSame(this.types["Foo"], this.queryTypeDef)
                assertSame(this.types["Bar"], this.mutationTypeDef)
                assertSame(this.types["Baz"], this.subscriptionTypeDef)
            }
    }

    @Test
    fun `no default parameters work`() {
        makeSchema(
            `defaults plus alternatives`,
            GJSchemaRaw.NO_ROOT_TYPE_DEFAULT,
            GJSchemaRaw.NO_ROOT_TYPE_DEFAULT,
            GJSchemaRaw.NO_ROOT_TYPE_DEFAULT
        ).apply {
            assertNull(this.queryTypeDef)
            assertNull(this.mutationTypeDef)
            assertNull(this.subscriptionTypeDef)
        }
    }

    @Test
    fun `no default parameters with schema def works`() {
        makeSchema(
            `schema plus defaults plus alternatives`,
            GJSchemaRaw.NO_ROOT_TYPE_DEFAULT,
            GJSchemaRaw.NO_ROOT_TYPE_DEFAULT,
            GJSchemaRaw.NO_ROOT_TYPE_DEFAULT
        ).apply {
            assertSame(this.types["Foo"], this.queryTypeDef)
            assertNull(this.mutationTypeDef)
            assertNull(this.subscriptionTypeDef)
        }
    }

    @Test
    fun `missing names from schema def fail`() {
        val check = InvariantChecker()
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with missing names`, mutationTypeName = "AltBar", subscriptionTypeName = "AltBaz")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with missing names`, queryTypeName = "AltFoo", subscriptionTypeName = "AltBaz")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with missing names`, queryTypeName = "AltFoo", mutationTypeName = "AltBar")
        }
        check.assertEmpty("\n")
    }

    @Test
    fun `missing names from parameters fail`() {
        val check = InvariantChecker()
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def plus alternatives`, queryTypeName = "NotDefined")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def plus alternatives`, mutationTypeName = "NotDefined")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def plus alternatives`, subscriptionTypeName = "NotDefined")
        }
        check.assertEmpty("\n")
    }

    @Test
    fun `non object type from schema def fail`() {
        val check = InvariantChecker()
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with non object types`, mutationTypeName = "AltBar", subscriptionTypeName = "AltBaz")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with non object types`, queryTypeName = "AltFoo", subscriptionTypeName = "AltBaz")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with non object types`, queryTypeName = "AltFoo", mutationTypeName = "AltBar")
        }
        check.assertEmpty("\n")
    }

    @Test
    fun `non object type from defaults fail`() {
        val check = InvariantChecker()
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with non object types`, mutationTypeName = "AltBar", subscriptionTypeName = "AltBaz")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with non object types`, queryTypeName = "AltFoo", subscriptionTypeName = "AltBaz")
        }
        check.doesThrow<IllegalArgumentException>("") {
            makeSchema(`schema def with non object types`, queryTypeName = "AltFoo", mutationTypeName = "AltBar")
        }
        check.assertEmpty("\n")
    }
}
