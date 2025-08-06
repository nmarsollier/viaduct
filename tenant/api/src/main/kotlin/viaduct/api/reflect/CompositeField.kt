package viaduct.api.reflect

import viaduct.api.types.GRT

/**
 * A CompositeField describes static properties of a GraphQL field,
 * with an output type that is a [GRT].
 */
interface CompositeField<Parent : GRT, UnwrappedType : GRT> : Field<Parent> {
    /** the descriptor of the type of this field, with list wrappers removed */
    val type: Type<UnwrappedType>
}
