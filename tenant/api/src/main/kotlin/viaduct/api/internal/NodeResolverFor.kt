package viaduct.api.internal

/** Used to annotate generated node resolver base classes */
@Target(AnnotationTarget.CLASS)
annotation class NodeResolverFor(
    val typeName: String,
)
