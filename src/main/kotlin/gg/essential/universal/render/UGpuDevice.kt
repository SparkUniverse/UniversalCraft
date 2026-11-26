package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable
import java.nio.ByteBuffer

@NonExtendable
sealed interface UGpuDevice {
    val info: Info

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

    fun clearColor(texture: UGpuTexture, red: Float, green: Float, blue: Float, alpha: Float)
    fun clearDepth(texture: UGpuTexture, depth: Double)

    fun createBuffer(usage: UGpuBuffer.Usage, size: Long): UGpuBuffer
    fun createBuffer(usage: UGpuBuffer.Usage, buffer: ByteBuffer): UGpuBuffer

    fun mapBuffer(gpuBufferSlice: UGpuBufferSlice, read: Boolean, write: Boolean): MappedBuffer
    interface MappedBuffer : AutoCloseable {
        val data: ByteBuffer
    }

    fun copyBufferToTexture(
        source: UGpuBufferSlice,
        sourceX: Int,
        sourceY: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        destination: UGpuTexture,
        destinationX: Int,
        destinationY: Int,
        copyWidth: Int,
        copyHeight: Int,
        mipLevel: Int,
        arrayLayer: Int,
    )
    fun copyTextureToBuffer(
        source: UGpuTexture,
        sourceX: Int,
        sourceY: Int,
        destination: UGpuBufferSlice,
        destinationX: Int,
        destinationY: Int,
        // Note: destinationWidth currently must match copyWidth when more than one line is being copied
        destinationWidth: Int,
        destinationHeight: Int,
        copyWidth: Int,
        copyHeight: Int,
        mipLevel: Int,
        arrayLayer: Int,
    )
    fun copyTextureToTexture(
        source: UGpuTexture,
        sourceX: Int,
        sourceY: Int,
        sourceMipLevel: Int,
        sourceArrayLayer: Int,
        destination: UGpuTexture,
        destinationX: Int,
        destinationY: Int,
        destinationMipLevel: Int,
        destinationArrayLayer: Int,
        copyWidth: Int,
        copyHeight: Int,
    )

    fun createFence(): UGpuFence

    fun createRenderPass(descriptor: URenderPassDescriptor): URenderPass

    @NonExtendable
    interface Info {
        val isZZeroToOne: Boolean
        val limits: Limits
    }

    @NonExtendable
    interface Limits {
        fun maxTextureSize(format: UGpuFormat): Int
    }
}
