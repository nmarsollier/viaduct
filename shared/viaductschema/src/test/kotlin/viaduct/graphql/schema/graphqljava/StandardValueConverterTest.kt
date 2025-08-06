package viaduct.graphql.schema.graphqljava

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import viaduct.graphql.schema.ViaductSchema
import viaduct.graphql.schema.graphqljava.test.ValueConverterContract

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StandardValueConverterTest : ValueConverterContract() {
    @BeforeAll
    fun setup() {
        valueConverter = ValueConverter.standard

        viaductExtendedSchema = GJSchema.fromRegistry(
            readTypes(schema),
            valueConverter = valueConverter
        )

        val A = (viaductExtendedSchema.types["EnumA"]!! as ViaductSchema.Enum).value("A")!!

        defaultValues = mapOf(
            "SubjectConstants.boolean" to true,
            "SubjectConstants.float" to 3.14159,
            "SubjectConstants.int" to 2,
            "SubjectConstants.long" to 3L,
            "SubjectConstants.short" to 4.toShort(),
            "SubjectConstants.string" to "hello",
            "SubjectConstants.rstring" to "world",
            "SubjectConstants.longList" to listOf(listOf(1L, 2L, 3L)),
            "SubjectConstants.in" to
                mapOf(
                    "f1" to 1L,
                    "f2" to listOf(2.toShort(), 3.toShort()),
                    "__typename" to "Input"
                ),
            "SubjectConstantsArgs.field.enumA" to A,
            "SubjectConstantsArgs.field.boolean" to true,
            "SubjectConstantsArgs.field.float" to 1.0,
            "SubjectConstantsArgs.field.int" to 2,
            "SubjectConstantsArgs.field.long" to 3L,
            "SubjectConstantsArgs.field.short" to 4.toShort(),
            "SubjectConstantsArgs.field.string" to "hello",
            "SubjectConstantsArgs.field.rstring" to "world",
            "SubjectConstantsArgs.field.longList" to listOf(listOf(1L, 2L, 3L)),
            "SubjectConstantsArgs.field.in" to
                mapOf(
                    "f1" to 1L,
                    "f2" to listOf(2.toShort(), 3.toShort()),
                    "__typename" to "Input"
                ),
            "SubjectConstants.enumA" to A,
        )

        defaultsToEffectiveDefaults()
    }
}
