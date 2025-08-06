package viaduct.tenant.runtime.globalid

import com.google.common.escape.Escaper
import com.google.common.net.UrlEscapers
import java.net.URLDecoder
import java.util.Base64
import kotlin.reflect.full.isSubclassOf
import viaduct.api.globalid.GlobalID
import viaduct.api.globalid.GlobalIDCodec
import viaduct.api.internal.ReflectionLoader
import viaduct.api.reflect.Type
import viaduct.api.types.CompositeOutput
import viaduct.api.types.Object

class GlobalIDCodecImpl(private val mirror: ReflectionLoader) : GlobalIDCodec {
    private val enc = Base64.getEncoder()
    private val dec = Base64.getDecoder()

    override fun <T : CompositeOutput> serialize(id: GlobalID<T>): String =
        enc.encodeToString(
            arrayListOf(
                id.type.name,
                escaper.escape(id.internalID)
            ).joinToString(delim).toByteArray()
        )

    override fun <T : CompositeOutput> deserialize(str: String): GlobalID<T> =
        dec.decode(str).decodeToString().let { decodedStr ->
            val parts = decodedStr.split(delim)
            require(parts.size == 2) {
                "Expected GlobalID \"$str\" to be a Base64-encoded string with the decoded format '<type name>$delim<internal ID>', " +
                    "got decoded value $decodedStr"
            }

            val (name, id) = parts
            val localId = URLDecoder.decode(id, "UTF-8")

            val type = mirror.reflectionFor(name).let {
                require(it.kcls.isSubclassOf(Object::class)) {
                    "type `$name` is not an Object"
                }
                @Suppress("UNCHECKED_CAST")
                it as Type<T>
            }

            GlobalIDImpl(type, localId)
        }

    companion object {
        private const val delim = ":"
        private val escaper: Escaper by lazy { UrlEscapers.urlFormParameterEscaper() }
    }
}
