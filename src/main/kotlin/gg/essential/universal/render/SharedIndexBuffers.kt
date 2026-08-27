package gg.essential.universal.render

import gg.essential.universal.UGraphics
import org.jetbrains.annotations.ApiStatus
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer

/**
 * Provides common shared index buffers for drawing with [URenderPass.drawIndexed].
 * @see SharedIndexBuffer.invoke
 */
object SharedIndexBuffers {
    /** Identity mapping. Consider using [URenderPass.draw] instead. */
    val sequential: SharedIndexBuffer =
        //#if MC >= 1.21.6 && !STANDALONE
        //$$ SharedIndexBufferFromMc(UGraphics.DrawMode.TRIANGLES, 1, 1)
        //#else
        object : IndexBufferBuilder() {
            override fun vertCountToIndexCount(vertices: Int): Int = vertices

            override fun fill(buf: IntBuffer) {
                for (i in 0 until buf.remaining()) {
                    buf.put(i, i)
                }
            }
        }
        //#endif

    /** Maps a sequence of four vertices into two triangles to form a quad each: 0 1 2 0 2 3 */
    val quads: SharedIndexBuffer =
        //#if MC >= 1.21.6 && !STANDALONE
        //$$ SharedIndexBufferFromMc(UGraphics.DrawMode.QUADS, 4, 6)
        //#else
        object : IndexBufferBuilder() {
            override fun vertCountToIndexCount(vertices: Int): Int = vertices / 4 * 6

            override fun fill(buf: IntBuffer) {
                var vert = 0
                for (i in 0 until buf.remaining() step 6) {
                    //  First triangle
                    buf.put(i + 0, vert + 0)
                    buf.put(i + 1, vert + 1)
                    buf.put(i + 2, vert + 2)
                    // Second triangle
                    buf.put(i + 3, vert + 0)
                    buf.put(i + 4, vert + 2)
                    buf.put(i + 5, vert + 3)
                    // Advance to next primitive (4 vertices per quad)
                    vert += 4
                }
            }
        }
        //#endif

    /** Forms a triangle for each vertex using the previous vertex and the 0th vertex: 0 1 2 0 2 3 0 3 4 0 4 5 */
    val triangleFan: SharedIndexBuffer =
        object : IndexBufferBuilder() {
            override fun vertCountToIndexCount(vertices: Int): Int = (vertices - 2) * 3

            override fun fill(buf: IntBuffer) {
                var vert = 0
                for (i in 0 until buf.remaining() step 3) {
                    buf.put(i + 0, 0)
                    buf.put(i + 1, vert + 1)
                    buf.put(i + 2, vert + 2)
                    vert++
                }
            }
        }
}

@ApiStatus.NonExtendable
interface SharedIndexBuffer {
    /**
     * Returns an index buffer for drawing [vertices] vertices with [URenderPass.drawIndexed].
     *
     * The returned buffer is shared between muliple calls and must not be closed by the caller.
     * Note: A previously returned buffer may however be invalidated when a larger buffer is requested.
     *       So only the most recently returned buffer may be assumed valid.
     */
    operator fun invoke(vertices: Int): Pair<UGpuBuffer, URenderPass.IndexType>
}


private abstract class IndexBufferBuilder : SharedIndexBuffer {
    private var gpuBuffer: UGpuBuffer? = null

    override operator fun invoke(vertices: Int): Pair<UGpuBuffer, URenderPass.IndexType> {
        val indices = vertCountToIndexCount(vertices)
        val bytesSize = indices * Int.SIZE_BYTES

        val existing = gpuBuffer
        if (existing != null && existing.size >= bytesSize) return Pair(existing, URenderPass.IndexType.INT)

        val byteBuf = ByteBuffer.allocateDirect(bytesSize).order(ByteOrder.nativeOrder())
        fill(byteBuf.asIntBuffer())

        val newGpuBuffer = UGraphics.getDevice().createBuffer(UGpuBuffer.Usage.INDEX, byteBuf)
        gpuBuffer?.close()
        gpuBuffer = newGpuBuffer
        return Pair(newGpuBuffer, URenderPass.IndexType.INT)
    }

    abstract fun vertCountToIndexCount(vertices: Int): Int
    abstract fun fill(buf: IntBuffer)
}

//#if MC >= 1.21.6 && !STANDALONE
//$$ private class SharedIndexBufferFromMc(
//$$     val drawMode: UGraphics.DrawMode,
//$$     val verticesPerPrimitive: Int,
//$$     val indicesPerPrimitive: Int,
//$$ ) : SharedIndexBuffer {
//$$     override fun invoke(vertices: Int): Pair<UGpuBuffer, URenderPass.IndexType> {
//$$         val indices = vertices / verticesPerPrimitive * indicesPerPrimitive
//$$         val mc = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(drawMode.mcMode)
//$$         val gpuBuffer = UGraphics.getPlatformAdapter().buffer(mc.getIndexBuffer(indices))
//$$         val indexType = when (mc.indexType) {
            //#if MC >= 26.2
            //$$ com.mojang.blaze3d.IndexType.SHORT -> URenderPass.IndexType.SHORT
            //$$ com.mojang.blaze3d.IndexType.INT -> URenderPass.IndexType.INT
            //#else
            //$$ com.mojang.blaze3d.vertex.VertexFormat.IndexType.SHORT -> URenderPass.IndexType.SHORT
            //$$ com.mojang.blaze3d.vertex.VertexFormat.IndexType.INT -> URenderPass.IndexType.INT
            //#endif
//$$             null -> throw AssertionError()
//$$         }
//$$         return Pair(gpuBuffer, indexType)
//$$     }
//$$ }
//#endif
