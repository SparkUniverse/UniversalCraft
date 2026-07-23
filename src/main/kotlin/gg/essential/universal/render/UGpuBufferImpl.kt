package gg.essential.universal.render

//#if MC >= 1.21.6 && !STANDALONE
//$$ import com.mojang.blaze3d.buffers.GpuBuffer
//$$ import java.lang.invoke.MethodHandles
//$$ import java.lang.reflect.AccessFlag
//#else
import org.lwjgl.opengl.GL15
//#endif

//#if MC >= 1.21.5 && !STANDALONE
//$$ import net.minecraft.client.gl.GlGpuBuffer
//#endif

internal class UGpuBufferImpl(
    //#if MC >= 1.21.6 && !STANDALONE
    //$$ val mc: GpuBuffer,
    //#else
    val usage: UGpuBuffer.Usage,
    override val size: Long,
    val glId: Int,
    //#endif
) : UGpuBuffer {

    //#if MC >= 1.21.6 && !STANDALONE
    //$$ val usage: UGpuBuffer.Usage get() = UGpuBuffer.Usage(mc.usage())
    //$$ override val size: Long get() = mc.size().toLong()
    //$$ override val isClosed: Boolean get() = mc.isClosed
    //$$ override fun close() = mc.close()
    //$$ val glId: Int get() = (mc as GlGpuBuffer).glId
    //#else
    override var isClosed = false
        private set

    var mappedCount = 0

    override fun close() {
        if (mappedCount > 0) throw IllegalArgumentException("Buffer is currently mapped")
        if (isClosed) return
        isClosed = true

        GL15.glDeleteBuffers(glId)
    }
    //#endif

    //#if MC == 1.21.5
    //$$ val mc: GlGpuBuffer
    //$$     get() = object : GlGpuBuffer(
    //$$         null,
    //$$         null,
    //$$         // MC doesn't use the type and usage in any of the code paths we care about, so we can just pass whatever.
    //$$         // Though BufferUsage must be one that's not `readable`, otherwise the GlGpuBuffer constructor will
    //$$         // re-allocate the buffer storage.
    //$$         com.mojang.blaze3d.buffers.BufferType.VERTICES,
    //$$         com.mojang.blaze3d.buffers.BufferUsage.DYNAMIC_WRITE,
    //$$         size.toInt(),
    //$$         glId,
    //$$     ) {
    //$$         // Mark as allocated, otherwise MC may re-allocate the data when `ensureAllocated` is called.
    //$$         // (I don't think that is ever called on any of the codepaths we care about, but may as well be safe.)
    //$$         init { hasData = true }
    //$$     }
    //#endif
}

internal val UGpuBuffer.impl: UGpuBufferImpl
    get() = when (this) { is UGpuBufferImpl -> this }

//#if MC >= 1.21.6 && !STANDALONE
//$$ internal val UGpuBufferSlice.mc: com.mojang.blaze3d.buffers.GpuBufferSlice
    //#if MC >= 1.21.11
    //$$ get() = when (buffer) { is UGpuBufferImpl -> buffer.mc.slice(offset, size) }
    //#else
    //$$ get() = when (buffer) { is UGpuBufferImpl -> buffer.mc.slice(offset.toInt(), size.toInt()) }
    //#endif
//$$
//$$ private val glGpuBufferIdFieldAccessor by lazy {
//$$     val field = GlGpuBuffer::class.java
        //#if MC >= 26.1
        //$$ .getDeclaredField("handle")
        //#else
        //$$ .declaredFields
        //$$ .first { field ->
        //$$     field.type == Int::class.java
        //$$             && AccessFlag.STATIC !in field.accessFlags()
        //$$             // looking for a `protected` field, but other mods could access-widen it
        //$$             && (AccessFlag.PROTECTED in field.accessFlags() || AccessFlag.PUBLIC in field.accessFlags())
        //$$ }
        //#endif
//$$     field.isAccessible = true
//$$     MethodHandles.lookup().unreflectGetter(field)
//$$ }
//$$ private val GlGpuBuffer.glId: Int
//$$     get() = glGpuBufferIdFieldAccessor.invoke(this) as Int
//#endif
