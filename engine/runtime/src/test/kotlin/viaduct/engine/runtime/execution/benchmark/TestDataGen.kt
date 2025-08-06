package viaduct.engine.runtime.execution.benchmark

import com.fasterxml.jackson.databind.ObjectMapper
import graphql.schema.idl.SchemaPrinter
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import viaduct.arbitrary.common.CompoundingWeight
import viaduct.arbitrary.common.Config
import viaduct.arbitrary.common.ConfigKey
import viaduct.arbitrary.common.Unvalidated
import viaduct.arbitrary.graphql.DescriptionLength
import viaduct.arbitrary.graphql.FieldSelectionWeight
import viaduct.arbitrary.graphql.FragmentSpreadWeight
import viaduct.arbitrary.graphql.GenInterfaceStubsIfNeeded
import viaduct.arbitrary.graphql.InlineFragmentWeight
import viaduct.arbitrary.graphql.InputObjectTypeSize
import viaduct.arbitrary.graphql.ListValueSize
import viaduct.arbitrary.graphql.MaxSelectionSetDepth
import viaduct.arbitrary.graphql.OperationCount
import viaduct.arbitrary.graphql.asIntRange
import viaduct.arbitrary.graphql.filterNotNull
import viaduct.arbitrary.graphql.graphQLExecutionInput
import viaduct.arbitrary.graphql.graphQLSchema

data class TestData(val sdl: String, val query: String, val variables: Map<String, Any?> = emptyMap()) {
    companion object {
        /**
         * Example:
         * TestData.loadFromResources("many-fragments-1")
         */
        fun loadFromResources(
            simpleName: String,
            prefix: String = "/schemas_and_queries/"
        ): TestData =
            TestData(
                sdl = load("${prefix}$simpleName-schema.graphqls"),
                query = load("${prefix}$simpleName-query.graphql"),
                variables = maybeLoad("${prefix}$simpleName-variables.json")?.let { text ->
                    @Suppress("UNCHECKED_CAST")
                    ObjectMapper().readValue(text, Map::class.java) as Map<String, Any?>
                } ?: emptyMap()
            )

        fun load(fqPath: String): String = requireNotNull(maybeLoad(fqPath)) { "Resource not found: $fqPath" }

        fun maybeLoad(fqPath: String): String? =
            TestData::class.java.getResourceAsStream(fqPath)
                ?.bufferedReader()
                ?.use { it.readText() }
    }
}

object TestDataGen {
    @JvmStatic
    @OptIn(ExperimentalTime::class)
    fun main(args: Array<String>) {
        // Tune these values to shape the generated data
        val cfg = Config.default +
            (DescriptionLength to 0.asIntRange()) +
            (GenInterfaceStubsIfNeeded to true) +
            (OperationCount to 1.asIntRange()) +
            (ListValueSize to 1.asIntRange()) +
            (InputObjectTypeSize to 1.asIntRange()) +
            (MaxSelectionSetDepth to 20) +
            (FieldSelectionWeight to CompoundingWeight(.6, 10)) +
            (InlineFragmentWeight to CompoundingWeight(.6, 10)) +
            (FragmentSpreadWeight to CompoundingWeight(.6, 10)) +
            (MinQueryLength to 20_000) +
            (MinVariablesSize to 2)

        val rs = args.firstOrNull()
            ?.let { RandomSource.seeded(it.toLong()) }
            ?: RandomSource.default()

        val (testData, duration) = measureTimedValue {
            println("Generating...")
            genTestData(cfg, rs)
        }
        val files = mkFiles(rs)
        files.write(testData)

        println(
            """
            |Generation took ${duration.inWholeMilliseconds}ms
            |
            |Wrote generated files:
            |   sdl:       ${files.sdl.absolutePath}
            |   query:     ${files.query.absolutePath}
            |   variables: ${files.variables.absolutePath}
            |
            |Next steps:
            | - inspect these files to make sure they meet your needs, then
            | - rename them to something human-friendly
            | - move them to where you'd like to use them
            """.trimMargin()
        )
    }

    private fun mkFiles(rs: RandomSource): Files =
        Arb.string(8, Codepoint.alphanumeric())
            .map { baseName ->
                val sdl = mkFile("$baseName-schema.graphqls") ?: return@map null
                val query = mkFile("$baseName-query.graphql") ?: return@map null
                val variables = mkFile("$baseName-variables.json") ?: return@map null

                Files(sdl, query, variables)
            }
            .filterNotNull()
            .next(rs)

    private fun mkFile(path: String): File? = File(path).takeUnless { it.exists() }

    private data class Files(val sdl: File, val query: File, val variables: File) {
        fun write(data: TestData) {
            sdl.writeText(data.sdl)
            query.writeText(data.query)
            variables.writeText(ObjectMapper().writeValueAsString(data.variables))
        }
    }

    private fun genTestData(
        cfg: Config,
        rs: RandomSource
    ): TestData {
        val schema = Arb.graphQLSchema(cfg).next(rs)
        val inp = Arb.graphQLExecutionInput(schema, cfg)
            .filter {
                it.query.length >= cfg[MinQueryLength] && it.variables.size >= cfg[MinVariablesSize]
            }.next(rs)
        val sdl = SchemaPrinter().print(schema)
        return TestData(sdl, inp.query, inp.variables)
    }
}

object MinQueryLength : ConfigKey<Int>(80_000, Unvalidated)

object MinVariablesSize : ConfigKey<Int>(2, Unvalidated)
