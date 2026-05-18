package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable
import java.nio.ByteBuffer

@NonExtendable
sealed interface UGpuDevice {
    fun createTexture(
        label: String?,
        usage: UGpuTexture.Usage,
        format: UGpuFormat,
        width: Int,
        height: Int,
        mipLevels: Int = 1,
    ): UGpuTexture

    fun createTextureView(
        texture: UGpuTexture,
        baseMipLevel: Int = 0,
        mipLevels: Int = texture.mipLevels,
    ): UGpuTextureView
}
