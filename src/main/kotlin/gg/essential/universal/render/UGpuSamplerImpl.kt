package gg.essential.universal.render

import gg.essential.universal.render.UGpuSampler.AddressMode
import gg.essential.universal.render.UGpuSampler.FilterMode

//#if MC >= 1.21.11 && !STANDALONE
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import net.minecraft.client.gl.GpuSampler
//#elseif MC >= 1.21.6 && !STANDALONE
//$$ import com.mojang.blaze3d.textures.GpuTexture
//#else
import gg.essential.universal.UGraphics
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
//#endif

internal val UGpuSampler.impl: UGpuSamplerImpl
    get() = when (this) { is UGpuSamplerImpl -> this }

//#if MC >= 1.21.11 && !STANDALONE
//$$ internal data class UGpuSamplerImpl(val mc: GpuSampler) : UGpuSampler
//#else
internal data class UGpuSamplerImpl(
    val addressModeU: AddressMode,
    val addressModeV: AddressMode,
    val minFilter: FilterMode,
    val magFilter: FilterMode,
    val useMipmaps: Boolean,
) : UGpuSampler {

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ private val AddressMode.mc: com.mojang.blaze3d.textures.AddressMode
    //$$     get() = when (this) {
    //$$         AddressMode.REPEAT -> com.mojang.blaze3d.textures.AddressMode.REPEAT
    //$$         AddressMode.CLAMP_TO_EDGE -> com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE
    //$$     }
    //$$ private val FilterMode.mc: com.mojang.blaze3d.textures.FilterMode
    //$$     get() = when (this) {
    //$$         FilterMode.NEAREST -> com.mojang.blaze3d.textures.FilterMode.NEAREST
    //$$         FilterMode.LINEAR -> com.mojang.blaze3d.textures.FilterMode.LINEAR
    //$$     }
    //$$
    //$$ fun configureTexture(texture: GpuTexture) {
    //$$     texture.setAddressMode(addressModeU.mc, addressModeV.mc)
    //$$     texture.setTextureFilter(minFilter.mc, magFilter.mc, useMipmaps)
    //$$ }
    //#else
    private val AddressMode.gl: Int
        get() = when (this) {
            AddressMode.REPEAT -> GL11.GL_REPEAT
            AddressMode.CLAMP_TO_EDGE -> GL12.GL_CLAMP_TO_EDGE
        }
    private val FilterMode.gl: Int
        get() = when (this) {
            FilterMode.NEAREST -> GL11.GL_NEAREST
            FilterMode.LINEAR -> GL11.GL_LINEAR
        }

    fun configureTexture(textureGlId: Int) {
        UGraphics.configureTexture(textureGlId) {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, addressModeU.gl)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, addressModeV.gl)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter.gl)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter.gl)
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, if (useMipmaps) 1000f else 0f)
        }
    }
    //#endif
}
//#endif
