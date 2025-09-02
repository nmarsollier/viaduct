package viaduct.api.types

/**
 * Tagging interface for types that implement the Node interface.
 * This includes both Node interfaces and Node object implementations.
 */
interface NodeCompositeOutput : CompositeOutput

/**
 * Tagging interface for object types that implement the Node interface
 */
interface NodeObject : Object, NodeCompositeOutput
