package gg.essential.universal.render

import gg.essential.universal.render.UGpuDeviceImpl.requireValidTextureSize
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14

//#if STANDALONE
//#else
//#if MC >= 1.21.6
//$$ import com.mojang.blaze3d.buffers.GpuBuffer
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//#endif
//#if MC >= 1.21.5
//$$ import com.mojang.blaze3d.textures.GpuTexture
//$$ import com.mojang.blaze3d.textures.TextureFormat
//$$ import org.lwjgl.opengl.GL30
//#endif
//#endif

internal object UGpuPlatformAdapterImpl : UGpuPlatformAdapter {
    override val defaultGpuFormatRgba: UGpuFormatImpl =
        //#if MC >= 26.2 && !STANDALONE
        //$$ gpuFormat(GpuFormat.RGBA8_UNORM)
        //#else
        gpuFormat(GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE)
        //#endif
    override val defaultGpuFormatDepth: UGpuFormatImpl =
        //#if MC >= 26.2 && !STANDALONE
        //$$ gpuFormat(GpuFormat.D32_FLOAT)
        //#else
        gpuFormat(GL14.GL_DEPTH_COMPONENT32, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT)
        //#endif

    //#if MC >= 26.2 && !STANDALONE
    //$$ override fun gpuFormat(gpuFormat: GpuFormat): UGpuFormatImpl =
    //$$     UGpuFormatImpl(gpuFormat)
    //#else
    override fun gpuFormat(internalFormat: Int, format: Int, type: Int): UGpuFormatImpl =
        UGpuFormatImpl(internalFormat, format, type)
    //#endif

    //#if MC >= 1.21.5 && !STANDALONE
    //$$ override fun texture(texture: GpuTexture): UGpuTextureImpl =
    //$$     UGpuTextureImpl(
            //#if MC < 1.21.6
            //$$ UGpuTexture.Usage(-1),
            //#endif
            //#if MC < 26.2
            //$$ when (texture.format) {
            //$$     TextureFormat.RGBA8 -> defaultGpuFormatRgba
            //$$     TextureFormat.RED8 -> gpuFormat(GL30.GL_R8, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE)
                //#if MC >= 1.21.6
                //$$ TextureFormat.RED8I -> gpuFormat(GL30.GL_R8I, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE)
                //#endif
            //$$     TextureFormat.DEPTH32 -> defaultGpuFormatDepth
                //#if NEOFORGE
                //$$ TextureFormat.DEPTH24_STENCIL8 -> gpuFormat(GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8)
                //$$ TextureFormat.DEPTH32_STENCIL8 -> gpuFormat(GL30.GL_DEPTH32F_STENCIL8, GL30.GL_DEPTH_STENCIL, GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV)
                //#endif
            //$$     null -> throw NullPointerException()
            //$$ },
            //#endif
    //$$         texture,
    //$$     )
            //#if MC < 26.2
            //$$ .also { FboCacheFix.track(it.impl.mc) }
            //#endif
    //$$ override fun texture(texture: UGpuTexture): GpuTexture =
    //$$     texture.impl.mc
    //#else
    override fun texture(glId: Int, format: UGpuFormat, width: Int, height: Int, mipLevels: Int): UGpuTexture {
        requireValidTextureSize(width, height, mipLevels)
        return UGpuTextureImpl(UGpuTexture.Usage(-1), format.impl, glId, width, height, mipLevels)
    }
    override fun texture(texture: UGpuTexture): Int =
        texture.impl.glId
    //#endif

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ override fun textureView(textureView: GpuTextureView): UGpuTextureViewImpl =
    //$$     UGpuTextureViewImpl(texture(textureView.texture()), textureView)
            //#if MC < 26.2
            //$$ .also { FboCacheFix.track(it.impl.mc) }
            //#endif
    //$$ override fun textureView(textureView: UGpuTextureView): GpuTextureView =
    //$$     textureView.impl.mc
    //#endif

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ override fun buffer(buffer: GpuBuffer): UGpuBufferImpl =
    //$$     UGpuBufferImpl(buffer)
    //$$ override fun buffer(buffer: UGpuBuffer): GpuBuffer =
    //$$     buffer.impl.mc
    //#else
    override fun buffer(glId: Int, usage: UGpuBuffer.Usage, size: Long): UGpuBufferImpl =
        UGpuBufferImpl(usage, size, glId)
    override fun buffer(buffer: UGpuBuffer): Int =
        buffer.impl.glId
    //#endif

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ override fun bufferSlice(buffer: GpuBufferSlice): UGpuBufferSlice =
    //$$     UGpuBufferSlice(buffer(buffer.buffer), buffer.offset.toLong(), buffer.length.toLong())
    //$$ override fun bufferSlice(buffer: UGpuBufferSlice): GpuBufferSlice =
    //$$     buffer.mc
    //#endif
}
