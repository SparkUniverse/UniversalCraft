package gg.essential.universal.render.font

import gg.essential.universal.ChatColor
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.render.ScissorState
import gg.essential.universal.render.UGpuDeviceImpl
import gg.essential.universal.render.UGpuTexture
import gg.essential.universal.render.UGpuTextureView
import gg.essential.universal.render.ViewportState
import org.lwjgl.opengl.GL11

//#if STANDALONE
//$$ import gg.essential.universal.UGraphics
//$$ import gg.essential.universal.UResolution
//$$ import gg.essential.universal.standalone.nanovg.NvgFont
//$$ import java.awt.Color
//#else
import net.minecraft.client.gui.FontRenderer

//#if MC >= 1.21.9
//$$ import net.minecraft.client.font.TextDrawable
//#elseif MC >= 1.21.6
//$$ import net.minecraft.client.font.BakedGlyph
//#endif

//#if MC >= 1.21.6
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import com.mojang.blaze3d.vertex.VertexFormat
//$$ import gg.essential.universal.render.UGpuBuffer
//$$ import gg.essential.universal.render.UGpuSampler
//$$ import gg.essential.universal.render.URenderPass
//$$ import gg.essential.universal.render.URenderPassDescriptor
//$$ import gg.essential.universal.render.URenderPipeline
//$$ import net.minecraft.client.render.BufferBuilder
//$$ import net.minecraft.client.render.fog.FogRenderer
//$$ import net.minecraft.client.util.BufferAllocator
//#endif

//#if MC >= 1.21.5
//$$ import gg.essential.universal.UGraphics
//$$ import net.minecraft.client.MinecraftClient
//#endif

//#if MC >= 1.21.2
//$$ import com.mojang.blaze3d.systems.ProjectionType
//#elseif MC >= 1.20
//$$ import com.mojang.blaze3d.systems.VertexSorter
//#endif

//#if MC >= 1.17 && MC < 1.21.6
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#endif

//#if MC >= 1.16 && MC < 1.21
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer
//$$ import net.minecraft.client.renderer.Tessellator
//#endif
//#endif

class UFontRenderer(
    //#if STANDALONE
    //$$ val mc: NvgFont,
    //#else
    val mc: FontRenderer,
    //#endif
) : AutoCloseable {
    constructor() : this(
        //#if STANDALONE
        //$$ UGraphics.MC_FONT,
        //#else
        UMinecraft.getFontRenderer(),
        //#endif
    )

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ private val allocators = mutableListOf<BufferAllocator>()
    //$$ private val fogRenderer = FogRenderer()
    //#endif

    class Text(
        val x: Int,
        val y: Int,
        val scale: Float,
        val text: String,
        val color: Int,
        val shadow: Boolean,
        /**
         * When non-null, uses this color for the entire shadow (provided [shadow] is `true`).
         * Otherwise the color of the shadow is derived from the color of the text, and is therefore also affected by
         * [formatting codes][ChatColor].
         */
        val shadowColor: Int?,
    )

    /**
     * Renders the given [texts] to the [destination] texture.
     *
     * Note: This currently assumes that the texts do not overlap. The output for overlapping texts is undefined.
     */
    fun render(destination: UGpuTextureView, texts: List<Text>) {
        require(destination.baseMipLevel == 0) { "baseMipLevel other than 0 is not yet supported" }

        //#if MC >= 1.21.6 && !STANDALONE
        //$$ val drawerImpl = GlyphDrawerImpl()
        //$$ for (text in texts) {
        //$$     val scale = text.scale
        //$$     val invScale = 1 / scale
        //$$     drawerImpl.matrix.m00(scale).m11(scale)
        //$$     val x = text.x * invScale
        //$$     val y = text.y * invScale
        //$$
        //$$     drawerImpl.currBatch = 0
        //$$
        //$$     if (text.shadow && text.shadowColor != null) {
        //$$         val shadowText = ChatColor.stripColorCodes(text.text)!!
        //$$         mc.prepare(shadowText, x + 1f, y + 1f, text.shadowColor, false, 0).draw(drawerImpl)
        //$$         mc.prepare(text.text, x, y, text.color, false, 0).draw(drawerImpl)
        //$$     } else {
        //$$         mc.prepare(text.text, x, y, text.color, text.shadow, 0).draw(drawerImpl)
        //$$     }
        //$$ }
        //$$
        //$$ val device = UGraphics.getDevice()
        //$$ val adapter = UGraphics.getPlatformAdapter()
        //$$ class DrawCall(
        //$$     val pipeline: URenderPipeline,
        //$$     val texture: UGpuTextureView,
        //$$     val vertexBuffer: UGpuBuffer,
        //$$     val indexBuffer: RenderSystem.ShapeIndexBuffer,
        //$$     val indexCount: Int,
        //$$ )
        //$$ val drawCalls = drawerImpl.batches.mapNotNull { batch ->
        //$$     batch.builder.endNullable()?.use { builtBuffer ->
        //$$         DrawCall(
        //$$             URenderPipeline.wrap(batch.pipeline),
        //$$             adapter.textureView(batch.texture),
        //$$             device.createBuffer(UGpuBuffer.Usage.VERTEX, builtBuffer.buffer),
        //$$             RenderSystem.getSequentialBuffer(builtBuffer.drawParameters.mode()),
        //$$             builtBuffer.drawParameters.indexCount(),
        //$$         )
        //$$     }
        //$$ }
        //$$ if (drawCalls.isEmpty()) return
        //$$
        //$$ val fogGpuBuffer = fogRenderer.getFogBuffer(FogRenderer.FogType.NONE)
        //$$
        //$$ val descriptor = URenderPassDescriptor { "Font rendering" }
        //$$     .withColorAttachment(destination)
        //$$ device.createRenderPass(descriptor).use { renderPass ->
        //$$     val w = destination.texture.width
        //$$     val h = destination.texture.height
        //$$     renderPass.projectionMatrix(floatArrayOf(
        //$$         2f/w, 0f,    0f,   0f,
        //$$         0f,   -2f/h, 0f,   0f,
        //$$         0f,   0f,    1f,   0f,
        //$$         -1f,  1f,    0f,   1f,
        //$$     ))
        //$$     renderPass.uniform("Fog", UGraphics.getPlatformAdapter().bufferSlice(fogGpuBuffer))
        //$$     val samplerNearest = UGpuSampler(
        //$$         UGpuSampler.AddressMode.CLAMP_TO_EDGE,
        //$$         UGpuSampler.AddressMode.CLAMP_TO_EDGE,
        //$$         UGpuSampler.FilterMode.NEAREST,
        //$$         UGpuSampler.FilterMode.NEAREST,
        //$$         false,
        //$$     )
        //$$     renderPass.texture("Sampler2", adapter.textureView(drawerImpl.lightTexture), UGpuSampler(
        //$$         UGpuSampler.AddressMode.CLAMP_TO_EDGE,
        //$$         UGpuSampler.AddressMode.CLAMP_TO_EDGE,
        //$$         UGpuSampler.FilterMode.LINEAR,
        //$$         UGpuSampler.FilterMode.LINEAR,
        //$$         false,
        //$$     ))
        //$$     for (drawCall in drawCalls) {
        //$$         renderPass.pipeline(drawCall.pipeline)
        //$$         renderPass.vertexBuffer(0, drawCall.vertexBuffer.slice())
        //$$         renderPass.indexBuffer(
        //$$             adapter.buffer(drawCall.indexBuffer.getIndexBuffer(drawCall.indexCount)),
        //$$             when (drawCall.indexBuffer.indexType) {
                        //#if MC >= 26.2
                        //$$ com.mojang.blaze3d.IndexType.SHORT -> URenderPass.IndexType.SHORT
                        //$$ com.mojang.blaze3d.IndexType.INT -> URenderPass.IndexType.INT
                        //#else
                        //$$ VertexFormat.IndexType.SHORT -> URenderPass.IndexType.SHORT
                        //$$ VertexFormat.IndexType.INT -> URenderPass.IndexType.INT
                        //#endif
        //$$                 null -> throw NullPointerException()
        //$$             },
        //$$         )
        //$$         renderPass.texture("Sampler0", drawCall.texture, samplerNearest)
        //$$         renderPass.drawIndexed(drawCall.indexCount)
        //$$     }
        //$$ }
        //$$ drawCalls.forEach { it.vertexBuffer.close() }
        //#else
        val texture = destination.texture
        withDrawFramebuffer(texture) {
            val prevViewport = ViewportState.active()
            val viewport = ViewportState(0, 0, texture.width, texture.height)
            if (viewport != prevViewport) {
                viewport.activate()
            }
            val prevScissor = ScissorState.active()
            if (prevScissor.enabled) ScissorState.DISABLED.activate()

            val projMat = UMatrixStack()
            projMat.scale(1f, -1f, 1f)
            projMat.translate(-1f, -1f, 0f)
            projMat.scale(2f / texture.width, 2f / texture.height, 1f)

            //#if STANDALONE
            //$$ val prevWidth = UResolution.viewportWidth
            //$$ val prevHeight = UResolution.viewportHeight
            //$$ val prevScale = UMinecraft.guiScale
            //$$ UResolution.viewportWidth = texture.width
            //$$ UResolution.viewportHeight = texture.height
            //$$ UMinecraft.guiScale = 1
            //$$ UMatrixStack.GLOBAL_STACK.push()
            //$$ UMatrixStack().replaceGlobalState()
            //#elseif MC >= 1.17
            //$$ val orgProjMat = RenderSystem.getProjectionMatrix()
            //#if MC >= 1.21.2
            //$$ val orgProjectionType = RenderSystem.getProjectionType()
            //$$ RenderSystem.setProjectionMatrix(projMat.peek().model, ProjectionType.ORTHOGRAPHIC)
            //#elseif MC >= 1.20
            //$$ val orgVertexSorter = RenderSystem.getVertexSorting()
            //$$ RenderSystem.setProjectionMatrix(projMat.peek().model, VertexSorter.BY_Z)
            //#else
            //$$ RenderSystem.setProjectionMatrix(projMat.peek().model)
            //#endif
            //#if MC >= 1.20.6
            //$$ RenderSystem.getModelViewStack().pushMatrix()
            //$$ RenderSystem.getModelViewStack().identity()
            //#else
            //$$ RenderSystem.getModelViewStack().push()
            //$$ RenderSystem.getModelViewStack().loadIdentity()
            //#endif
            //#if MC < 1.21.2
            //$$ RenderSystem.applyModelViewMatrix()
            //#endif
            //#else
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPushMatrix()
            projMat.replaceGlobalState()
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glPushMatrix()
            GL11.glLoadIdentity()
            //#endif

            //#if MC >= 1.16
            //$$ val stack = UMatrixStack()
            //#endif

            //#if STANDALONE
            //#elseif MC >= 1.21
            //$$ val buffer = UMinecraft.getMinecraft().bufferBuilders.entityVertexConsumers
            //#elseif MC >= 1.16
            //$$ val buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().buffer)
            //#endif

            var currScale = 1f
            for (text in texts) {
                val scale = text.scale
                //#if STANDALONE
                //$$ // scaling passed through to `drawString`
                //$$ val x = text.x.toFloat()
                //$$ val y = text.y.toFloat()
                //#else
                if (scale != currScale) {
                    //#if MC >= 1.17
                    //#if MC >= 1.20.6
                    //$$ stack.peek().model.identity()
                    //#else
                    //$$ stack.peek().model.loadIdentity()
                    //#endif
                    //$$ stack.scale(scale, scale, scale)
                    //#else
                    GL11.glLoadIdentity()
                    GL11.glScalef(scale, scale, scale)
                    //#endif
                    currScale = scale
                }
                val invScale = 1 / scale
                val x = text.x * invScale
                val y = text.y * invScale
                //#endif

                if (text.shadow && text.shadowColor != null) {
                    val shadowText = ChatColor.stripColorCodes(text.text)!!
                    //#if STANDALONE
                    //$$ mc.drawString(UMatrixStack.UNIT, shadowText, Color(text.shadowColor), x + 1f, y + 1f, 10f, scale, false, null)
                    //$$ mc.drawString(UMatrixStack.UNIT, text.text, Color(text.color), x, y, 10f, scale, false, null)
                    //#elseif MC >= 1.16
                    //$$ mc.renderString(shadowText, x + 1f, y + 1f, text.shadowColor, false, stack.peek().model, buffer, TEXT_LAYER_TYPE, 0, 15728880)
                    //$$ mc.renderString(text.text, x, y, text.color, false, stack.peek().model, buffer, TEXT_LAYER_TYPE, 0, 15728880)
                    //#elseif MC >= 1.15
                    //$$ mc.drawString(shadowText, x + 1f, y + 1f, text.shadowColor)
                    //$$ mc.drawString(text.text, x, y, text.color)
                    //#else
                    mc.drawString(shadowText, x + 1f, y + 1f, text.shadowColor, false)
                    mc.drawString(text.text, x, y, text.color, false)
                    //#endif
                } else {
                    //#if STANDALONE
                    //$$ mc.drawString(UMatrixStack.UNIT, text.text, Color(text.color), x, y, 10f, scale, text.shadow, null)
                    //#elseif MC >= 1.16
                    //$$ mc.renderString(text.text, x, y, text.color, text.shadow, stack.peek().model, buffer, TEXT_LAYER_TYPE, 0, 15728880)
                    //#elseif MC >= 1.15
                    //$$ if (text.shadow) {
                    //$$     mc.drawStringWithShadow(text.text, x, y, text.color)
                    //$$ } else {
                    //$$     mc.drawString(text.text, x, y, text.color)
                    //$$ }
                    //#else
                    mc.drawString(text.text, x, y, text.color, text.shadow)
                    //#endif
                }
            }

            //#if STANDALONE
            //#elseif MC >= 1.16
            //$$ buffer.finish()
            //#endif

            //#if STANDALONE
            //$$ UMatrixStack.GLOBAL_STACK.pop()
            //$$ UResolution.viewportWidth = prevWidth
            //$$ UResolution.viewportHeight = prevHeight
            //$$ UMinecraft.guiScale = prevScale
            //#elseif MC >= 1.17
            //#if MC >= 1.20.6
            //$$ RenderSystem.getModelViewStack().popMatrix()
            //#else
            //$$ RenderSystem.getModelViewStack().pop()
            //#endif
            //#if MC < 1.21.2
            //$$ RenderSystem.applyModelViewMatrix()
            //#endif
            //#if MC >= 1.21.2
            //$$ RenderSystem.setProjectionMatrix(orgProjMat, orgProjectionType)
            //#elseif MC >= 1.20
            //$$ RenderSystem.setProjectionMatrix(orgProjMat, orgVertexSorter)
            //#else
            //$$ RenderSystem.setProjectionMatrix(orgProjMat)
            //#endif
            //#else
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPopMatrix()
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glPopMatrix()
            //#endif

            if (prevScissor.enabled) prevScissor.activate()
            if (viewport != prevViewport) {
                prevViewport.activate()
            }
        }
        //#endif
    }

    override fun close() {
        //#if MC >= 1.21.6 && !STANDALONE
        //$$ allocators.forEach { it.close() }
        //$$ allocators.clear()
        //$$ fogRenderer.close()
        //#endif
    }

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ private class DrawBatch(
    //$$     val pipeline: RenderPipeline,
    //$$     val texture: GpuTextureView,
    //$$     val builder: BufferBuilder,
    //$$ )
    //$$ private inner class GlyphDrawerImpl : TextRenderer.GlyphDrawer {
        //#if MC >= 26.1
        //$$ val lightTexture = Minecraft.getInstance().gameRenderer.lightmap()
        //$$ // lightmap() may return either the uiLightmap, which is 1x1, or the regular lightmap (like pre-26.1)
        //$$ // and the text shader uses texelFetch which ignores the wrapping mode, so we need to pass it 0/0 as the
        //$$ // light coord or it will simply return 0 as the color.
        //$$ // for the 0, see GlyphRenderState.buildVertices
        //$$ // for the 0x00F0_00F0, see e.g. DrawableGizmoPrimitives.Group.renderTexts
        //$$ val light = if (lightTexture.getWidth(0) == 1) 0 else 0x00F0_00F0
        //#else
        //$$ val lightTexture = MinecraftClient.getInstance().gameRenderer.lightmapTextureManager.glTextureView
        //$$ val light = 0x00F0_00F0 // see GlyphGuiElementRenderState.setupVertices
        //#endif
    //$$
    //$$     // Ideally we'd render everything in a single draw call. But when MC has seen enough glyphs that they don't
    //$$     // fit on its single atlas any more, it starts a second atlas, at which point we'll need multiple draw
    //$$     // calls with different textures.
    //$$     // Within a single text we have to preserve the order in which MC supplies us the glyphs, otherwise we
    //$$     // could end up rendering the background on the foreground, or strikethrough behind the letters, etc.
    //$$     // Different texts are independent though, and so can share the same set of draw batches.
    //$$     val batches = mutableListOf<DrawBatch>()
    //$$     var currBatch = 0
    //$$
    //$$     val matrix = org.joml.Matrix4f()
    //$$
    //$$     private fun builder(pipeline: RenderPipeline, texture: GpuTextureView): BufferBuilder {
    //$$         while (true) {
    //$$             if (currBatch > batches.lastIndex) {
    //$$                 if (currBatch > allocators.lastIndex) allocators.add(BufferAllocator(65536))
    //$$                 val builder = BufferBuilder(
    //$$                     allocators[currBatch],
                        //#if MC >= 26.2
                        //$$ pipeline.primitiveTopology,
                        //$$ pipeline.getVertexFormatBinding(0)!!,
                        //#else
                        //$$ pipeline.vertexFormatMode,
                        //$$ pipeline.vertexFormat,
                        //#endif
    //$$                 )
    //$$                 batches.add(DrawBatch(pipeline, texture, builder))
    //$$             }
    //$$             val batch = batches[currBatch]
    //$$             if (batch.pipeline == pipeline && batch.texture == texture) {
    //$$                 return batch.builder
    //$$             }
    //$$             currBatch++
    //$$         }
    //$$     }
    //$$
    //#if MC >= 1.21.9
    //$$     private fun draw(drawable: TextDrawable) =
    //$$         drawable.render(matrix, builder(drawable.pipeline, drawable.textureView()), light, false)
    //#if MC >= 1.21.11
    //$$     override fun drawGlyph(glyph: TextDrawable.DrawnGlyphRect) = draw(glyph)
    //#else
    //$$     override fun drawGlyph(drawable: TextDrawable) = draw(drawable)
    //#endif
    //$$     override fun drawRectangle(drawable: TextDrawable) = draw(drawable)
    //#else
    //$$     override fun drawGlyph(drawnGlyph: BakedGlyph.DrawnGlyph) {
    //$$         val bakedGlyph = drawnGlyph.glyph()
    //$$         val texture = bakedGlyph.texture ?: return
    //$$         bakedGlyph.draw(drawnGlyph, matrix, builder(bakedGlyph.pipeline, texture), light, false)
    //$$     }
    //$$     override fun drawRectangle(bakedGlyph: BakedGlyph, rectangle: BakedGlyph.Rectangle) {
    //$$         val texture = bakedGlyph.texture ?: return
    //$$         bakedGlyph.drawRectangle(rectangle, matrix, builder(bakedGlyph.pipeline, texture), light, false)
    //$$     }
    //#endif
    //$$ }
    //#else
    private inline fun withDrawFramebuffer(color: UGpuTexture, block: () -> Unit) {
        //#if MC == 1.21.5
        //$$ val fb = MinecraftClient.getInstance().framebuffer
        //$$ val orgColor = fb.colorAttachment
        //$$ val orgDepth = fb.depthAttachment
        //$$ fbAttachmentFieldSetters.first.invoke(fb, UGraphics.getPlatformAdapter().texture(color))
        //$$ fbAttachmentFieldSetters.second.invoke(fb, null)
        //$$ try {
        //$$     return block()
        //$$ } finally {
        //$$     fbAttachmentFieldSetters.first.invoke(fb, orgColor)
        //$$     fbAttachmentFieldSetters.second.invoke(fb, orgDepth)
        //$$ }
        //#else
        UGpuDeviceImpl.withDrawFramebuffer(color, null, block)
        //#endif
    }
    //#endif

    //#if MC == 1.21.5
    //$$ private val fbAttachmentFieldSetters by lazy {
    //$$     val fb = MinecraftClient.getInstance().framebuffer
    //$$     val lookup = java.lang.invoke.MethodHandles.lookup()
    //$$     val cls = net.minecraft.client.gl.Framebuffer::class.java
    //$$     val colorField = lookup.unreflectSetter(cls.declaredFields.first {
    //$$         it.type == com.mojang.blaze3d.textures.GpuTexture::class.java && it.get(fb) == fb.colorAttachment
    //$$     }.also { it.isAccessible = true })
    //$$     val depthField = lookup.unreflectSetter(cls.declaredFields.first {
    //$$         it.type == com.mojang.blaze3d.textures.GpuTexture::class.java && it.get(fb) == fb.depthAttachment
    //$$     }.also { it.isAccessible = true })
    //$$     Pair(colorField, depthField)
    //$$ }
    //#endif

    private companion object {
        //#if MC >= 1.21.6
        //#elseif MC >= 1.19.4
        //$$ private val TEXT_LAYER_TYPE = TextRenderer.TextLayerType.NORMAL
        //#elseif MC >= 1.16
        //$$ private val TEXT_LAYER_TYPE = false
        //#endif
    }
}
