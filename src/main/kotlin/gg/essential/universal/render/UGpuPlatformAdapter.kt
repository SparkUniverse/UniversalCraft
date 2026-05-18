package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable

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
//#endif

@NonExtendable
sealed interface UGpuPlatformAdapter {
    val defaultGpuFormatRgba: UGpuFormat
    val defaultGpuFormatDepth: UGpuFormat

    //#if MC >= 26.2 && !STANDALONE
    //$$ fun gpuFormat(gpuFormat: GpuFormat): UGpuFormat
    //#else
    /** @see org.lwjgl.opengl.GL11.glTexImage2D */
    fun gpuFormat(internalFormat: Int, format: Int, type: Int): UGpuFormat
    //#endif

    //#if MC >= 1.21.5 && !STANDALONE
    //$$ fun texture(texture: GpuTexture): UGpuTexture
    //$$ fun texture(texture: UGpuTexture): GpuTexture
    //#else
    fun texture(glId: Int, format: UGpuFormat, width: Int, height: Int, mipLevels: Int): UGpuTexture
    fun texture(texture: UGpuTexture): Int
    //#endif

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ fun textureView(textureView: GpuTextureView): UGpuTextureView
    //$$ fun textureView(textureView: UGpuTextureView): GpuTextureView
    //#endif
}
