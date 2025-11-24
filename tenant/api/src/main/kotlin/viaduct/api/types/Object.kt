package viaduct.api.types

import viaduct.utils.api.StableApi

/**
 * Tagging interface for object types
 */
@StableApi
interface Object : Struct, RecordOutput
