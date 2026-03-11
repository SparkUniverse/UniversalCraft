package gg.essential.universal.render

import gg.essential.universal.vertex.UBuiltBuffer
import gg.essential.universal.vertex.UBuiltBufferInternal

//#if STANDALONE
//#else
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
//$$ import java.util.OptionalInt
//$$ import java.util.OptionalDouble
//#endif
//#endif

// Kept internal for now because I'm not yet sure how Mojang will evolve their RenderPass.
// At the moment we can't keep it open across draw calls because MC prevents uploading any new buffers while the render
// pass is active. Not sure if that's intentional.
// For older versions it would definitely be useful to issue multiple draw calls via one URenderPass object because
// we can cache the global GL state between calls and don't need to re-query and reset everything on every draw.
internal class URenderPass : AutoCloseable {
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
    }

    internal inner class DrawCallBuilderImpl(
        private val pipeline: URenderPipeline,
        private val builtBuffer: UBuiltBufferInternal,
    ) : DrawCallBuilder {
        //#if MC>=12105 && !STANDALONE
        //$$ val mc: RenderPass
        //$$ init {
            //#if MC>=12106
            //$$ val dynamicUniforms = RenderSystem.getDynamicUniforms().write(
            //$$     RenderSystem.getModelViewMatrix(),
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
        //$$     val builtBuffer = builtBuffer.mc
        //$$     val vertexBuffer = pipeline.format.uploadImmediateVertexBuffer(builtBuffer.buffer)
        //$$     val sortedBuffer = builtBuffer.sortedBuffer
        //$$     val (indexBuffer, indexType) = if (sortedBuffer != null) {
        //$$         pipeline.format.uploadImmediateIndexBuffer(sortedBuffer) to builtBuffer.drawParameters.indexType()
        //$$     } else {
        //$$         val shapeIndexBuffer = RenderSystem.getSequentialBuffer(builtBuffer.drawParameters.mode())
        //$$         shapeIndexBuffer.getIndexBuffer(builtBuffer.drawParameters.indexCount()) to shapeIndexBuffer.indexType
        //$$     }
        //$$     mc = MinecraftClient.getInstance().framebuffer.let { fb ->
        //$$         RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    //#if MC>=12106
                    //$$ { "Immediate draw for $pipeline" },
                    //$$ RenderSystem.outputColorTextureOverride ?: fb.colorAttachmentView!!,
                    //$$ OptionalInt.empty(),
                    //$$ RenderSystem.outputDepthTextureOverride ?: fb.depthAttachmentView,
                    //$$ OptionalDouble.empty(),
                    //#else
                    //$$ fb.colorAttachment!!, OptionalInt.empty(), fb.depthAttachment, OptionalDouble.empty()
                    //#endif
        //$$         )
        //$$     }
        //$$     mc.setVertexBuffer(0, vertexBuffer)
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

        //#if MC>=12106 && !STANDALONE
        //$$ private val tmpBuffers = mutableListOf<GpuBuffer>()
        //#endif

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

        override fun texture(name: String, textureGlId: Int): DrawCallBuilder = apply {
            //#if MC>=12105 && !STANDALONE
            //#if MC>=12106
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

        override fun texture(index: Int, textureGlId: Int): DrawCallBuilder = apply {
            //#if MC>=12105 && !STANDALONE
            //$$ texture(pipeline.mcRenderPipeline.samplers[index], textureGlId)
            //#else
            pipeline.texture(index, textureGlId)
            //#endif
        }

        fun submit() {
            //#if MC>=12105 && !STANDALONE
            //$$ val scissor = scissor ?: ScissorState.active()
            //$$ if (scissor.enabled) {
            //$$     mc.enableScissor(scissor.x, scissor.y, scissor.width, scissor.height)
            //$$ } else {
            //$$     mc.disableScissor()
            //$$ }
            //$$
            //$$ pipeline.draw(mc, builtBuffer.mc)
            //$$
            //$$ mc.close()
            //#if MC>=12106
            //$$ tmpBuffers.forEach { it.close() }
            //#endif
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

            pipeline.unbind()
            //#endif
        }
    }
}
