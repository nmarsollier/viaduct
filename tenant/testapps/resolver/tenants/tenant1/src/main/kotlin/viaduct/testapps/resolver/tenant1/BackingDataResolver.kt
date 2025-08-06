package viaduct.testapps.resolver.tenant1

import viaduct.api.Resolver
import viaduct.api.grts.TestBackingData
import viaduct.testapps.resolver.tenant1.resolverbases.QueryResolvers
import viaduct.testapps.resolver.tenant1.resolverbases.TestBackingDataResolvers

@Resolver
class BackingDataResolver : QueryResolvers.BackingData() {
    override suspend fun resolve(ctx: Context): TestBackingData {
        return TestBackingData.Builder(ctx).build()
    }
}

@Resolver(
    """
    fragment _ on TestBackingData {
        backingDataValue
    }
    """
)
class StrValueResolver : TestBackingDataResolvers.StrValue() {
    override suspend fun resolve(ctx: Context): String {
        val privateField = ctx.objectValue.get("backingDataValue", BackingDataValue::class) as BackingDataValue
        return privateField.strValue
    }
}

@Resolver
class BackingDataValueResolver : TestBackingDataResolvers.BackingDataValue() {
    override suspend fun resolve(ctx: Context): BackingDataValue {
        return BackingDataValue(
            strValue = "backing data value",
            intValue = 15
        )
    }
}
