package viaduct.api.internal

/** Used to annotate generated resolver base classes */
@Target(AnnotationTarget.CLASS)
annotation class ResolverFor(
    val typeName: String,
    val fieldName: String,
)
