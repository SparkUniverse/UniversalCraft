package gg.essential.universal.render

import gg.essential.universal.UGraphics
import java.nio.ByteBuffer
import kotlin.math.max
import org.lwjgl.opengl.GL11

//#if STANDALONE
//$$ import org.lwjgl.opengl.GL20C
//#else
//#if MC >= 1.21.5
//$$ import com.mojang.blaze3d.textures.TextureFormat
//$$ import net.minecraft.client.texture.GlTexture
//#endif

//#if MC >= 1.21.5 && MC < 26.1
//$$ import net.minecraft.client.gl.GlBackend
//#endif

//#if MC >= 1.16
//$$ import com.mojang.blaze3d.platform.GlStateManager
//#endif

import net.minecraft.client.renderer.GlStateManager
//#endif

internal object UGpuDeviceImpl : UGpuDevice {
    override fun createTexture(
        label: String?,
        usage: UGpuTexture.Usage,
        format: UGpuFormat,
        width: Int,
        height: Int,
        mipLevels: Int
    ): UGpuTexture {
        require(usage.bits != 0) { "At least one usage bit must be set" }
        require(width > 0) { "Width must be positive but was $width" }
        require(height > 0) { "Height must be positive but was $height" }
        require(mipLevels > 0) { "Mip levels must be positive but was $mipLevels"}
        fun log2(x: Int) = 31 - x.countLeadingZeroBits()
        val maxMipLevels = log2(max(width, height)) + 1
        require(mipLevels <= maxMipLevels) { "Texture of size ${width}x${height} supports at most $maxMipLevels but $mipLevels were requested" }

        //#if STANDALONE
        //$$ return createGlTexture(label, usage, format.impl, width, height, mipLevels)
        //#elseif MC >= 26.2
        //$$ return createB3DTexture(label, usage, format.impl.mc, width, height, mipLevels)
        //#elseif MC >= 1.21.5
        //$$ return when (format) {
        //$$     UGpuPlatformAdapterImpl.defaultGpuFormatRgba ->
        //$$         createB3DTexture(label, usage, format.impl, TextureFormat.RGBA8, width, height, mipLevels)
        //$$     UGpuPlatformAdapterImpl.defaultGpuFormatDepth ->
        //$$         createB3DTexture(label, usage, format.impl, TextureFormat.DEPTH32, width, height, mipLevels)
        //$$     else ->
        //$$         createGlTexture(label, usage, format.impl, width, height, mipLevels)
        //$$ }
        //#else
        return createGlTexture(label, usage, format.impl, width, height, mipLevels)
        //#endif
    }

    //#if MC >= 1.21.5 && !STANDALONE
    //$$ private fun createB3DTexture(
    //$$     label: String?,
    //$$     usage: UGpuTexture.Usage,
        //#if MC < 26.2
        //$$ format: UGpuFormatImpl,
        //#endif
    //$$     b3dFormat: TextureFormat,
    //$$     width: Int,
    //$$     height: Int,
    //$$     mipLevels: Int
    //$$ ): UGpuTextureImpl {
    //$$     val b3dTexture = RenderSystem.getDevice().createTexture(
    //$$         label,
            //#if MC >= 1.21.6
            //$$ usage.bits,
            //#endif
    //$$         b3dFormat,
    //$$         width,
    //$$         height,
            //#if MC >= 1.21.6
            //$$ 1,
            //#endif
    //$$         mipLevels,
    //$$     )
    //$$     return UGpuTextureImpl(
            //#if MC < 1.21.6
            //$$ usage,
            //#endif
            //#if MC < 26.2
            //$$ format,
            //#endif
    //$$         b3dTexture,
    //$$     )
    //$$ }
    //#endif

    //#if MC < 26.2 || STANDALONE
    private fun createGlTexture(
        label: String?,
        usage: UGpuTexture.Usage,
        format: UGpuFormatImpl,
        width: Int,
        height: Int,
        mipLevels: Int
    ): UGpuTextureImpl {
        //#if STANDALONE
        //$$ val glId = GL20C.glGenTextures()
        //#elseif MC >= 1.16
        //$$ val glId = GlStateManager.genTexture()
        //#else
        val glId = GlStateManager.generateTexture()
        //#endif

        UGraphics.configureTexture(glId) {
            GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                format.internalFormat,
                width,
                height,
                0,
                format.format,
                format.type,
                null as ByteBuffer?,
            )
        }

        //#if MC >= 1.21.5 && !STANDALONE
        //$$ val glTexture = object : GlTexture(
            //#if MC >= 1.21.6
            //$$ usage.bits,
            //#endif
        //$$     label ?: "$glId",
        //$$     if (format.hasDepth) TextureFormat.DEPTH32 else TextureFormat.RGBA8,
        //$$     width,
        //$$     height,
            //#if MC >= 1.21.6
            //$$ 1, // depthOrLayers
            //#endif
        //$$     mipLevels,
        //$$     glId,
        //$$ ) {}
        //#endif

        //#if MC >= 1.21.5 && MC < 26.1
        //$$ (RenderSystem.getDevice() as? GlBackend)?.debugLabelManager?.labelGlTexture(glTexture)
        //#else
        // TODO could backport this
        @Suppress("unused", "UNUSED_VARIABLE")
        val _label = label
        //#endif

        return UGpuTextureImpl(
            //#if MC < 1.21.6 || STANDALONE
            usage,
            //#endif
            //#if MC < 26.2 || STANDALONE
            format,
            //#endif
            //#if MC >= 1.21.5 && !STANDALONE
            //$$ glTexture,
            //#else
            glId,
            width,
            height,
            mipLevels,
            //#endif
        )
    }
    //#endif

    override fun createTextureView(
        texture: UGpuTexture,
        baseMipLevel: Int,
        mipLevels: Int
    ): UGpuTextureView {
        require(!texture.isClosed) { "Texture is closed" }
        require(baseMipLevel >= 0) { "Base mip level cannot be negative but was $baseMipLevel" }
        require(mipLevels > 0) { "Mip levels must be positive but was $mipLevels" }
        require(baseMipLevel + mipLevels <= texture.mipLevels) { "$baseMipLevel + $mipLevels exceeds available mip levels (${texture.mipLevels})" }

        //#if MC >= 1.21.6 && !STANDALONE
        //$$ val view = RenderSystem.getDevice().createTextureView(texture.impl.mc)
        //$$ return UGpuTextureViewImpl(texture.impl, view)
        //#else
        return UGpuTextureViewImpl(texture.impl, baseMipLevel, mipLevels)
        //#endif
    }
}
