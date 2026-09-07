package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable
import java.io.Closeable

@NonExtendable
sealed interface UGpuBuffer : Closeable {
    val size: Long

    val isClosed: Boolean

    fun slice(offset: Long = 0, size: Long = this.size - offset) =
        UGpuBufferSlice(this, offset, size)

    data class Usage(val bits: Int) {
        operator fun contains(other: Usage): Boolean =
            (bits and other.bits) == other.bits

        operator fun plus(other: Usage): Usage =
            Usage(bits or other.bits)

        companion object {
            val MAP_READ = Usage(1 shl 0)
            val MAP_WRITE = Usage(1 shl 1)
            val HINT_CLIENT_STORAGE = Usage(1 shl 2)
            val COPY_DST = Usage(1 shl 3)
            val COPY_SRC = Usage(1 shl 4)
            val VERTEX = Usage(1 shl 5)
            val INDEX = Usage(1 shl 6)
            val UNIFORM = Usage(1 shl 7)
            val UNIFORM_TEXEL_BUFFER = Usage(1 shl 8)
            val INDIRECT_PARAMETERS = Usage(1 shl 9)
        }
    }
}

data class UGpuBufferSlice(val buffer: UGpuBuffer, val offset: Long, val size: Long) {
    init {
        require(offset >= 0) { "offset must not be negative" }
        require(size >= 0) { "size must not be negative" }
        require(offset + size <= buffer.size) { "slice (starting at $offset with size $size) points past end of buffer (with size ${buffer.size})" }
    }

    fun slice(offset: Long = 0, size: Long = this.size - offset) =
        UGpuBufferSlice(this.buffer, this.offset + offset, size)
}
