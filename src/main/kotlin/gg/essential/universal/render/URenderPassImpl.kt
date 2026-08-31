package gg.essential.universal.render

import gg.essential.universal.UGraphics
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.lang.ClassCastException
import java.lang.IllegalStateException

//#if MC>=11700
//$$ import org.lwjgl.opengl.GL30.glBindFramebuffer
//$$ import org.lwjgl.opengl.GL30.glFramebufferTexture2D
//$$ import org.lwjgl.opengl.GL30.glGenFramebuffers
//#elseif MC>=11400
//$$ import com.mojang.blaze3d.platform.GlStateManager.bindFramebuffer as glBindFramebuffer
//$$ import com.mojang.blaze3d.platform.GlStateManager.framebufferTexture2D as glFramebufferTexture2D
//$$ import com.mojang.blaze3d.platform.GlStateManager.genFramebuffers as glGenFramebuffers
//#else
import net.minecraft.client.renderer.OpenGlHelper.glBindFramebuffer
import net.minecraft.client.renderer.OpenGlHelper.glFramebufferTexture2D
import net.minecraft.client.renderer.OpenGlHelper.glGenFramebuffers
//#endif

//#if STANDALONE
//$$ import gg.essential.universal.standalone.render.VertexFormat
//#else
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.renderer.vertex.VertexFormatElement

//#if MC >= 1.21.6
//$$ import com.mojang.blaze3d.buffers.GpuBuffer
//$$ import com.mojang.blaze3d.buffers.GpuBufferSlice
//$$ import net.minecraft.client.gl.DynamicUniforms
//$$ import org.lwjgl.system.MemoryStack
//#endif

//#if MC >= 1.21.5
//$$ import com.mojang.blaze3d.systems.RenderPass
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import net.minecraft.client.texture.GlTexture
//#if MC >= 26.2
//$$ import java.util.Optional
//#else
//$$ import java.util.OptionalInt
//#endif
//$$ import java.util.OptionalDouble
//#endif

//#if MC == 1.21.5
//$$ import net.minecraft.client.gl.GlUniform
//$$ import net.minecraft.client.gl.ShaderProgram
//#endif

//#if MC >= 1.17
//#if MC < 1.21.5
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#endif
//#else
import org.lwjgl.opengl.GL13
import net.minecraft.client.renderer.GlStateManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
//#endif
//#endif

internal class URenderPassImpl(val descriptor: URenderPassDescriptor) : URenderPass {
    init {
        if (active) throw IllegalStateException("Must close previous RenderPass before opening new one.")
        active = true

        check(descriptor.colorAttachments.isNotEmpty()) { "Zero color attachments are not yet supported." }
    }

    private val outputSize = descriptor.outputSize()
    private val renderArea = descriptor.renderArea ?: outputSize

    //#if MC >= 1.21.5 && !STANDALONE
    //$$ private val mc: RenderPass
    //#else
    private val prevVao: Int
    private val prevViewport = ViewportState.active()
    private var currViewport = prevViewport
    private val prevDrawFrameBufferBinding = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
    private val prevReadFrameBufferBinding = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
    private val prevGlState = ManagedGlState.active()
    private val currGlState = ManagedGlState(prevGlState)
    private var prevScissor: ScissorState = ScissorState.active()
    private var currScissor: ScissorState = prevScissor
    private var currPipeline: URenderPipeline? = null
    //#endif

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ private val tmpBuffers = mutableListOf<GpuBuffer>()
    //#endif

    init {
        // Cleanup global MC state
        //#if MC >= 1.17 && MC < 1.21.5
        //$$ net.minecraft.client.render.BufferRenderer.unbindAll()
        //#endif

        // Clear texture before render pass
        //#if MC >= 26.2 && !STANDALONE
        //$$ // Done by `createRenderPass` below
        //#else
        for ((textureView, clearColor) in descriptor.colorAttachments) {
            if (clearColor != null) {
                UGpuDeviceImpl.clearColor(textureView.texture, renderArea, clearColor)
            }
        }
        descriptor.depthAttachment?.let { (textureView, clearDepth) ->
            if (clearDepth != null) {
                UGpuDeviceImpl.clearDepth(textureView.texture, renderArea, clearDepth)
            }
        }
        //#endif

        // Setup render pass
        //#if MC >= 1.21.5 && !STANDALONE
        //$$ mc = RenderSystem.getDevice()
        //$$     .createCommandEncoder()
        //$$     .createRenderPass(
                //#if MC >= 1.21.6
                //$$ descriptor.label,
                //#endif
                //#if MC >= 1.21.6
                //$$ descriptor.colorAttachments.single().first.impl.mc,
                //#else
                //$$ descriptor.colorAttachments.single().first.texture.impl.mc,
                //#endif
                //#if MC >= 26.2
                //$$ descriptor.colorAttachments.single().second?.let { Optional.of(org.joml.Vector4f(it.red, it.green, it.blue, it.alpha)) } ?: Optional.empty(),
                //#else
                //$$ OptionalInt.empty(),
                //#endif
                //#if MC >= 1.21.6
                //$$ descriptor.depthAttachment?.first?.impl?.mc,
                //#else
                //$$ descriptor.depthAttachment?.first?.texture?.impl?.mc,
                //#endif
                //#if MC >= 26.2
                //$$ descriptor.depthAttachment?.second?.let { OptionalDouble.of(it) } ?: OptionalDouble.empty(),
                //#else
                //$$ OptionalDouble.empty(),
                //#endif
        //$$     )
        //#else
        glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer)
        for ((index, attachment) in descriptor.colorAttachments.withIndex()) {
            val (textureView, _) = attachment
            glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index, GL11.GL_TEXTURE_2D, textureView.texture.impl.glId, textureView.baseMipLevel)
        }
        descriptor.depthAttachment?.let { (textureView, _) ->
            glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, textureView.texture.impl.glId, textureView.baseMipLevel)
        }
        // Prior to GL 4.1, read and draw buffers (both!) must be explicitly set to NONE if the framebuffer does not
        // have a color attachment, otherwise it will not be considered complete and operations on it may error.
        GL11.glDrawBuffer(if (descriptor.colorAttachments.isEmpty()) GL11.GL_NONE else GL30.GL_COLOR_ATTACHMENT0)
        GL11.glReadBuffer(if (descriptor.colorAttachments.isEmpty()) GL11.GL_NONE else GL30.GL_COLOR_ATTACHMENT0)

        val viewport = ViewportState(outputSize.x, outputSize.y, outputSize.width, outputSize.height)
        if (viewport != currViewport) {
            viewport.activate()
            currViewport = viewport
        }

        if (UGpuDeviceImpl.OpenGL30) {
            prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
            GL30.glBindVertexArray(vao)
        } else {
            prevVao = 0
        }
        //#endif
    }

    override fun close() {
        //#if MC >= 1.21.5 && !STANDALONE
        //$$ mc.close()
        //#else
        currPipeline?.unbind()
        if (prevScissor != currScissor) prevScissor.activate()
        prevGlState.activate(currGlState, prevGlState, true)
        for (index in descriptor.colorAttachments.indices) {
            glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + index, GL11.GL_TEXTURE_2D, 0, 0)
        }
        descriptor.depthAttachment?.let { _ ->
            glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, 0, 0)
        }
        glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDrawFrameBufferBinding)
        glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevReadFrameBufferBinding)
        if (prevViewport != currViewport) prevViewport.activate()
        if (UGpuDeviceImpl.OpenGL30) {
            GL30.glBindVertexArray(prevVao)
        }
        //#endif

        //#if MC >= 1.21.6 && !STANDALONE
        //$$ tmpBuffers.forEach { it.close() }
        //#endif

        active = false
    }

    private var scissor: ScissorState = ScissorState(true, renderArea.x, renderArea.y, renderArea.width, renderArea.height)
    private var pipeline: URenderPipeline? = null
    private var vertexBuffer: UGpuBufferSlice? = null
    private var indexBuffer: Pair<UGpuBuffer, URenderPass.IndexType>? = null
    private val textures = mutableMapOf<String, Pair<UGpuTextureView, UGpuSampler>>()
    private val uniforms = mutableMapOf<String, Any>()
    private var projectionMatrix = IDENTITY_MAT4
    private var modelViewMatrix = IDENTITY_MAT4

    private var projectionUboDirty = true
    private var dynamicTransformsUboDirty = true

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        require(width > 0) { "Scissor width must be positive but was $width" }
        require(height > 0) { "Scissor height must be positive but was $height" }
        val scissor = ScissorState(true, x, y, width, height)
        val boundedScissor = scissor.coerceIn(renderArea.x, renderArea.y, renderArea.width, renderArea.height)
        require(scissor == boundedScissor) { "$scissor is out of bounds for render area $renderArea" }
        this.scissor = scissor
    }

    override fun pipeline(pipeline: URenderPipeline) {
        if (pipeline.wantsDepthTexture) {
            require(descriptor.depthAttachment != null) { "Pipeline requires depth attachment but this render pass does not have one." }
        }
        this.pipeline = pipeline
    }

    override fun vertexBuffer(slot: Int, buffer: UGpuBufferSlice) {
        require(slot == 0) { "Slots other than 0 are not yet supported" }
        require(buffer.offset == 0L) { "Buffer offsets are not yet supported" }
        this.vertexBuffer = buffer
    }

    override fun indexBuffer(buffer: UGpuBuffer, type: URenderPass.IndexType) {
        this.indexBuffer = Pair(buffer, type)
    }

    override fun texture(name: String, textureView: UGpuTextureView, sampler: UGpuSampler) {
        require(textureView.baseMipLevel == 0) { "baseMipLevel other than 0 is not yet supported" }
        textures[name] = Pair(textureView, sampler)
    }

    override fun uniform(name: String, values: FloatArray) {
        uniforms[name] = values
    }

    override fun uniform(name: String, values: IntArray) {
        uniforms[name] = values
    }

    override fun uniform(name: String, buffer: UGpuBufferSlice) {
        //#if MC >= 1.21.6 && !STANDALONE
        //$$ uniforms[name] = buffer.mc
        //#else
        throw UnsupportedOperationException("UBOs are currently only supported on 1.21.6+")
        //#endif
    }

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ private fun allocUBO(name: String, values: FloatArray) =
    //$$     MemoryStack.stackPush().use { stack ->
    //$$         val byteBuf = stack.malloc(values.size * Float.SIZE_BYTES)
    //$$         values.forEach { byteBuf.putFloat(it) }
    //$$         byteBuf.flip()
    //$$         RenderSystem.getDevice().createBuffer({ "$name UBO" }, GpuBuffer.USAGE_UNIFORM, byteBuf)
    //$$     }.also { tmpBuffers.add(it) }
    //$$ private fun allocUBO(name: String, values: IntArray) =
    //$$     MemoryStack.stackPush().use { stack ->
    //$$         val byteBuf = stack.malloc(values.size * Int.SIZE_BYTES)
    //$$         values.forEach { byteBuf.putInt(it) }
    //$$         byteBuf.flip()
    //$$         RenderSystem.getDevice().createBuffer({ "$name UBO" }, GpuBuffer.USAGE_UNIFORM, byteBuf)
    //$$     }.also { tmpBuffers.add(it) }
    //#endif

    override fun projectionMatrix(matrix: FloatArray) {
        require(matrix.size == 16) { "Matrix must have 16 entries (4x4)"}
        if (matrix.contentEquals(projectionMatrix)) return
        projectionMatrix = matrix.copyOf()
        projectionUboDirty = true
    }

    override fun modelViewMatrix(matrix: FloatArray) {
        require(matrix.size == 16) { "Matrix must have 16 entries (4x4)"}
        if (matrix.contentEquals(modelViewMatrix)) return
        modelViewMatrix = matrix.copyOf()
        dynamicTransformsUboDirty = true
    }

    override fun draw(
        vertexCount: Int,
        instanceCount: Int,
        firstVertex: Int,
        firstInstance: Int,
    ) {
        require(instanceCount == 1) { "instanceCount is not yet supported" }
        require(firstInstance == 0) { "firstInstance is not yet supported" }

        val pipeline = pipeline ?: throw IllegalStateException("No pipeline has been set")
        val vertexBuffer = vertexBuffer ?: throw IllegalStateException("No vertex buffer has been set")

        check(!vertexBuffer.buffer.isClosed) { "Vertex buffer has already been closed." }

        //#if MC >= 1.21.5 && !STANDALONE
        //$$ drawCall(pipeline) {
            //#if MC >= 26.2
            //$$ mc.setVertexBuffer(0, vertexBuffer.mc)
            //#else
            //$$ mc.setVertexBuffer(0, vertexBuffer.buffer.impl.mc)
            //#endif
            //#if MC >= 26.2
            //$$ mc.draw(vertexCount, instanceCount, firstVertex, firstInstance)
            //#else
            //$$ mc.draw(firstVertex, vertexCount)
            //#endif
        //$$ }
        //#else
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer.buffer.impl.glId)
        drawCall(pipeline) {
            GL11.glDrawArrays(pipeline.drawMode.actualGlMode, firstVertex, vertexCount)
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        //#endif
    }

    override fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int,
        vertexOffset: Int,
        firstInstance: Int,
    ) {
        require(instanceCount == 1) { "instanceCount is not yet supported" }
        require(vertexOffset == 0) { "vertexOffset is not yet supported" }
        require(firstInstance == 0) { "firstInstance is not yet supported" }

        val pipeline = pipeline ?: throw IllegalStateException("No pipeline has been set")
        val vertexBuffer = vertexBuffer ?: throw IllegalStateException("No vertex buffer has been set")
        val (indexBuffer, indexType) = indexBuffer ?: throw IllegalStateException("No index buffer has been set")

        check(!vertexBuffer.buffer.isClosed) { "Vertex buffer has already been closed." }
        check(!indexBuffer.isClosed) { "Index buffer has already been closed." }
        require((firstIndex + indexCount) * indexType.bytes <= indexBuffer.size) { "Tried to draw $indexCount $indexType indices starting from $firstIndex but buffer is only ${indexBuffer.size} bytes in size." }

        //#if MC >= 1.21.5 && !STANDALONE
        //$$ drawCall(pipeline) {
            //#if MC >= 26.2
            //$$ mc.setVertexBuffer(0, vertexBuffer.mc)
            //#else
            //$$ mc.setVertexBuffer(0, vertexBuffer.buffer.impl.mc)
            //#endif
        //$$     mc.setIndexBuffer(indexBuffer.impl.mc, when (indexType) {
                //#if MC >= 26.2
                //$$ URenderPass.IndexType.SHORT -> com.mojang.blaze3d.IndexType.SHORT
                //$$ URenderPass.IndexType.INT -> com.mojang.blaze3d.IndexType.INT
                //#else
                //$$ URenderPass.IndexType.SHORT -> VertexFormat.IndexType.SHORT
                //$$ URenderPass.IndexType.INT -> VertexFormat.IndexType.INT
                //#endif
        //$$     })
            //#if MC >= 26.2
            //$$ mc.drawIndexed(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance)
            //#elseif MC >= 1.21.6
            //$$ mc.drawIndexed(vertexOffset, firstIndex, indexCount, instanceCount)
            //#else
            //$$ mc.drawIndexed(firstIndex, indexCount)
            //#endif
        //$$ }
        //#else
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.impl.glId)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer.buffer.impl.glId)
        drawCall(pipeline) {
            GL11.glDrawElements(
                pipeline.drawMode.actualGlMode,
                indexCount,
                indexType.glId,
                firstIndex.toLong() * indexType.bytes,
            )
        }
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        //#endif
    }

    private fun drawCall(pipeline: URenderPipeline, draw: () -> Unit) {
        //#if MC >= 1.21.5 && !STANDALONE
        //$$ mc.setPipeline(pipeline.compiled() ?: return)
        //#else
        if (currPipeline != pipeline) {
            currPipeline?.unbind()
            pipeline.bind()
            pipeline.glState.activate(currGlState, prevGlState, false)
            currPipeline = pipeline
        }
        //#endif

        //#if MC >= 1.21.5 && !STANDALONE
        //$$ mc.enableScissor(scissor.x, scissor.y, scissor.width, scissor.height)
        //#else
        if (currScissor != scissor) {
            scissor.activate()
            currScissor = scissor
        }
        //#endif

        for ((name, textureViewAndSampler) in textures) {
            val (textureView, sampler) = textureViewAndSampler
            check(!textureView.isClosed) { "Texture view for `$name` is closed" }
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

        //#if MC >= 1.21.6 && !STANDALONE
        //$$ if (projectionUboDirty) {
        //$$     mc.setUniform("Projection", allocUBO("Projection", projectionMatrix))
        //$$     projectionUboDirty = false
        //$$ }
        //$$ if (dynamicTransformsUboDirty) {
        //$$     mc.setUniform("DynamicTransforms", MemoryStack.stackPush().use { stack ->
        //$$         val byteBuf = stack.malloc(DynamicUniforms.SIZE)
        //$$         DynamicUniforms.UniformValue(
        //$$             modelViewMatrix.let { org.joml.Matrix4f(
        //$$                 it[ 0], it[ 1], it[ 2], it[ 3],
        //$$                 it[ 4], it[ 5], it[ 6], it[ 7],
        //$$                 it[ 8], it[ 9], it[10], it[11],
        //$$                 it[12], it[13], it[14], it[15],
        //$$             ) },
        //$$             org.joml.Vector4f(1f, 1f, 1f, 1f),
        //$$             org.joml.Vector3f(),
        //$$             org.joml.Matrix4f(),
                    //#if MC < 1.21.11
                    //$$ 1f,
                    //#endif
        //$$         ).write(byteBuf)
        //$$         byteBuf.flip()
        //$$         RenderSystem.getDevice().createBuffer({ "DynamicTransforms UBO" }, GpuBuffer.USAGE_UNIFORM, byteBuf)
        //$$     }.also { tmpBuffers.add(it) })
        //$$     dynamicTransformsUboDirty = false
        //$$ }
        //#elseif MC >= 1.21.5 && !STANDALONE
        //$$ mc.setUniform("ProjMat", *projectionMatrix)
        //$$ mc.setUniform("ModelViewMat", *modelViewMatrix)
        //$$ mc.setUniform("ColorModulator", 1f, 1f, 1f, 1f)
        //#elseif MC >= 1.17
        //$$ pipeline.uniform("ProjMat", *projectionMatrix)
        //$$ pipeline.uniform("ModelViewMat", *modelViewMatrix)
        //$$ pipeline.uniform("ColorModulator", 1f, 1f, 1f, 1f)
        //#else
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glMultMatrix(tmpFloatBuffer.apply { put(projectionMatrix); flip() })
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPushMatrix()
        GL11.glLoadIdentity()
        GL11.glMultMatrix(tmpFloatBuffer.apply { put(modelViewMatrix); flip() })
        //#if MC >= 1.16
        //$$ @Suppress("DEPRECATION")
        //#endif
        GlStateManager.color(1f, 1f, 1f, 1f)
        //#endif

        for ((name, value) in uniforms) {
            when (value) {
                //#if MC >= 1.21.6 && !STANDALONE
                //$$ is FloatArray -> mc.setUniform(name, allocUBO(name, value).also { uniforms[name] = it })
                //$$ is IntArray -> mc.setUniform(name, allocUBO(name, value).also { uniforms[name] = it })
                //$$ is GpuBuffer -> mc.setUniform(name, value)
                //$$ is GpuBufferSlice -> mc.setUniform(name, value)
                //#elseif MC >= 1.21.5 && !STANDALONE
                //$$ is FloatArray -> mc.setUniform(name, *value)
                //$$ is IntArray -> mc.setUniform(name, *value)
                //#else
                is FloatArray -> pipeline.uniform(name, *value)
                is IntArray -> pipeline.uniform(name, *value)
                //#endif
                else -> throw ClassCastException("Unexpected value of type ${value.javaClass}")
            }
        }

        //#if MC == 1.21.5
        //$$ // MC 1.21.5 implicitly applies global default uniforms (even overwriting manually specified ones).
        //$$ // To prevent that, we set all uniform fields of `ShaderProgram` to `null`, so that `initializeUniform`
        //$$ // effectively no-ops.
        //$$ val compiledPipeline = RenderSystem.getDevice().precompilePipeline(pipeline.mcRenderPipeline)
        //$$ val program = (compiledPipeline as net.minecraft.client.gl.CompiledShaderPipeline).program
        //$$ val orgUniformFields = UniformFields()
        //$$ orgUniformFields.copyFrom(program)
        //$$ UniformFields().copyTo(program)
        //#endif

        //#if MC >= 1.21.5 && !STANDALONE
        //$$ draw()
        //#else
        bind(pipeline.format)
        //#if MC >= 1.17 && !STANDALONE
        //$$ val shader = RenderSystem.getShader()!!
        //$$ for (i in 0 until 8) {
        //$$     shader.addSampler("Sampler$i", RenderSystem.getShaderTexture(i))
        //$$ }
        //$$ shader.bind()
        //#endif
        draw()
        //#if MC >= 1.17 && !STANDALONE
        //$$ shader.unbind()
        //#endif
        unbind(pipeline.format)
        //#endif

        //#if MC == 1.21.5
        //$$ orgUniformFields.copyTo(program)
        //#endif

        //#if MC >= 1.17
        //#else
        GL11.glMatrixMode(GL11.GL_PROJECTION)
        GL11.glPopMatrix()
        GL11.glMatrixMode(GL11.GL_MODELVIEW)
        GL11.glPopMatrix()
        //#endif
    }

    //#if MC < 1.21.5 || STANDALONE
    private fun bind(vertexFormat: VertexFormat) {
        //#if MC >= 1.17
        //$$ var index = 0
        //#endif
        //#if STANDALONE
        //$$ val stride = vertexFormat.stride * 4
        //$$ var nextOffset = 0L
        //$$ for (part in vertexFormat.parts) {
        //$$     val size = part.size
        //$$     val type = GL11.GL_FLOAT
        //$$     val normalized = false
        //$$     val offset = nextOffset
        //$$     nextOffset += part.size * 4
        //#else
        val stride = vertexFormat.nextOffset
        var nextOffset = 0L
        for (element in vertexFormat.elements) {
            val size = element.size / element.type.size
            val type = element.type.glConstant
            val normalized = when (element.usage) {
                //#if MC < 1.21
                VertexFormatElement.EnumUsage.PADDING -> continue
                //#endif
                VertexFormatElement.EnumUsage.NORMAL,
                VertexFormatElement.EnumUsage.COLOR -> true
                else -> false
            }
            val offset = nextOffset
            nextOffset += element.size
        //#endif
            //#if MC >= 1.17
            //$$ GL20.glEnableVertexAttribArray(index)
            //#if STANDALONE
            //$$ GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset)
            //#else
            //$$ if (element.type == VertexFormatElement.Type.UV && type != GL11.GL_FLOAT) {
            //$$     GL30.glVertexAttribIPointer(index, size, type, stride, offset)
            //$$ } else {
            //$$     GL20.glVertexAttribPointer(index, size, type, normalized, stride, offset)
            //$$ }
            //#endif
            //$$ index++
            //#else
            when (element.usage) {
                VertexFormatElement.EnumUsage.POSITION -> {
                    GL11.glVertexPointer(size, type, stride, offset)
                    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
                }
                VertexFormatElement.EnumUsage.NORMAL -> {
                    GL11.glNormalPointer(type, stride, offset)
                    GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY)
                }
                VertexFormatElement.EnumUsage.COLOR -> {
                    GL11.glColorPointer(size, type, stride, offset)
                    GL11.glEnableClientState(GL11.GL_COLOR_ARRAY)
                }
                VertexFormatElement.EnumUsage.UV -> {
                    GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + element.index)
                    GL11.glTexCoordPointer(size, type, stride, offset)
                    GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
                    GL13.glClientActiveTexture(GL13.GL_TEXTURE0)
                }
                VertexFormatElement.EnumUsage.GENERIC -> {
                    GL20.glEnableVertexAttribArray(element.index)
                    GL20.glVertexAttribPointer(element.index, size, type, normalized, stride, offset)
                }
                else -> {}
            }
            //#endif
        }
    }

    private fun unbind(vertexFormat: VertexFormat) {
        //#if MC >= 1.17
        //$$ var index = 0
        //#endif
        //#if STANDALONE
        //$$ for (part in vertexFormat.parts) {
        //#else
        for (element in vertexFormat.elements) {
            //#if MC < 1.21
            if (element.usage == VertexFormatElement.EnumUsage.PADDING) continue
            //#endif
        //#endif
            //#if MC >= 1.17
            //$$ GL20.glDisableVertexAttribArray(index)
            //$$ index++
            //#else
            when (element.usage) {
                VertexFormatElement.EnumUsage.POSITION -> {
                    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY)
                }
                VertexFormatElement.EnumUsage.NORMAL -> {
                    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY)
                }
                VertexFormatElement.EnumUsage.COLOR -> {
                    GL11.glDisableClientState(GL11.GL_COLOR_ARRAY)
                    //#if MC >= 1.16
                    //$$ @Suppress("DEPRECATION")
                    //$$ com.mojang.blaze3d.platform.GlStateManager.clearCurrentColor()
                    //#else
                    GlStateManager.resetColor()
                    //#endif
                }
                VertexFormatElement.EnumUsage.UV -> {
                    GL13.glClientActiveTexture(GL13.GL_TEXTURE0 + element.index)
                    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY)
                    GL13.glClientActiveTexture(GL13.GL_TEXTURE0)
                }
                VertexFormatElement.EnumUsage.GENERIC -> {
                    GL20.glDisableVertexAttribArray(element.index)
                }
                else -> {}
            }
            //#endif
        }
    }
    //#endif

    private val URenderPass.IndexType.glId: Int
        get() = when (this) {
            URenderPass.IndexType.SHORT -> GL11.GL_UNSIGNED_SHORT
            URenderPass.IndexType.INT -> GL11.GL_UNSIGNED_INT
        }

    private val URenderPass.IndexType.bytes: Int
        get() = when (this) {
            URenderPass.IndexType.SHORT -> 2
            URenderPass.IndexType.INT -> 4
        }

    @Suppress("DEPRECATION")
    private val UGraphics.DrawMode.actualGlMode: Int
        get() = when (this) {
            UGraphics.DrawMode.LINES,
            UGraphics.DrawMode.LINE_STRIP -> throw UnsupportedOperationException("Behavior of lines is inconsistent across versions")
            UGraphics.DrawMode.TRIANGLES -> GL11.GL_TRIANGLES
            UGraphics.DrawMode.TRIANGLE_STRIP -> GL11.GL_TRIANGLE_STRIP
            UGraphics.DrawMode.TRIANGLE_FAN -> GL11.GL_TRIANGLE_FAN
            UGraphics.DrawMode.QUADS -> GL11.GL_TRIANGLES
        }

    companion object {
        private val framebuffer by lazy { glGenFramebuffers() }

        private val vao by lazy { GL30.glGenVertexArrays() }

        //#if MC < 1.17
        // Note: LWJGL2 requires a buffer of 16 elements, even if the property we query only has 4
        private val tmpFloatBuffer = ByteBuffer.allocateDirect(16 * Float.SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
        //#endif

        private val IDENTITY_MAT4 = FloatArray(16).also {
            it[0] = 1f
            it[5] = 1f
            it[10] = 1f
            it[15] = 1f
        }

        private var active = false
    }
}

//#if MC == 1.21.5
//$$ private class UniformFields(
//$$     var modelViewMat: GlUniform? = null,
//$$     var projectionMat: GlUniform? = null,
//$$     var textureMat: GlUniform? = null,
//$$     var screenSize: GlUniform? = null,
//$$     var colorModulator: GlUniform? = null,
//$$     var light0Direction: GlUniform? = null,
//$$     var light1Direction: GlUniform? = null,
//$$     var glintAlpha: GlUniform? = null,
//$$     var fogStart: GlUniform? = null,
//$$     var fogEnd: GlUniform? = null,
//$$     var fogColor: GlUniform? = null,
//$$     var fogShape: GlUniform? = null,
//$$     var lineWidth: GlUniform? = null,
//$$     var gameTime: GlUniform? = null,
//$$     var modelOffset: GlUniform? = null,
//$$ ) {
//$$     fun copyFrom(program: ShaderProgram) {
//$$         modelViewMat = program.modelViewMat
//$$         projectionMat = program.projectionMat
//$$         textureMat = program.textureMat
//$$         screenSize = program.screenSize
//$$         colorModulator = program.colorModulator
//$$         light0Direction = program.light0Direction
//$$         light1Direction = program.light1Direction
//$$         glintAlpha = program.glintAlpha
//$$         fogStart = program.fogStart
//$$         fogEnd = program.fogEnd
//$$         fogColor = program.fogColor
//$$         fogShape = program.fogShape
//$$         lineWidth = program.lineWidth
//$$         gameTime = program.gameTime
//$$         modelOffset = program.modelOffset
//$$     }
//$$     fun copyTo(program: ShaderProgram) {
//$$         program.modelViewMat = modelViewMat
//$$         program.projectionMat = projectionMat
//$$         program.textureMat = textureMat
//$$         program.screenSize = screenSize
//$$         program.colorModulator = colorModulator
//$$         program.light0Direction = light0Direction
//$$         program.light1Direction = light1Direction
//$$         program.glintAlpha = glintAlpha
//$$         program.fogStart = fogStart
//$$         program.fogEnd = fogEnd
//$$         program.fogColor = fogColor
//$$         program.fogShape = fogShape
//$$         program.lineWidth = lineWidth
//$$         program.gameTime = gameTime
//$$         program.modelOffset = modelOffset
//$$     }
//$$ }
//#endif
