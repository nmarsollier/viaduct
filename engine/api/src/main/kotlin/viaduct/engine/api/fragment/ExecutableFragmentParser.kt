package viaduct.engine.api.fragment

import graphql.language.Document

interface ExecutableFragmentParser {
    fun parse(fragment: Fragment): Document
}
