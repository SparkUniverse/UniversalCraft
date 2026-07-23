package gg.essential.universal.render

import gg.essential.universal.UGraphics
import java.nio.ByteBuffer
import kotlin.math.max
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL21
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL31

//#if STANDALONE
//$$ import org.lwjgl.opengl.GL20C
//#else

//#if MC >= 26.2
//$$ import com.mojang.blaze3d.opengl.GlConst
//$$ import com.mojang.blaze3d.systems.CommandEncoder
//$$ import com.mojang.blaze3d.systems.CommandEncoderBackend
//$$ import com.mojang.blaze3d.systems.GpuDevice
//$$ import com.mojang.blaze3d.systems.GpuDeviceBackend
//$$ import com.mojang.blaze3d.vulkan.VulkanBackend
//$$ import com.mojang.blaze3d.vulkan.VulkanCommandEncoder
//$$ import com.mojang.blaze3d.vulkan.VulkanDevice
//$$ import com.mojang.blaze3d.vulkan.VulkanGpuBuffer
//$$ import com.mojang.blaze3d.vulkan.VulkanGpuTexture
//$$ import org.lwjgl.system.MemoryStack
//$$ import org.lwjgl.vulkan.VK10
//$$ import org.lwjgl.vulkan.VK12
//$$ import org.lwjgl.vulkan.VkBufferImageCopy
//$$ import org.lwjgl.vulkan.VkCommandBuffer
//$$ import java.lang.invoke.MethodHandles
//#endif

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
    //#if MC >= 1.16
    //$$ val OpenGL31 by lazy { org.lwjgl.opengl.GL.getCapabilities().OpenGL31 }
    //$$ val OpenGL30 by lazy { org.lwjgl.opengl.GL.getCapabilities().OpenGL30 }
    //#else
    val OpenGL31 by lazy { org.lwjgl.opengl.GLContext.getCapabilities().OpenGL31 }
    val OpenGL30 by lazy { org.lwjgl.opengl.GLContext.getCapabilities().OpenGL30 }
    //#endif

    private val tmpVao by lazy { GL30.glGenVertexArrays() }

    override fun createTexture(
        label: String?,
        usage: UGpuTexture.Usage,
        format: UGpuFormat,
        width: Int,
        height: Int,
        mipLevels: Int
    ): UGpuTexture {
        require(usage.bits != 0) { "At least one usage bit must be set" }
        requireValidTextureSize(width, height,  mipLevels)

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
            //#if MC >= 1.21.5 && MC < 26.2
            //$$ .also { FboCacheFix.track(it.impl.mc) }
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
            //#if MC >= 1.21.6 && MC < 26.2
            //$$ .also { FboCacheFix.track(it.impl.mc) }
            //#endif
    }

    internal fun requireValidTextureSize(width: Int, height: Int, mipLevels: Int) {
        require(width > 0) { "Width must be positive but was $width" }
        require(height > 0) { "Height must be positive but was $height" }
        require(mipLevels > 0) { "Mip levels must be positive but was $mipLevels"}
        fun log2(x: Int) = 31 - x.countLeadingZeroBits()
        val maxMipLevels = log2(max(width, height)) + 1
        require(mipLevels <= maxMipLevels) { "Texture of size ${width}x${height} supports at most $maxMipLevels but $mipLevels were requested" }
    }

    override fun createBuffer(usage: UGpuBuffer.Usage, size: Long): UGpuBuffer {
        require(size <= Int.MAX_VALUE) { "Sizes greater than Int.MAX_VALUE are not supported" } // due to MC 1.21.6-9
        //#if MC >= 1.21.11 && !STANDALONE
        //$$ return UGpuBufferImpl(RenderSystem.getDevice().createBuffer(null, usage.bits, size))
        //#elseif MC >= 1.21.6 && !STANDALONE
        //$$ return UGpuBufferImpl(RenderSystem.getDevice().createBuffer(null, usage.bits, size.toInt()))
        //#else
        val buffer = UGpuBufferImpl(usage, size, GL15.glGenBuffers())
        withBufferBound(buffer) { bindTarget ->
            GL15.glBufferData(bindTarget, size, usage.glUsageHint)
        }
        return buffer
        //#endif
    }

    override fun createBuffer(usage: UGpuBuffer.Usage, buffer: ByteBuffer): UGpuBuffer {
        //#if MC >= 1.21.6 && !STANDALONE
        //$$ return UGpuBufferImpl(RenderSystem.getDevice().createBuffer(null, usage.bits, buffer))
        //#else
        val gpuBuffer = UGpuBufferImpl(usage, buffer.remaining().toLong(), GL15.glGenBuffers())
        withBufferBound(gpuBuffer) { bindTarget ->
            GL15.glBufferData(bindTarget, buffer, usage.glUsageHint)
        }
        return gpuBuffer
        //#endif
    }

    override fun mapBuffer(gpuBufferSlice: UGpuBufferSlice, read: Boolean, write: Boolean): UGpuDevice.MappedBuffer {
        check(!gpuBufferSlice.buffer.isClosed) { "Buffer is closed" }
        if (read) require(UGpuBuffer.Usage.MAP_READ in gpuBufferSlice.buffer.impl.usage) { "Buffer must have MAP_READ usage flag" }
        if (write) require(UGpuBuffer.Usage.MAP_WRITE in gpuBufferSlice.buffer.impl.usage) { "Buffer must have MAP_WRITE usage flag" }

        //#if MC >= 1.21.6 && !STANDALONE
        //#if MC >= 26.2
        //$$ val view = gpuBufferSlice.mc.map(read, write)
        //#else
        //$$ val view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(gpuBufferSlice.mc, read, write)
        //#endif
        //$$ return object : UGpuDevice.MappedBuffer {
        //$$     override val data: ByteBuffer = view.data()
        //$$     override fun close() = view.close()
        //$$ }
        //#else
        val access = when {
            read && write -> GL15.GL_READ_WRITE
            read -> GL15.GL_READ_ONLY
            write -> GL15.GL_WRITE_ONLY
            else -> throw IllegalArgumentException("At least one of `read` or `write` must be true")
        }
        val buffer = withBufferBound(gpuBufferSlice.buffer.impl) { bindTarget ->
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // old_buffer may be null according to javadocs
            GL15.glMapBuffer(bindTarget, access, gpuBufferSlice.size, null)
        } ?: throw IllegalStateException("Failed to map buffer")
        gpuBufferSlice.buffer.impl.mappedCount++
        return object : UGpuDevice.MappedBuffer {
            override val data: ByteBuffer = buffer

            var closed = false

            override fun close() {
                if (closed) return
                closed = true
                gpuBufferSlice.buffer.impl.mappedCount--
                withBufferBound(gpuBufferSlice.buffer.impl) { bindTarget ->
                    GL15.glUnmapBuffer(bindTarget)
                }
            }
        }
        //#endif
    }

    //#if MC < 1.21.6 || STANDALONE
    private inline fun <T> withBufferBound(gpuBuffer: UGpuBufferImpl, block: (bindTarget: Int) -> T): T {
        val bindTarget = gpuBuffer.usage.bindTarget
        val bindTargetBinding = when (bindTarget) {
            GL15.GL_ARRAY_BUFFER -> GL15.GL_ARRAY_BUFFER_BINDING
            GL15.GL_ELEMENT_ARRAY_BUFFER -> GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING
            GL31.GL_UNIFORM_BUFFER -> GL31.GL_UNIFORM_BUFFER_BINDING
            GL31.GL_COPY_WRITE_BUFFER -> 0x8F37 // GL31.GL_COPY_WRITE_BUFFER_BINDING (missing from LWJGL3 for unknown reason?)
            else -> throw AssertionError("Unexpected bind target $bindTarget")
        }
        val prevVao: Int
        if (bindTarget == GL15.GL_ELEMENT_ARRAY_BUFFER && OpenGL30) {
            // Requires VAO, see https://stackoverflow.com/questions/20391921/
            prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
            GL30.glBindVertexArray(tmpVao)
        } else {
            prevVao = 0
        }
        val prevBinding = GL11.glGetInteger(bindTargetBinding)
        GL15.glBindBuffer(bindTarget, gpuBuffer.glId)
        try {
            return block(bindTarget)
        } finally {
            GL15.glBindBuffer(bindTarget, prevBinding)
            if (bindTarget == GL15.GL_ELEMENT_ARRAY_BUFFER && OpenGL30) {
                GL30.glBindVertexArray(prevVao)
            }
        }
    }

    // We'll try to pick an appropriate binding target for our buffers because
    // > Once created, a named buffer object may be re-bound to any target as often as needed. However, the GL
    // > implementation may make choices about how to optimize the storage of a buffer object based on its initial
    // > binding target.
    private val UGpuBuffer.Usage.bindTarget: Int
        get() = when {
            UGpuBuffer.Usage.VERTEX in this -> GL15.GL_ARRAY_BUFFER
            UGpuBuffer.Usage.INDEX in this -> GL15.GL_ELEMENT_ARRAY_BUFFER
            UGpuBuffer.Usage.UNIFORM in this -> if (OpenGL31) GL31.GL_UNIFORM_BUFFER else GL15.GL_ARRAY_BUFFER
            else -> if (OpenGL31) GL31.GL_COPY_WRITE_BUFFER else GL15.GL_ARRAY_BUFFER
        }

    private val UGpuBuffer.Usage.glUsageHint: Int
        get() = when {
            UGpuBuffer.Usage.MAP_WRITE in this -> if (UGpuBuffer.Usage.HINT_CLIENT_STORAGE in this) GL15.GL_STREAM_DRAW else GL15.GL_STATIC_DRAW
            UGpuBuffer.Usage.MAP_READ in this -> if (UGpuBuffer.Usage.HINT_CLIENT_STORAGE in this) GL15.GL_STREAM_READ else GL15.GL_STATIC_READ
            else -> GL15.GL_STATIC_DRAW
        }
    //#endif

    override fun copyBufferToTexture(
        source: UGpuBufferSlice,
        sourceX: Int,
        sourceY: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        destination: UGpuTexture,
        destinationX: Int,
        destinationY: Int,
        copyWidth: Int,
        copyHeight: Int,
        mipLevel: Int,
        arrayLayer: Int,
    ) {
        val destinationWidth = destination.width shr mipLevel
        val destinationHeight = destination.width shr mipLevel
        require(!source.buffer.isClosed) { "Source buffer is closed" }
        require(!destination.isClosed) { "Destination texture is closed" }
        require(UGpuBuffer.Usage.COPY_SRC in source.buffer.impl.usage) { "Source buffer must have COPY_SRC usage flag" }
        require(UGpuTexture.Usage.COPY_DST in destination.impl.usage) { "Destination texture must have COPY_DST usage flag" }
        require(copyWidth >= 0) { "copyWidth must be positive but was $copyWidth" }
        require(copyHeight >= 0) { "copyHeight must be positive but was $copyHeight" }
        require(sourceX >= 0) { "sourceX must be positive but was $sourceX" }
        require(sourceY >= 0) { "sourceY must be positive but was $sourceY" }
        require(sourceX + copyWidth <= sourceWidth) { "Tried to copy $copyWidth from $sourceX but source is only $sourceWidth wide" }
        require(sourceY + copyHeight <= sourceHeight) { "Tried to copy $copyHeight from $sourceY but source is only $sourceHeight high" }
        require(destinationX >= 0) { "destinationX must be positive but was $destinationX" }
        require(destinationY >= 0) { "destinationY must be positive but was $destinationY" }
        require(destinationX + copyWidth <= destinationWidth) { "Tried to copy $copyWidth to $destinationX but destination is only $destinationWidth wide" }
        require(destinationY + copyHeight <= destinationHeight) { "Tried to copy $copyHeight to $destinationY but destination is only $destinationHeight high" }
        require(mipLevel >= 0) { "mipLevel must not be negative" }
        require(mipLevel < destination.mipLevels) { "mipLevel is $mipLevel but texture only has $mipLevel levels" }
        require(arrayLayer == 0) { "arrayLayer other than 0 is not yet supported" }

        val format = destination.impl.format
        require(source.offset % format.componentByteSize == 0L) { "Source buffer offset must be ${format.componentByteSize}-aligned but was ${source.offset}" }
        val texelByteSize = format.componentCount * format.componentByteSize
        val texelCopyRange = sourceX + sourceY * sourceWidth.toLong() until(sourceX + copyWidth) + (sourceY + copyHeight - 1) * sourceWidth.toLong()
        val bytesCopyRange = texelCopyRange.first * texelByteSize .. texelCopyRange.last * texelByteSize
        require(bytesCopyRange.last < source.size) { "Copy range $bytesCopyRange is out of bounds for $source"}

        if (isVulkan()) {
            //#if MC >= 26.2 && !STANDALONE
            //$$ MemoryStack.stackPush().use { stack ->
            //$$     VK12.vkCmdCopyBufferToImage(
            //$$         vkCommandBuffer(),
            //$$         (source.buffer.impl.mc as VulkanGpuBuffer).vkBuffer(),
            //$$         (destination.impl.mc as VulkanGpuTexture).vkImage(),
            //$$         1,
            //$$         VkBufferImageCopy.calloc(1, stack).also { region ->
            //$$             region.bufferOffset(source.offset + bytesCopyRange.first)
            //$$             region.bufferRowLength(sourceWidth)
            //$$             region.bufferImageHeight(sourceHeight)
            //$$             region.imageSubresource().let { image ->
            //$$                 image.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
            //$$                 image.mipLevel(mipLevel)
            //$$                 image.baseArrayLayer(arrayLayer)
            //$$                 image.layerCount(1)
            //$$             }
            //$$             region.imageOffset().set(destinationX, destinationY, 0)
            //$$             region.imageExtent().set(copyWidth, copyHeight, 1)
            //$$         },
            //$$     )
            //$$     vkMemoryBarrier(stack)
            //$$ }
            //#endif
        } else {
            UGraphics.configureTexture(destination.impl.glId) {
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, sourceWidth)
                GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, sourceHeight)
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, format.componentByteSize)

                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, source.buffer.impl.glId)

                GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D,
                    mipLevel,
                    destinationX,
                    destinationY,
                    copyWidth,
                    copyHeight,
                    //#if MC >= 26.2 && !STANDALONE
                    //$$ GlConst.toGlExternalId(destination.impl.format.mc),
                    //$$ GlConst.toGlType(destination.impl.format.mc),
                    //#else
                    destination.impl.format.format,
                    destination.impl.format.type,
                    //#endif
                    source.offset + bytesCopyRange.first,
                )

                GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0)

                // We restore these to defaults as well as to not disturb third-party mods which assume defaults
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0)
                GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4)
            }
        }
    }

    override fun createFence(): UGpuFence {
        return UGpuFenceImpl()
    }

    //#if MC >= 26.2 && !STANDALONE
    //$$ private val lookup = MethodHandles.lookup()
    //$$ private val GpuDevice_backend by lazy {
    //$$     val field = GpuDevice::class.java.getDeclaredField("backend")
    //$$     field.isAccessible = true
    //$$     lookup.unreflectGetter(field)
    //$$ }
    //$$
    //$$ fun isVulkan() =
    //$$     RenderSystem.getDevice()
    //$$         .let { GpuDevice_backend.invoke(it) as GpuDeviceBackend }
    //$$         .let { it is VulkanDevice }
    //$$
    //$$ private val CommandEncoder_backend by lazy {
    //$$     val field = CommandEncoder::class.java.getDeclaredField("backend")
    //$$     field.isAccessible = true
    //$$     lookup.unreflectGetter(field)
    //$$ }
    //$$
    //$$ private val VulkanCommandEncoder_commandBuffer by lazy {
    //$$     val method = VulkanCommandEncoder::class.java.getDeclaredMethod("commandBuffer")
    //$$     method.isAccessible = true
    //$$     lookup.unreflect(method)
    //$$ }
    //$$ private fun vkCommandBuffer(): VkCommandBuffer =
    //$$     RenderSystem.getDevice()
    //$$         .createCommandEncoder()
    //$$         .let { CommandEncoder_backend.invoke(it) as CommandEncoderBackend }
    //$$         .let { VulkanCommandEncoder_commandBuffer.invoke(it) as VkCommandBuffer }
    //$$
    //$$ private val VulkanCommandEncoder_memoryBarrier by lazy {
    //$$     val method = VulkanCommandEncoder::class.java.getDeclaredMethod("memoryBarrier", MemoryStack::class.java)
    //$$     method.isAccessible = true
    //$$     lookup.unreflect(method)
    //$$ }
    //$$ private fun vkMemoryBarrier(stack: MemoryStack): Unit =
    //$$     RenderSystem.getDevice()
    //$$         .createCommandEncoder()
    //$$         .let { CommandEncoder_backend.invoke(it) as CommandEncoderBackend }
    //$$         .let { VulkanCommandEncoder_memoryBarrier.invoke(it, stack); Unit }
    //#else
    fun isVulkan() = false
    //#endif
}
