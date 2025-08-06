package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.Test2ObjectValue
import viaduct.api.grts.TestNestedValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver(
    """
        fragment _ on Query {
            objectValue {
                strValue
            }
            objectValue2 {
                value
            }
        }
    """
)
class NestedFieldResolver : QueryResolvers.NestedField() {
    override suspend fun resolve(ctx: Context): TestNestedValue {
        return TestNestedValue.Builder(ctx)
            .value("string value")
            .nestedValue(ctx.objectValue.getObjectValue()?.getStrValue())
            .nestedObject(Test2ObjectValue.Builder(ctx).value(ctx.objectValue.getObjectValue2()?.getValue()).build())
            .build()
    }
}
