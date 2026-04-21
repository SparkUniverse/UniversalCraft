package gg.essential.universal.render

import gg.essential.universal.UGraphics
import org.jetbrains.annotations.ApiStatus.NonExtendable
import java.io.Closeable

@NonExtendable
sealed interface UGpuFormat {
    companion object {
        val DEFAULT_RGBA: UGpuFormat =
            UGraphics.getPlatformAdapter().defaultGpuFormatRgba
        val DEFAULT_DEPTH: UGpuFormat =
            UGraphics.getPlatformAdapter().defaultGpuFormatDepth
    }
}

@NonExtendable
sealed interface UGpuTexture : Closeable {
    val width: Int
    val height: Int
    val mipLevels: Int

    val isClosed: Boolean

    data class Usage(val bits: Int) {
        operator fun contains(other: Usage): Boolean =
            (bits and other.bits.inv()) == other.bits

        operator fun plus(other: Usage): Usage =
            Usage(bits or other.bits)

        companion object {
            val COPY_DST = Usage(1 shl 0)
            val COPY_SRC = Usage(1 shl 1)
            val TEXTURE_BINDING = Usage(1 shl 2)
            val RENDER_ATTACHMENT = Usage(1 shl 3)
        }
    }
}

@NonExtendable
sealed interface UGpuTextureView : Closeable {
    val texture: UGpuTexture
    val baseMipLevel: Int
    val mipLevels: Int

    val isClosed: Boolean
}
