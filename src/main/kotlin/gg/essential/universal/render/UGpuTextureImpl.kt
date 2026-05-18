package gg.essential.universal.render

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

//#if STANDALONE
//#else
//#if MC >= 26.2
//$$ import com.mojang.blaze3d.GpuFormat
//#endif
//#if MC >= 1.21.6
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//#endif
//#if MC >= 1.21.5
//$$ import com.mojang.blaze3d.textures.GpuTexture
//#endif

import net.minecraft.client.renderer.GlStateManager
//#endif

internal data class UGpuFormatImpl(
    //#if MC >= 26.2 && !STANDALONE
    //$$ val mc: GpuFormat,
    //#else
    val internalFormat: Int,
    val format: Int,
    val type: Int,
    //#endif
) : UGpuFormat {
    val hasColor: Boolean
        //#if MC >= 26.2 && !STANDALONE
        //$$ get() = mc.hasColorAspect()
        //#else
        get() = when (format) {
            GL11.GL_STENCIL_INDEX, GL11.GL_DEPTH_COMPONENT, GL30.GL_DEPTH_STENCIL -> false
            else -> true
        }
        //#endif
    val hasDepth: Boolean
        //#if MC >= 26.2 && !STANDALONE
        //$$ get() = mc.hasDepthAspect()
        //#else
        get() = when (format) {
            GL11.GL_DEPTH_COMPONENT, GL30.GL_DEPTH_STENCIL -> true
            else -> false
        }
        //#endif
}

internal class UGpuTextureImpl(
    //#if MC < 1.21.6 || STANDALONE
    val usage: UGpuTexture.Usage,
    //#endif
    //#if MC < 26.2 || STANDALONE
    val format: UGpuFormatImpl,
    //#endif
    //#if MC >= 1.21.5 && !STANDALONE
    //$$ val mc: GpuTexture,
    //#else
    val glId: Int,
    override val width: Int,
    override val height: Int,
    override val mipLevels: Int
    //#endif
) : UGpuTexture {

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ val usage: UGpuTexture.Usage
    //$$     get() = UGpuTexture.Usage(mc.usage())
    //#endif

    //#if MC >= 26.2 && !STANDALONE
    //$$ val format: UGpuFormatImpl
    //$$     get() = UGpuFormatImpl(mc.format)
    //#endif

    //#if MC >= 1.21.5 && !STANDALONE
    //$$ override val width: Int get() = mc.getWidth(0)
    //$$ override val height: Int get() = mc.getHeight(0)
    //$$ override val mipLevels: Int get() = mc.mipLevels
    //#endif

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ override val isClosed: Boolean get() = mc.isClosed
    //$$ override fun close() = mc.close()
    //#else
    private var refCount = 1

    internal fun increaseRefCount() {
        refCount++
    }

    internal fun decreaseRefCount() {
        refCount--

        if (refCount == 0) {
            free()
        }
    }

    private fun free() {
        //#if STANDALONE
        //$$ GL11.glDeleteTextures(glId)
        //#elseif MC >= 1.21.5
        //$$ mc.close()
        //#else
        GlStateManager.deleteTexture(glId)
        //#endif
    }

    override var isClosed = false
        private set

    override fun close() {
        if (!isClosed) {
            isClosed = true
            decreaseRefCount()
        }
    }
    //#endif
}

//#if MC >= 1.21.6 && !STANDALONE
//$$ internal class UGpuTextureViewImpl(
//$$     override val texture: UGpuTextureImpl,
//$$     val mc: GpuTextureView,
//$$ ) : UGpuTextureView {
//$$     override val baseMipLevel: Int get() = mc.baseMipLevel()
//$$     override val mipLevels: Int get() = mc.mipLevels()
//$$     override val isClosed: Boolean get() = mc.isClosed
//$$     override fun close() = mc.close()
//$$ }
//#else
internal class UGpuTextureViewImpl(
    override val texture: UGpuTextureImpl,
    override val baseMipLevel: Int,
    override val mipLevels: Int,
) : UGpuTextureView {
    init {
        texture.increaseRefCount()
    }

    override var isClosed: Boolean = false
        private set

    override fun close() {
        if (!isClosed) {
            isClosed = true
            texture.decreaseRefCount()
        }
    }
}
//#endif

internal val UGpuFormat.impl: UGpuFormatImpl
    get() = when (this) { is UGpuFormatImpl -> this }
internal val UGpuTexture.impl: UGpuTextureImpl
    get() = when (this) { is UGpuTextureImpl -> this }
internal val UGpuTextureView.impl: UGpuTextureViewImpl
    get() = when (this) { is UGpuTextureViewImpl -> this }
