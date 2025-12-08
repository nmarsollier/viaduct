package viaduct.api.types

/**
 * Tagging interface for virtual input types that wrap field arguments
 */
interface Arguments : InputLike {
    /** A marker object indicating the lack of schematic arguments */
    object NoArguments : Arguments
}
