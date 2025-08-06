package viaduct.tenant.runtime.context

import graphql.schema.GraphQLObjectType
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import viaduct.api.globalid.GlobalID
import viaduct.api.internal.NodeReferenceFactory
import viaduct.api.internal.select.SelectionSetFactory
import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.mocks.MockGlobalIDCodec
import viaduct.api.mocks.MockInternalContext
import viaduct.api.select.SelectionSet
import viaduct.api.types.Query as QueryType
import viaduct.engine.api.ViaductSchema
import viaduct.engine.runtime.mocks.ContextMocks
import viaduct.engine.runtime.select.RawSelectionSetFactoryImpl
import viaduct.engine.runtime.select.RawSelectionSetImpl
import viaduct.service.api.spi.FlagManager
import viaduct.tenant.runtime.globalid.GlobalIDImpl
import viaduct.tenant.runtime.globalid.GlobalIdFeatureAppTest
import viaduct.tenant.runtime.globalid.Query
import viaduct.tenant.runtime.globalid.User
import viaduct.tenant.runtime.internal.NodeReferenceFactoryImpl
import viaduct.tenant.runtime.select.SelectionSetFactoryImpl
import viaduct.tenant.runtime.select.SelectionSetImpl

@ExperimentalCoroutinesApi
class NodeExecutionContextImplTest {
    private val userId = GlobalIDImpl(User.Reflection, "123")
    private val queryObject = mockk<Query>()
    private val rawSelectionSetFactory = RawSelectionSetFactoryImpl(ViaductSchema(GlobalIdFeatureAppTest.schema))
    private val engineExecutionContext = ContextMocks(
        myFullSchema = GlobalIdFeatureAppTest.schema,
        myFlagManager = FlagManager.Companion.DefaultFlagManager,
    ).engineExecutionContext

    private fun mk(
        userId: GlobalID<User> = this.userId,
        selectionSet: SelectionSet<User> = mockk<SelectionSet<User>>(),
        queryLoader: SelectionsLoader<QueryType> = SelectionsLoader.const(queryObject),
        selectionSetFactory: SelectionSetFactory = SelectionSetFactoryImpl(rawSelectionSetFactory),
        nodeReferenceFactory: NodeReferenceFactory = NodeReferenceFactoryImpl {
                id: String,
                graphQLObjectType: GraphQLObjectType,
            ->
            engineExecutionContext.createNodeEngineObjectData(id, graphQLObjectType)
        }
    ) = NodeExecutionContextImpl(
        ExecutionContextImpl(
            MockInternalContext(GlobalIdFeatureAppTest.schema, MockGlobalIDCodec()),
            queryLoader,
            selectionSetFactory,
            nodeReferenceFactory
        ),
        userId,
        selectionSet
    )

    @Test
    fun properties() {
        val ctx = mk()
        assertEquals(userId, ctx.id)
    }

    @Test
    fun selectionsFor() {
        val ctx = mk()
        val ss = ctx.selectionsFor(Query.Reflection, "__typename", mapOf("var" to true))
        assertTrue(ss.contains(Query.Reflection.Fields.__typename))
        val inner = (ss as SelectionSetImpl).rawSelectionSet as RawSelectionSetImpl
        assertEquals(mapOf("var" to true), inner.ctx.variables)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun query() =
        runBlockingTest {
            val ctx = mk()
            ctx.selectionsFor(Query.Reflection, "__typename").also {
                assertTrue(it.contains(Query.Reflection.Fields.__typename))

                ctx.query(it).also { result ->
                    assertEquals(queryObject, result)
                }
            }
        }

    @Test
    fun nodeFor() {
        val ctx = mk()
        ctx.nodeFor(userId)
    }
}
