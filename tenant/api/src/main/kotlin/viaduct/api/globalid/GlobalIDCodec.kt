package viaduct.api.globalid

import viaduct.api.types.CompositeOutput

/**
 * GlobalIDCodec provides a way to serialize and deserialize GlobalIDs.
 */
interface GlobalIDCodec {
    /**
     * Serialize a GlobalID to a string.
     * @param id The GlobalID to serialize.
     * @return The serialized GlobalID.
     */
    fun <T : CompositeOutput> serialize(id: GlobalID<T>): String

    /**
     * Deserialize a GlobalID from a string.
     * @param str The string to deserialize.
     * @return The deserialized GlobalID.
     */
    fun <T : CompositeOutput> deserialize(str: String): GlobalID<T>
}
