package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestEnum
import viaduct.api.grts.TestObjectValue
import viaduct.api.grts.TestResolvedFieldErrorValue
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers

@Resolver(
    """
        fragment _ on Query {
            objectValue {
                strValue
            }
        }
    """
)
class NestedFieldErrorResolver : QueryResolvers.NestedFieldError() {
    override suspend fun resolve(ctx: Context): TestResolvedFieldErrorValue {
        return TestResolvedFieldErrorValue.Builder(ctx)
            .nestedValue(
                TestObjectValue.Builder(ctx)
                    .strValue(ctx.objectValue.getObjectValue()?.getStrValue() ?: "default")
                    .enumValue(ctx.objectValue.getObjectValue()?.getEnumValue() ?: TestEnum.VALUE1)
                    .build()
            ).build()
    }
}
