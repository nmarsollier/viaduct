package viaduct.utils.collections

import viaduct.utils.collections.BitVector.Builder

open class BitVectorSetup {
    internal fun mk(size: Int): BitVector {
        return BitVector(size)
    }

    internal fun mkb(): Builder {
        return Builder()
    }

    companion object {
        /** Assumed word-size for size-based corner cases.  */
        internal const val WS: Int = 64
    }
}
