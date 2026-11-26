package gg.essential.universal.utils

import gg.essential.universal.UGraphics
import gg.essential.universal.UResolution
import gg.essential.universal.render.UGpuTextureView
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.util.Identifier

//#if MC >= 26.4
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import com.mojang.renderpearl.api.textures.FilterMode
//$$ import net.minecraft.client.gui.GuiPipeline
//$$ import net.minecraft.client.renderer.texture.ExternalTextureProvider
//$$ import net.minecraft.client.renderer.texture.TextureResources
//#endif

internal fun DrawContext.drawTexture(textureView: UGpuTextureView) = drawTextureImpl(this, textureView)
private fun drawTextureImpl(context: DrawContext, textureView: UGpuTextureView) {
    val width = textureView.texture.width
    val height = textureView.texture.height
    val scaleFactor = UResolution.scaleFactor.toFloat()

    val textureManager = MinecraftClient.getInstance().textureManager
    val identifier = Identifier.of("universalcraft", "__tmp_texture__")
    //#if MC >= 26.4
    //$$ textureManager.registerProvider(identifier, ExternalTextureProvider(TextureResources(
    //$$     UGraphics.getPlatformAdapter().texture(textureView.texture),
    //$$     UGraphics.getPlatformAdapter().textureView(textureView),
    //$$     RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
    //$$ )))
    //#else
    textureManager.registerTexture(identifier, object : AbstractTexture() {
        init { this.glTextureView = UGraphics.getPlatformAdapter().textureView(textureView) }
        override fun close() {} // we don't want the later `destroyTexture` to close our texture
    })
    //#endif

    context.matrices.pushMatrix()
    context.matrices.scale(1 / scaleFactor, 1 / scaleFactor) // drawTexture only accepts `int`s
    context.drawTexture(
        //#if MC >= 26.4
        //$$ GuiPipeline.TEXTURED_PREMULTIPLIED_ALPHA,
        //#else
        RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
        //#endif
        identifier,
        // x, y
        0, 0,
        // u, v
        0f, height.toFloat(),
        // width, height
        width, height,
        // uWidth, vHeight
        width, -height,
        // textureWidth, textureHeight
        width, height,
    )
    context.matrices.popMatrix()

    textureManager.destroyTexture(identifier)
}
