package viaduct.tenant.codegen.bytecode

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmType
import kotlinx.metadata.KmVariance
import kotlinx.metadata.isNullable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import viaduct.codegen.km.kmListOfType
import viaduct.codegen.utils.Km
import viaduct.codegen.utils.KmName
import viaduct.codegen.utils.name
import viaduct.graphql.schema.ViaductExtendedSchema
import viaduct.graphql.schema.test.mkSchema
import viaduct.tenant.codegen.bytecode.config.baseTypeKmType
import viaduct.tenant.codegen.bytecode.config.cfg
import viaduct.tenant.codegen.bytecode.config.kmType

class KmUtilsTest {
    val pkg = KmName("testing")

    @BeforeEach
    fun setUp() {
        cfg.isModern = false
    }

    @Test
    fun testListKmType() {
        val schema = """
            input ListTest {
              field1: [String]
              field2: [String!]
              field3: [String]!
              field4: [String!]!
              field5: [[String!]]
              field6: [[String]!]
              field7: [[String]!]!
            }
        """.trimIndent()
        val input = mkSchema(schema).types["ListTest"] as ViaductExtendedSchema.Input

        val nullNull = kmListOfType(Km.STRING.asNullableType(), true)
        assertKmTypeEquals(nullNull, input.field("field1")!!.kmType(pkg))

        val nullNonNull = kmListOfType(Km.STRING.asType(), true)
        assertKmTypeEquals(nullNonNull, input.field("field2")!!.kmType(pkg))

        val nonNullNull = kmListOfType(Km.STRING.asNullableType())
        assertKmTypeEquals(nonNullNull, input.field("field3")!!.kmType(pkg))

        val nonNullNonNull = kmListOfType(Km.STRING.asType())
        assertKmTypeEquals(nonNullNonNull, input.field("field4")!!.kmType(pkg))

        assertKmTypeEquals(
            kmListOfType(nullNonNull, true),
            input.field("field5")!!.kmType(pkg)
        )

        assertKmTypeEquals(
            kmListOfType(nonNullNull, true),
            input.field("field6")!!.kmType(pkg)
        )

        assertKmTypeEquals(
            kmListOfType(nonNullNull),
            input.field("field7")!!.kmType(pkg)
        )
    }

    @Test
    fun testListInputKmTypeVariance() {
        val schema = """
            input ListVarianceTest {
              scalarList: [String]
              jsonList: [JSON]
              enumList: [E]
              interfaceList: [I]
              objectList: [T]
              unionList: [U]
              nestedScalarList: [[String]]
              nestedObjectList: [[V]]
            }

            scalar JSON

            enum E {
              A
            }

            interface I {
              field: String
            }

            type T implements I {
              field: String
            }

            union U = V
            type V {
              intField: Int
            }
        """.trimIndent()
        val input = mkSchema(schema).types["ListVarianceTest"] as ViaductExtendedSchema.Input

        val scalarList = input.field("scalarList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.INVARIANT, scalarList.arguments[0].variance)

        val jsonList = input.field("jsonList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, jsonList.arguments[0].variance)

        val enumList = input.field("enumList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, enumList.arguments[0].variance)

        val interfaceList = input.field("interfaceList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, interfaceList.arguments[0].variance)

        val objectList = input.field("objectList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, objectList.arguments[0].variance)

        val unionList = input.field("unionList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, unionList.arguments[0].variance)

        val nestedScalarList = input.field("nestedScalarList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, nestedScalarList.arguments[0].variance)
        assertEquals(KmVariance.INVARIANT, nestedScalarList.arguments[0].type!!.arguments[0].variance)

        val nestedObjectList = input.field("nestedObjectList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, nestedObjectList.arguments[0].variance)
        assertEquals(KmVariance.OUT, nestedObjectList.arguments[0].type!!.arguments[0].variance)
    }

    @Test
    fun testListInputKmTypeV2Variance() {
        cfg.isModern = true
        val schema = """
            input ListVarianceTest {
              scalarList: [String]
              jsonList: [JSON]
              enumList: [E]
              interfaceList: [I]
              objectList: [T]
              unionList: [U]
              nestedScalarList: [[String]]
              nestedObjectList: [[V]]
            }

            scalar JSON

            enum E {
              A
            }

            interface I {
              field: String
            }

            type T implements I {
              field: String
            }

            union U = V
            type V {
              intField: Int
            }
        """.trimIndent()
        val input = mkSchema(schema).types["ListVarianceTest"] as ViaductExtendedSchema.Input

        val scalarList = input.field("scalarList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.INVARIANT, scalarList.arguments[0].variance)

        val jsonList = input.field("jsonList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, jsonList.arguments[0].variance)

        val enumList = input.field("enumList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, enumList.arguments[0].variance)

        val interfaceList = input.field("interfaceList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, interfaceList.arguments[0].variance)

        val objectList = input.field("objectList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.INVARIANT, objectList.arguments[0].variance)

        val unionList = input.field("unionList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, unionList.arguments[0].variance)

        val nestedScalarList = input.field("nestedScalarList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, nestedScalarList.arguments[0].variance)
        assertEquals(KmVariance.INVARIANT, nestedScalarList.arguments[0].type!!.arguments[0].variance)

        val nestedObjectList = input.field("nestedObjectList")!!.kmType(pkg, isInput = true)
        assertEquals(KmVariance.OUT, nestedObjectList.arguments[0].variance)
        assertEquals(KmVariance.INVARIANT, nestedObjectList.arguments[0].type!!.arguments[0].variance)
    }

    @Test
    fun testPagedConnectionKmType() {
        val schema = """
            interface PagedConnection {
                # don't need pageInfo in tests
                edges: [ConnectionEdge]
            }
            interface ConnectionEdge {
                cursor: String!
            }
            type ActualConnection implements PagedConnection {
                edges: [ActualEdge]
            }
            type ActualEdge implements ConnectionEdge {
                cursor: String!
                node: ActualNode
            }
            type ActualNode {
                id: ID!
            }
        """.trimIndent()
        val typeDefs = mkSchema(schema)

        val gqlActualConnection = typeDefs.types["ActualConnection"]!!.asTypeExpr()
        val subject = gqlActualConnection.baseTypeKmType(pkg, field = null)
        assertKmTypeEquals(
            cfg.EDGES_QUERY_RESPONSE.asKmName.asNullableType(),
            subject
        )
        assertEquals(1, subject.arguments.size)
        assertEquals(KmVariance.OUT, subject.arguments[0].variance)
        assertEquals("testing/ActualNode", subject.arguments[0].type!!.name.toString())
    }

    private fun assertKmTypeEquals(
        expected: KmType?,
        actual: KmType?
    ) {
        if (expected == null) {
            assertNull(actual)
            return
        }
        assertEquals(expected.isNullable, actual!!.isNullable)
        assertEquals(
            (expected.classifier as KmClassifier.Class).name,
            (actual.classifier as KmClassifier.Class).name
        )
        for (i in 0..expected.arguments.size - 1) {
            val expectedArgument = expected.arguments[i]
            val actualArgument = actual.arguments[i]
            assertEquals(expectedArgument.variance, actualArgument.variance)
            assertKmTypeEquals(expectedArgument.type, actualArgument.type)
        }
    }

    @Test
    fun testConnectionKmTypeV2() {
        cfg.isModern = true
        val schema = """
            interface PagedConnection {
                # don't need pageInfo in tests
                edges: [ConnectionEdge]
            }
            interface ConnectionEdge {
                cursor: String!
            }
            type ActualConnection implements PagedConnection {
                edges: [ActualEdge]
            }
            type ActualEdge implements ConnectionEdge {
                cursor: String!
                node: ActualNode
            }
            type ActualNode {
                id: ID!
            }
        """.trimIndent()
        val typeDefs = mkSchema(schema)

        val gqlActualConnection = typeDefs.types["ActualConnection"]!!.asTypeExpr()
        val subject = gqlActualConnection.baseTypeKmType(pkg, field = null)
        assertEquals("testing/ActualConnection", subject.name.toString())
        assertEquals(0, subject.arguments.size)
    }

    @Test
    fun testValueInCtSyntax() {
        val schema = """
            scalar Date
            scalar DateTime
            input F { f: Float! }
            enum E { A }
            input ListTest {
              b: Boolean
              d: Date
              dt: DateTime
              f: Float
              id: ID
              i: Int
              l: Long
              r: Short
              s: String
              ri: Int!
              rli: [Int]!
              lri: [Int!]
              rlls: [[String]]!
              n: F
              e: E = A
            }
        """.trimIndent()
        val input = mkSchema(schema).types["ListTest"] as ViaductExtendedSchema.Input
        assertEquals(
            "java.lang.Boolean.valueOf(true)",
            input.field("b")!!.type.valueInCtSyntax(true, pkg)
        )
        assertEquals(
            "java.time.LocalDate.parse(\"2000-01-31\")",
            input.field("d")!!.type.valueInCtSyntax(java.time.LocalDate.of(2000, 1, 31), pkg)
        )
        val inst = java.time.LocalDateTime.of(2000, 1, 31, 1, 0, 0).toInstant(java.time.ZoneOffset.of("Z"))
        assertEquals(
            "java.time.Instant.parse(\"2000-01-31T01:00:00Z\")",
            input.field("dt")!!.type.valueInCtSyntax(inst, pkg)
        )
        assertEquals(
            "java.lang.Double.valueOf(1.0)",
            input.field("f")!!.type.valueInCtSyntax(1.0f, pkg)
        )
        assertEquals(
            "\"LISTING:10\"",
            input.field("id")!!.type.valueInCtSyntax("LISTING:10", pkg)
        )
        assertEquals(
            "java.lang.Integer.valueOf(1)",
            input.field("i")!!.type.valueInCtSyntax(1, pkg)
        )
        assertEquals(
            "java.lang.Long.valueOf(1L)",
            input.field("l")!!.type.valueInCtSyntax(1L, pkg)
        )
        assertEquals(
            "java.lang.Short.valueOf((short)1)",
            input.field("r")!!.type.valueInCtSyntax(1.toShort(), pkg)
        )
        assertEquals(
            "\"Hi\"",
            input.field("s")!!.type.valueInCtSyntax("Hi", pkg)
        )
        assertEquals(
            "1",
            input.field("ri")!!.type.valueInCtSyntax(1, pkg)
        )
        assertEquals(
            "Arrays.asList(new Object[0])",
            input.field("rli")!!.type.valueInCtSyntax(listOf<Int>(), pkg)
        )
        assertEquals(
            "Arrays.asList(new Object[] { java.lang.Integer.valueOf(1) })",
            input.field("rli")!!.type.valueInCtSyntax(listOf(1), pkg)
        )
        assertEquals(
            "Arrays.asList(new Object[] { 1 })",
            input.field("lri")!!.type.valueInCtSyntax(listOf(1), pkg)
        )
        assertEquals(
            "Arrays.asList(new Object[] { Arrays.asList(new Object[] { \"Hi\", \"There\", null }) })",
            input.field("rlls")!!.type.valueInCtSyntax(listOf(listOf("Hi", "There", null)), pkg)
        )
        assertEquals(
            "new testing.F(2.7182818284)",
            input.field("n")!!.type.valueInCtSyntax(mapOf<String, Double>("f" to 2.7182818284), pkg)
        )
        val bt = input.field("e")!!.type.baseTypeDef as ViaductExtendedSchema.Enum
        val v = bt.values.find { it.name == "A" }!!
        assertEquals(
            "\"A\"",
            input.field("e")!!.type.valueInCtSyntax(v, pkg)
        )
    }
}
