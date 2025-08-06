package viaduct.tenant.runtime.context.factory

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import viaduct.api.context.FieldExecutionContext
import viaduct.api.context.MutationFieldExecutionContext
import viaduct.api.internal.select.SelectionsLoader
import viaduct.api.types.Mutation
import viaduct.tenant.runtime.context.FieldExecutionContextImpl
import viaduct.tenant.runtime.context.MutationFieldExecutionContextImpl

fun interface MutationFieldExecutionContextFactory : FieldExecutionContextFactory {
    override fun make(args: FieldArgs): MutationFieldExecutionContext<*, *, *, *>
}

object MutationFieldExecutionContextMetaFactory {
    /**
     * Create a [Factory] that will always wrap the output of [fieldContext]
     * in a MutationExecutionContext.
     */
    fun create(
        fieldContext: FieldExecutionContextFactory,
        mutationSelectionsLoader: Factory<SelectionsLoaderArgs, SelectionsLoader<Mutation>> = SelectionsLoaderFactory.forMutation,
    ): MutationFieldExecutionContextFactory =
        MutationFieldExecutionContextFactory { args ->
            MutationFieldExecutionContextImpl(
                fieldContext.make(args) as FieldExecutionContextImpl,
                mutationSelectionsLoader.mk(
                    SelectionsLoaderArgs(
                        resolverId = args.resolverId,
                        selectionsLoaderFactory = args.selectionsLoaderFactory,
                    )
                )
            )
        }

    /**
     * Create a [Factory] that will wrap the result of [fieldContext]
     * in a MutationExecutionContext, if [contextCls] supports mutations.
     * Otherwise, the created Factory will return the unmodified output of [fieldContext].
     */
    fun <Ctx : FieldExecutionContext<*, *, *, *>> ifMutation(
        contextCls: KClass<Ctx>,
        fieldContext: FieldExecutionContextFactory,
        mutationSelectionsLoader: Factory<SelectionsLoaderArgs, SelectionsLoader<Mutation>> = SelectionsLoaderFactory.forMutation,
    ): FieldExecutionContextFactory =
        if (contextCls.isSubclassOf(MutationFieldExecutionContext::class)) {
            create(fieldContext, mutationSelectionsLoader)
        } else {
            fieldContext
        }
}
