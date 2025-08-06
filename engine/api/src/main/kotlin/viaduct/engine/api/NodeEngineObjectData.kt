package viaduct.engine.api

/** Backing data for a reference to a Node value */
interface NodeEngineObjectData : LazyEngineObjectData {
    /** a serialized representation of a Node's GlobalID */
    val id: String
}
