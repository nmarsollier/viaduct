package viaduct.api

import org.intellij.lang.annotations.Language

@Target(AnnotationTarget.CLASS)
annotation class Resolver(
    @Language("GraphQL") val objectValueFragment: String = "",
    @Language("GraphQL") val queryValueFragment: String = "",
    val variables: Array<Variable> = []
)

/**
 * @property name the name of the variable being defined
 * @property fromObjectField a path into a selection set of the field to which this annotation is applied.
 *
 *  For example, given a [Resolver] annotation with this `objectValueFragment`:
 *  ```graphql
 *  {
 *    foo(x: 2) {
 *       myBar: bar {
 *         y
 *       }
 *    }
 *  }
 *  ```
 *  Then a "foo.myBar.y" [fromObjectField] value will bind the value of the "y" selection to a variable
 *  named [name].
 *
 *  The value of [fromObjectField] is subject to these requirements:
 *
 *  1. the path must be a path that is selected in the selection set this [Variable] is bound to
 *  1. the path must terminate on a scalar or enum value, or a list that wraps one of these values
 *  1. the path may not traverse through list-valued fields
 *
 * @property fromArgument a path into a GraphQL argument of the field to which this annotation
 *  is applied.
 *
 *  Example:
 *  ```
 *  name = "x"
 *  fromArgument = "foo"
 *  ```
 *  Will bind the value from the "foo" argument of the dependent field to a variable named "x"
 *
 *  The value of [fromArgument] may be a dot-separated path, in which case the path will be
 *  traversed to select a value to bind to [name].
 *  This path may not traverse through lists, though it may terminate on a list or input
 *  object type.
 *  If any value in the traversal path is null, then the final extracted value will be null.
 *  If a supplied value for a traversal step is missing but a default value is defined for the
 *  argument or input field, then the default value will be used.
 *
 *  Example:
 *  ```
 *  name = "x"
 *  fromArgument = "foo.bar"
 *  ```
 *  Will traverse through the "foo" field of the dependent field, which must be an input type, and
 *  will bind the value of the "bar" input field to a variable named "x".
 *
 *  In all cases, the schema type of the argument indicated by this property must be coercible to
 *  the type in which this variable is used.
 */
annotation class Variable(
    val name: String,
    val fromObjectField: String = UNSET_STRING_VALUE,
    val fromQueryField: String = UNSET_STRING_VALUE,
    val fromArgument: String = UNSET_STRING_VALUE
) {
    companion object {
        const val UNSET_STRING_VALUE = "XY!#* N0T S3T!"
    }
}

@Target(AnnotationTarget.CLASS)
annotation class Variables(
    /**
     * A string describing the names and types of 1 or more variables.
     * Names and types are separated by `:`, and name-type pairs are joined by a colon.
     * All spaces are ignored.
     *
     * Example:
     * ```
     *   "foo: Int, bar: String!"
     * ```
     */
    val types: String
)
