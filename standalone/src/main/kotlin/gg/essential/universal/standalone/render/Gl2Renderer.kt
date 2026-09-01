package gg.essential.universal.standalone.render

import gg.essential.universal.UGraphics
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C

internal class Gl2Renderer {
    private val vao = if (GL.getCapabilities().OpenGL30) GL30C.glGenVertexArrays() else 0
    private val vertexBuffer = GL20C.glGenBuffers()
    private val indexBuffer = GL20C.glGenBuffers()

    fun draw(bufferBuilder: BufferBuilder, drawMode: UGraphics.DrawMode) {
        if (GL.getCapabilities().OpenGL30) {
            GL30C.glBindVertexArray(vao)
        }

        for (i in bufferBuilder.attributes.indices) {
            GL20C.glEnableVertexAttribArray(i)
        }

        GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, vertexBuffer)
        GL20C.glBufferData(GL20C.GL_ARRAY_BUFFER, bufferBuilder.array, GL20C.GL_STATIC_DRAW)

        when (drawMode) {
            UGraphics.DrawMode.QUADS -> {
                GL20C.glBindBuffer(GL20C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
                GL20C.glBufferData(GL20C.GL_ELEMENT_ARRAY_BUFFER, IndexBufferBuilder.forQuads(bufferBuilder.count / 4), GL20C.GL_STATIC_DRAW)
                renderInBatches(bufferBuilder, 4, 6)
            }
            UGraphics.DrawMode.TRIANGLES -> {
                setupVertexAttribPointers(bufferBuilder, 0)
                GL20C.glDrawArrays(GL20C.GL_TRIANGLES, 0, bufferBuilder.count)
            }
            UGraphics.DrawMode.TRIANGLE_FAN -> {
                GL20C.glBindBuffer(GL20C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer)
                GL20C.glBufferData(GL20C.GL_ELEMENT_ARRAY_BUFFER, IndexBufferBuilder.forTriangleFan(bufferBuilder.count - 2), GL20C.GL_STATIC_DRAW)
                if (bufferBuilder.count > UShort.MAX_VALUE.toInt()) {
                    TODO("Render triangle fans with more than ${UShort.MAX_VALUE} vertices.")
                }
                setupVertexAttribPointers(bufferBuilder, 0)
                GL20C.glDrawElements(GL20C.GL_TRIANGLES, (bufferBuilder.count - 2) * 3, GL20C.GL_UNSIGNED_SHORT, 0)
            }
            else -> TODO("Support rendering $drawMode")
        }

        for (i in bufferBuilder.attributes.indices) {
            GL20C.glDisableVertexAttribArray(i)
        }
    }

    private fun renderInBatches(bufferBuilder: BufferBuilder, vertsPerPrimitive: Int, indicesPerPrimitive: Int) {
        val vertices = bufferBuilder.count.toLong()
        // Limited by the fact that our index buffer is using Short; aligned at at shape boundaries
        val maxBatchSize = UShort.MAX_VALUE.toLong() / vertsPerPrimitive * vertsPerPrimitive
        for (offset in 0 until vertices step maxBatchSize) {
            val batchSize = (vertices - offset).coerceAtMost(maxBatchSize).toInt()

            setupVertexAttribPointers(bufferBuilder, offset)
            GL20C.glDrawElements(GL20C.GL_TRIANGLES, batchSize / vertsPerPrimitive * indicesPerPrimitive, GL20C.GL_UNSIGNED_SHORT, 0)
        }
    }

    private fun setupVertexAttribPointers(bufferBuilder: BufferBuilder, vertexOffset: Long) {
        var attrOffset = 0
        for ((index, attribute) in bufferBuilder.attributes.withIndex()) {
            GL20C.glVertexAttribPointer(
                index,
                attribute.size,
                GL20C.GL_FLOAT,
                false,
                bufferBuilder.stride * 4 /* bytes per float */,
                (attrOffset + vertexOffset * bufferBuilder.stride) * 4 /* bytes per float */
            )
            attrOffset += attribute.size
        }
    }
}