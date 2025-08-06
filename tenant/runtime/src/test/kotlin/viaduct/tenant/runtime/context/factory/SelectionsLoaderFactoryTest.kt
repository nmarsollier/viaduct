package viaduct.tenant.runtime.context.factory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import viaduct.api.mocks.MockExecutionContext
import viaduct.api.select.SelectionSet

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionsLoaderFactoryTest {
    @Test
    fun forQuery() =
        runBlockingTest {
            MockArgs().let { args ->
                val loader = SelectionsLoaderFactory.forQuery.mk(args.getSelectionsLoaderArgs())
                assertDoesNotThrow {
                    loader.load(
                        MockExecutionContext(args.internalContext),
                        SelectionSet.empty(Query.Reflection)
                    )
                }
            }
        }

    @Test
    fun forMutation() =
        runBlockingTest {
            MockArgs().let { args ->
                val loader = SelectionsLoaderFactory.forMutation.mk(args.getSelectionsLoaderArgs())
                assertDoesNotThrow {
                    loader.load(
                        MockExecutionContext(args.internalContext),
                        SelectionSet.empty(Mutation.Reflection)
                    )
                }
            }
        }
}
