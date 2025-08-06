@file:OptIn(ExperimentalCoroutinesApi::class)

package viaduct.arbitrary.graphql

import graphql.Scalars
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.kotest.property.Arb
import io.kotest.property.forAll
import io.kotest.property.forNone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import viaduct.arbitrary.common.Config
import viaduct.arbitrary.common.KotestPropertyBase

class GraphQLStringsTest : KotestPropertyBase() {
    @Test
    fun `Arb_graphQLName produces spec-compliant names`() =
        runBlockingTest {
            val patt = Regex("^[a-z,A-Z,_][a-z,A-Z,_,0-9,]*")
            Arb.graphQLName().forAll {
                it.matches(patt)
            }
        }

    @Test
    fun `Arb_graphQLName produces names that are valid GraphQL type names`() =
        runBlockingTest {
            val placeholder =
                GraphQLFieldDefinition.newFieldDefinition()
                    .name("placeholder")
                    .type(Scalars.GraphQLInt)
                    .build()

            Arb.graphQLName().forAll { typeName ->
                val query =
                    GraphQLObjectType.newObject()
                        .name(typeName)
                        .field(placeholder)
                        .build()
                val result =
                    Result.runCatching {
                        GraphQLSchema.newSchema().query(query).build()
                    }

                result.isSuccess
            }
        }

    @Test
    fun `Arb_graphQLName does not produce introspection names`() =
        runBlockingTest {
            Arb.graphQLName(1..4).forNone { it.startsWith("__") }
        }

    @Test
    fun `Arb_graphQLFieldName produces names that are valid GraphQL field names`() =
        runBlockingTest {
            val query =
                GraphQLObjectType.newObject()
                    .name("Query")
                    .build()

            Arb.graphQLFieldName().forAll { fieldName ->
                val newQuery =
                    query.transform {
                        val field =
                            GraphQLFieldDefinition.newFieldDefinition()
                                .name(fieldName)
                                .type(Scalars.GraphQLInt)
                                .build()
                        it.clearFields().field(field)
                    }

                val result =
                    Result.runCatching {
                        GraphQLSchema.newSchema().query(newQuery).build()
                    }

                result.isSuccess
            }
        }

    @Test
    fun `Arb_graphQLFieldName does not produce introspection names`() =
        runBlockingTest {
            Arb.graphQLFieldName().forNone { it.startsWith("__") }
        }

    @Test
    fun `BanFieldNames`() =
        runBlockingTest {
            // create a ban list of single-letter field names, a-m and A-M
            val banned = CharRange('a', 'm').fold(emptySet<String>()) { acc, ch ->
                acc + ch.toString() + ch.uppercase()
            }

            // create a set of single-char names
            val cfg = Config.default +
                (BanFieldNames to banned) +
                (FieldNameLength to 1..2) +
                (SchemaSize to 1)

            val arbs = listOf(
                Arb.graphQLFieldName(cfg),
                Arb.graphQLEnumValueName(cfg),
                Arb.graphQLArgumentName(cfg)
            )

            arbs.forEach { arb ->
                arb.forNone { banned.contains(it) }
            }
        }
}
