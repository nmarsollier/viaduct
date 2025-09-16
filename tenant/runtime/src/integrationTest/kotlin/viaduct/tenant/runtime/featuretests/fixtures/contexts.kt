package viaduct.tenant.runtime.featuretests.fixtures

import viaduct.api.context.FieldExecutionContext
import viaduct.api.context.MutationFieldExecutionContext
import viaduct.api.context.NodeExecutionContext
import viaduct.api.types.NodeObject

/** An alias for [FieldExecutionContext] that allows a resolver to read non-GRT data and write either GRT or non-GRT data */
typealias UntypedFieldContext = FieldExecutionContext<ObjectStub, QueryStub, ArgumentsStub, CompositeStub>

/** An alias for [MutationFieldExecution] that allows a mutation resolver to read non-GRT data and write either GRT or non-GRT data */
typealias UntypedMutationFieldContext = MutationFieldExecutionContext<ObjectStub, QueryStub, ArgumentsStub, CompositeStub>

/** An alias for [NodeExecutionContext] that allows a node resolver to read non-GRT data and write either GRT or non-GRT data */
typealias UntypedNodeContext = NodeExecutionContext<NodeObject>
