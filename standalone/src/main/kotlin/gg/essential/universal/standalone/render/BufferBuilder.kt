package gg.essential.universal.standalone.render

import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec3
import dev.folomeev.kotgl.matrix.vectors.mutables.mutableVec4
import gg.essential.universal.UMatrixStack
import gg.essential.universal.standalone.render.VertexFormat.Part
import gg.essential.universal.standalone.utils.timesSelf
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.universal.vertex.UBuiltBuffer
import gg.essential.universal.vertex.UBuiltBufferInternal
import gg.essential.universal.vertex.UVertexConsumer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal class BufferBuilder(
    val attributes: List<Part>,
) : UVertexConsumer, UBufferBuilder, UBuiltBufferInternal {
    /** How many floats there are in one vertex */
    val stride: Int = attributes.sumOf { it.size }

    private var idx: Int = 0
    internal var byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(stride * 64 * 4).order(ByteOrder.nativeOrder())
    internal var array = byteBuffer.asFloatBuffer()

    private operator fun FloatBuffer.set(index: Int, value: Float) = put(index, value)

    /** How many vertices there are in this buffer */
    val count: Int
        get() = idx / stride

    override fun pos(stack: UMatrixStack, x: Double, y: Double, z: Double) = apply {
        val vec = mutableVec4(x.toFloat(), y.toFloat(), z.toFloat(), 1f)
        vec.timesSelf(stack.peek().model)
        array[idx + 0] = vec.x
        array[idx + 1] = vec.y
        array[idx + 2] = vec.z
        array[idx + 3] = vec.w
        idx += 4
    }

    override fun tex(u: Double, v: Double) = apply {
        array[idx + 0] = u.toFloat()
        array[idx + 1] = v.toFloat()
        idx += 2
    }

    override fun norm(stack: UMatrixStack, x: Float, y: Float, z: Float) = apply {
        val vec = mutableVec3(x, y, z)
        vec.timesSelf(stack.peek().normal)
        array[idx + 0] = vec.x
        array[idx + 1] = vec.y
        array[idx + 2] = vec.z
        idx += 3
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): UVertexConsumer =
        color(red / 255f, green / 255f, blue / 255f, alpha / 255f)

    override fun color(red: Float, green: Float, blue: Float, alpha: Float): UVertexConsumer = apply {
        array[idx + 0] = red
        array[idx + 1] = green
        array[idx + 2] = blue
        array[idx + 3] = alpha
        idx += 4
    }

    override fun light(u: Int, v: Int): UVertexConsumer = apply {
        array[idx + 0] = u / 240f
        array[idx + 1] = v / 240f
        idx += 2
    }

    override fun overlay(u: Int, v: Int): UVertexConsumer {
        TODO("not yet supported")
    }

    override fun endVertex() = apply {
        if (idx >= array.remaining()) {
            val clone = ByteBuffer.allocateDirect(byteBuffer.remaining() * 2).order(ByteOrder.nativeOrder())
            clone.put(byteBuffer)
            clone.position(0)
            byteBuffer = clone
            array = clone.asFloatBuffer()
        }
    }

    override fun build(): UBuiltBuffer? {
        byteBuffer.limit(idx * 4)
        return this.takeIf { count > 0 }
    }

    override val mc: BufferBuilder
        get() = this

    override fun close() {
    }

    override fun closedExternally() {
    }
}

