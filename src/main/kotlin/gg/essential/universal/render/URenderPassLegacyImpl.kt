package gg.essential.universal.render

import gg.essential.universal.vertex.UBuiltBuffer
import gg.essential.universal.vertex.UBuiltBufferInternal
import kotlin.ranges.coerceIn

//#if STANDALONE
//#else
//#if MC >= 26.2
//$$ import com.mojang.blaze3d.vertex.VertexFormat
//$$ import java.nio.ByteBuffer
//#endif

//#if MC>=12111
//$$ import com.mojang.blaze3d.textures.AddressMode
//$$ import com.mojang.blaze3d.textures.FilterMode
//$$ import net.minecraft.client.render.RenderLayers
//#endif

//#if MC>=12106
//$$ import com.mojang.blaze3d.buffers.GpuBuffer
//$$ import org.lwjgl.system.MemoryStack
//#endif

//#if MC>=12105
//$$ import com.mojang.blaze3d.systems.RenderPass
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import com.mojang.blaze3d.textures.TextureFormat
//$$ import gg.essential.universal.vertex.UBufferBuilder
//$$ import net.minecraft.client.MinecraftClient
//$$ import net.minecraft.client.texture.GlTexture
//#if MC >= 26.2
//$$ import java.util.Optional
//#else
//$$ import java.util.OptionalInt
//#endif
//$$ import java.util.OptionalDouble
//#endif
//#endif

/**
 * Legacy [DrawCallBuilder]-based render pass implementation.
 * Compared to proper URenderPass, it has the following shortcomings:
 * - Always draws to the main MC framebuffer (with RenderSystem override on versions that support it).
 * - Implicitly uses global scissor state if not explicitly specified.
 * - Implicitly uses global dynamic uniforms (e.g. model-view matrix, texture matrix, model offset, etc.).
 * - Implicitly uses global default uniforms (e.g. projection matrix).
 * - Only supports rendering from [UBuiltBuffer]. No way to render the same buffer multiple times. No way to specify
 *   custom index buffer.
 * - Uses a separate underlying MC `RenderPass` for each draw because it may need to write to GpuBuffers before each
 *   draw and writing to buffers is not supported during a `RenderPass`.
 */
internal class URenderPassLegacyImpl : AutoCloseable {
    //#if MC>=12105 && !STANDALONE
    //#else
    private val prevGlState = ManagedGlState.active()
    private val currGlState = ManagedGlState(prevGlState)
    private var currPipeline: URenderPipeline? = null
    //#endif

    override fun close() {
        //#if MC>=12105 && !STANDALONE
        //#else
        prevGlState.activate(currGlState, prevGlState, true)
        //#endif
    }

    fun draw(builtBuffer: UBuiltBuffer, pipeline: URenderPipeline, configure: (DrawCallBuilder) -> Unit) {
        val builder = DrawCallBuilderImpl(pipeline, builtBuffer as UBuiltBufferInternal)
        configure(builder)
        builder.submit()
        builder.close()
    }

    internal inner class DrawCallBuilderImpl(
        private val pipeline: URenderPipeline,
        private val builtBuffer: UBuiltBufferInternal,
    ) : DrawCallBuilder, AutoCloseable {
        //#if MC>=12106 && !STANDALONE
        //$$ private val tmpBuffers = mutableListOf<GpuBuffer>()
        //#endif

        //#if MC>=12105 && !STANDALONE
        //$$ val mc: RenderPass
        //$$ val outputTextureSize: Pair<Int, Int>
        //$$ init {
            //#if MC>=12106
            //$$ val dynamicUniforms = RenderSystem.getDynamicUniforms().write(
                //#if MC >= 26.2
                //$$ RenderSystem.getModelViewMatrixCopy(),
                //#else
                //$$ RenderSystem.getModelViewMatrix(),
                //#endif
            //$$     org.joml.Vector4f(1f, 1f, 1f, 1f),
            //#if MC>=12109
            //$$     org.joml.Vector3f(),
            //#else
            //$$     RenderSystem.getModelOffset(),
            //#endif
            //#if MC>=12111
            //$$     org.joml.Matrix4f(),
            //#else
            //$$     RenderSystem.getTextureMatrix(),
            //$$     RenderSystem.getShaderLineWidth(),
            //#endif
            //$$ )
            //#endif
            //#if MC >= 26.2
            //$$ fun VertexFormat.uploadImmediateVertexBuffer(buffer: ByteBuffer) =
            //$$     RenderSystem.getDevice().createBuffer({ "Immediate vertex buffer for $pipeline" }, GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_VERTEX, buffer)
            //$$         .also { tmpBuffers.add(it) }
            //$$ fun VertexFormat.uploadImmediateIndexBuffer(buffer: ByteBuffer) =
            //$$     RenderSystem.getDevice().createBuffer({ "Immediate index buffer for $pipeline" }, GpuBuffer.USAGE_COPY_DST or GpuBuffer.USAGE_INDEX, buffer)
            //$$         .also { tmpBuffers.add(it) }
            //#endif
        //$$     val builtBuffer = builtBuffer.mc
        //$$     val vertexBuffer = pipeline.format.uploadImmediateVertexBuffer(builtBuffer.buffer)
        //$$     val sortedBuffer = builtBuffer.sortedBuffer
        //$$     val (indexBuffer, indexType) = if (sortedBuffer != null) {
        //$$         pipeline.format.uploadImmediateIndexBuffer(sortedBuffer) to builtBuffer.drawParameters.indexType()
        //$$     } else {
        //$$         val shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.drawParameters.mode())
        //$$         shapeIndexBuffer.getIndexBuffer(builtBuffer.drawParameters.indexCount()) to shapeIndexBuffer.indexType
        //$$     }
            //#if MC >= 26.2
            //$$ mc = Minecraft.getInstance().gameRenderer.mainRenderTarget().let { fb ->
            //#else
            //$$ mc = MinecraftClient.getInstance().framebuffer.let { fb ->
            //#endif
                //#if MC >= 1.21.6
                //$$ val outputColorTexture = RenderSystem.outputColorTextureOverride ?: fb.colorAttachmentView!!
                //$$ val outputDepthTexture = RenderSystem.outputDepthTextureOverride ?: fb.depthAttachmentView
                //#else
                //$$ val outputColorTexture = fb.colorAttachment!!
                //$$ val outputDepthTexture = fb.depthAttachment
                //#endif
        //$$         outputTextureSize = Pair(outputColorTexture.getWidth(0), outputColorTexture.getHeight(0))
        //$$         RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    //#if MC>=12106
                    //$$ { "Immediate draw for $pipeline" },
                    //#endif
        //$$             outputColorTexture,
                    //#if MC >= 26.2
                    //$$ Optional.empty(),
                    //#else
                    //$$ OptionalInt.empty(),
                    //#endif
        //$$             outputDepthTexture,
        //$$             OptionalDouble.empty(),
        //$$         )
        //$$     }
            //#if MC >= 26.2
            //$$ mc.setVertexBuffer(0, vertexBuffer.slice())
            //#else
            //$$ mc.setVertexBuffer(0, vertexBuffer)
            //#endif
        //$$     mc.setIndexBuffer(indexBuffer, indexType)
            //#if MC>=12106
            //$$ RenderSystem.bindDefaultUniforms(mc)
            //$$ mc.setUniform("DynamicTransforms", dynamicUniforms);
            //#endif
        //$$ }
        //#else
        init {
            pipeline.bind()
        }
        //#endif

        private var scissor: ScissorState? = null

        override fun noScissor(): DrawCallBuilder = apply {
            scissor = ScissorState.DISABLED
        }

        override fun scissor(x: Int, y: Int, width: Int, height: Int) = apply {
            scissor = ScissorState(true, x, y, width, height)
        }

        override fun uniform(name: String, vararg values: Float): DrawCallBuilder = apply {
            //#if MC>=12105 && !STANDALONE
            //#if MC>=12106
            //$$ mc.setUniform(name, MemoryStack.stackPush().use { stack ->
            //$$     val byteBuf = stack.malloc(values.size * 4)
            //$$     values.forEach { byteBuf.putFloat(it) }
            //$$     byteBuf.flip()
            //$$     RenderSystem.getDevice().createBuffer({ "$name UBO" }, GpuBuffer.USAGE_UNIFORM, byteBuf)
            //$$ }.also { tmpBuffers.add(it) })
            //#else
            //$$ mc.setUniform(name, *values)
            //#endif
            //#else
            pipeline.uniform(name, *values)
            //#endif
        }

        override fun uniform(name: String, vararg values: Int): DrawCallBuilder = apply {
            //#if MC>=12105 && !STANDALONE
            //#if MC>=12106
            //$$ mc.setUniform(name, MemoryStack.stackPush().use { stack ->
            //$$     val byteBuf = stack.malloc(values.size * 4)
            //$$     values.forEach { byteBuf.putInt(it) }
            //$$     byteBuf.flip()
            //$$     RenderSystem.getDevice().createBuffer({ "$name UBO" }, GpuBuffer.USAGE_UNIFORM, byteBuf)
            //$$ }.also { tmpBuffers.add(it) })
            //#else
            //$$ mc.setUniform(name, *values)
            //#endif
            //#else
            pipeline.uniform(name, *values)
            //#endif
        }

        //#if MC >= 1.21.5 && !STANDALONE
        //$$ private fun samplerNameByIndex(index: Int) =
            //#if MC >= 26.2
            //$$ pipeline.mcRenderPipeline.bindGroupLayouts.asSequence().flatMap { it.samplers }.elementAt(index)
            //#else
            //$$ pipeline.mcRenderPipeline.samplers[index]
            //#endif
        //#endif

        override fun texture(name: String, textureView: UGpuTextureView, sampler: UGpuSampler): DrawCallBuilder = apply {
            //#if MC >= 1.21.5 && !STANDALONE
            //#if MC >= 1.21.11
            //$$ mc.bindTexture(name, textureView.impl.mc, sampler.impl.mc)
            //#elseif MC >= 1.21.6
            //$$ sampler.impl.configureTexture(textureView.texture.impl.mc)
            //$$ mc.bindSampler(name, textureView.impl.mc)
            //#else
            //$$ sampler.impl.configureTexture((textureView.texture.impl.mc as GlTexture).glId)
            //$$ mc.bindSampler(name, textureView.texture.impl.mc)
            //#endif
            //#else
            sampler.impl.configureTexture(textureView.texture.impl.glId)
            pipeline.texture(name, textureView.texture.impl.glId)
            //#endif
        }

        override fun texture(index: Int, textureView: UGpuTextureView, sampler: UGpuSampler): DrawCallBuilder = apply {
            //#if MC >= 1.21.5 && !STANDALONE
            //$$ texture(samplerNameByIndex(index), textureView, sampler)
            //#else
            sampler.impl.configureTexture(textureView.texture.impl.glId)
            pipeline.texture(index, textureView.texture.impl.glId)
            //#endif
        }

        @Deprecated("Does not support Vulkan; uses hard-coded sampler on 1.21.11+, relies on texture configuration on older versions.")
        override fun texture(name: String, textureGlId: Int): DrawCallBuilder = apply {
            //#if MC>=12105 && !STANDALONE
            //#if MC >= 26.2
            //$$ val texture = object : GlTexture(USAGE_TEXTURE_BINDING, "", GpuFormat.RGBA8_UNORM, 0, 0, 0, 1, textureGlId, null) {
            //#elseif MC>=12106
            //$$ val texture = object : GlTexture(USAGE_TEXTURE_BINDING, "", TextureFormat.RGBA8, 0, 0, 0, 1, textureGlId) {
            //#else
            //$$ val texture = object : GlTexture("", TextureFormat.RGBA8, 0, 0, 0, textureGlId) {
            //#endif
            //#if MC<12111
            //$$     init {
            //$$         needsReinit = false
            //$$     }
            //#endif
            //$$ }
            //#if MC>=12111
            //$$ val sampler = RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.NEAREST, true)
            //$$ mc.bindTexture(name, RenderSystem.getDevice().createTextureView(texture), sampler)
            //#elseif MC>=12106
            //$$ mc.bindSampler(name, RenderSystem.getDevice().createTextureView(texture))
            //#else
            //$$ mc.bindSampler(name, texture)
            //#endif
            //#else
            pipeline.texture(name, textureGlId)
            //#endif
        }

        @Deprecated("Does not support Vulkan; uses hard-coded sampler on 1.21.11+, relies on texture configuration on older versions.")
        override fun texture(index: Int, textureGlId: Int): DrawCallBuilder = apply {
            //#if MC>=12105 && !STANDALONE
            //$$ @Suppress("DEPRECATION")
            //$$ texture(samplerNameByIndex(index), textureGlId)
            //#else
            pipeline.texture(index, textureGlId)
            //#endif
        }

        fun submit() {
            //#if MC>=12105 && !STANDALONE
            //$$ var scissor = scissor ?: ScissorState.active()
            //$$ if (scissor.enabled) {
            //$$     // As of 26.2-snapshot-4, Minecraft will throw when the given scissor is out-of-bounds or empty.
            //$$     // Existing users of URenderPass are already used to it accepting such scissors, so we'll silently
            //$$     // coerce the given scissor to fit, and skip drawing altogether when it's empty.
            //$$     scissor = scissor.coerceIn(0, 0, outputTextureSize.first, outputTextureSize.second)
            //$$     if (scissor.width <= 0 || scissor.height <= 0) {
            //$$         return
            //$$     }
            //$$     mc.enableScissor(scissor.x, scissor.y, scissor.width, scissor.height)
            //$$ } else {
            //$$     mc.disableScissor()
            //$$ }
            //$$
            //$$ pipeline.draw(mc, builtBuffer.mc)
            //#else
            if (currPipeline != pipeline) {
                currPipeline = pipeline
                pipeline.glState.activate(currGlState, prevGlState, false)
            }

            var prevScissor: ScissorState? = null
            if (scissor != null) {
                prevScissor = ScissorState.active()
                scissor?.activate()
            }

            pipeline.draw(builtBuffer)

            prevScissor?.activate()
            //#endif
        }

        override fun close() {
            //#if MC >= 1.21.5 && !STANDALONE
            //$$ mc.close()
            //#else
            pipeline.unbind()
            //#endif

            //#if MC >= 1.21.6 && !STANDALONE
            //$$ tmpBuffers.forEach { it.close() }
            //#endif
        }
    }
}
