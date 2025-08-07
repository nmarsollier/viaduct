package viaduct.engine.runtime.tenantloading

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import viaduct.engine.api.RequiredSelectionSet
import viaduct.engine.api.TenantModuleBootstrapper
import viaduct.engine.api.ViaductSchema
import viaduct.engine.api.mocks.MockCheckerExecutor
import viaduct.engine.api.mocks.MockCheckerExecutorFactory
import viaduct.engine.api.mocks.MockFieldUnbatchedResolverExecutor
import viaduct.engine.api.mocks.MockNodeBatchResolverExecutor
import viaduct.engine.api.mocks.MockSchema
import viaduct.engine.api.mocks.MockTenantAPIBootstrapper
import viaduct.engine.api.mocks.MockTenantModuleBootstrapper
import viaduct.engine.api.select.SelectionsParser
import viaduct.engine.runtime.validation.Validator

class DispatcherRegistryValidatorTest {
    private val moduleBootstrap = MockTenantModuleBootstrapper(
        fieldResolverExecutors = listOf(
            "Foo" to "field" to MockFieldUnbatchedResolverExecutor(
                RequiredSelectionSet(SelectionsParser.parse("Foo", "y"), emptyList()),
                resolverId = "Foo.field",
            )
        ),
        nodeResolverExecutors = listOf(
            "Foo" to MockNodeBatchResolverExecutor("Foo")
        ),
        schema = MockSchema.minimal,
    )

    private fun test(
        bootstrappers: List<TenantModuleBootstrapper> = listOf(moduleBootstrap),
        nodeResolverValidator: Validator<NodeResolverDispatcherValidationCtx> = Validator.Unvalidated,
        resolverExecutorValidator: Validator<ResolverExecutorValidationCtx> = Validator.Unvalidated,
        requiredSelectionSetValidator: Validator<RequiredSelectionsValidationCtx> = Validator.Unvalidated,
        checkerExecutorValidator: Validator<CheckerExecutorValidationCtx> = Validator.Unvalidated
    ) {
        val validator = DispatcherRegistryValidator(nodeResolverValidator, resolverExecutorValidator, requiredSelectionSetValidator, checkerExecutorValidator)
        DispatcherRegistryFactory(
            MockTenantAPIBootstrapper(bootstrappers),
            validator,
            MockCheckerExecutorFactory(
                mapOf(
                    "Foo" to "field" to MockCheckerExecutor()
                )
            )
        ).create(ViaductSchema(MockSchema.minimal))
    }

    @Test
    fun `passes on validator success`() {
        assertDoesNotThrow {
            test()
        }
    }

    @Test
    fun `fails on node resolver validator failure`() {
        assertThrows<IllegalArgumentException> {
            test(nodeResolverValidator = Validator.Invalid)
        }
    }

    @Test
    fun `fails on resolverExecutor validator failure`() {
        assertThrows<IllegalArgumentException> {
            test(resolverExecutorValidator = Validator.Invalid)
        }
    }

    @Test
    fun `fails on requiredSelectionSet validator failure`() {
        assertThrows<IllegalArgumentException> {
            test(requiredSelectionSetValidator = Validator.Invalid)
        }
    }

    @Test
    fun `fails on checkerExecutor validator validator failure`() {
        assertThrows<IllegalArgumentException> {
            test(checkerExecutorValidator = Validator.Invalid)
        }
    }
}
