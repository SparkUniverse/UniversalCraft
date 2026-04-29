package gg.essential.universal.vertex

import gg.essential.universal.UGraphics

//#if STANDALONE
//$$ import gg.essential.universal.standalone.render.BufferBuilder
//$$ import gg.essential.universal.standalone.render.VertexFormat
//#else
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.VertexFormat

//#if MC >= 26.2
//$$ import com.mojang.blaze3d.vertex.ByteBufferBuilder
//#endif

//#if MC>=11900 && MC < 26.2
//$$ import net.minecraft.client.render.Tessellator
//#endif
//#endif

//#if STANDALONE
//$$ import gg.essential.universal.standalone.render.BufferBuilder as BuiltBuffer
//#elseif MC>=12100
//$$ import net.minecraft.client.render.BuiltBuffer
//#elseif MC>=11900
//$$ import net.minecraft.client.render.BufferBuilder.BuiltBuffer
//#else
import net.minecraft.client.renderer.WorldRenderer as BuiltBuffer
//#endif

/**
 * Builds a list of vertices to be rendered.
 *
 * Note that once a builder has been created, it must be completed with a call to [build], it may not simply be dropped.
 * Note that only a single builder may be active at any one time (this restriction may be lifted in the future).
 * Note that instances of this class should generally be used immediately and may not be valid across frames.
 */
interface UBufferBuilder : UVertexConsumer {
    /**
     * Finishes the builder. Returns `null` if no vertices were emitted.
     *
     * Calling any other method on this builder after this one, or calling this one multiple times, is undefined
     * behavior.
     */
    fun build(): UBuiltBuffer?

    companion object {
        //#if MC >= 26.2 && !STANDALONE
        //$$ private val sharedBuffer = ByteBufferBuilder(64 * 1024)
        //#endif

        @JvmStatic
        fun create(drawMode: UGraphics.DrawMode, format: UGraphics.CommonVertexFormats): UBufferBuilder =
            create(drawMode, format.mc)

        @JvmStatic
        fun create(drawMode: UGraphics.DrawMode, format: VertexFormat): UBufferBuilder {
            //#if STANDALONE
            //$$ @Suppress("UNUSED_VARIABLE") val unused = drawMode
            //$$ return BufferBuilder(format.parts)
            //#else
            //#if MC >= 26.2
            //$$ val mcBufferBuilder = BufferBuilder(sharedBuffer, drawMode.mcMode, format)
            //#elseif MC>=12100
            //$$ val mcBufferBuilder = Tessellator.getInstance().begin(drawMode.mcMode, format)
            //#else
            //#if MC>=11900
            //$$ val mcBufferBuilder = Tessellator.getInstance().buffer
            //#else
            val mcBufferBuilder = UBufferBuilderImpl.POOL.removeLastOrNull() ?: WorldRenderer(1024 * 1024)
            //#endif
            mcBufferBuilder.begin(drawMode.mcMode, format)
            //#endif
            return UBufferBuilderImpl(mcBufferBuilder)
            //#endif
        }

        //#if !STANDALONE
        private class UBufferBuilderImpl(val mc: WorldRenderer) : VanillaVertexConsumer(mc), UBufferBuilder {
            //#if MC>=11900
            //$$ override fun build(): UBuiltBuffer? =
            //$$     mc.endNullable()?.let { UBuiltBufferImpl(it) }
            //#else
            private var vertexCount: Int = 0 // can't just use the MC one, that becomes private in 1.16

            override fun endVertex(): UVertexConsumer {
                vertexCount++
                return super.endVertex()
            }

            override fun build(): UBuiltBuffer? {
                mc.finishDrawing()
                if (vertexCount > 0) {
                    return UBuiltBufferImpl(mc)
                } else {
                    mc.reset()
                    POOL.add(mc)
                    return null
                }
            }

            companion object {
                val POOL = mutableListOf<WorldRenderer>()
            }
            //#endif
        }

        private class UBuiltBufferImpl(override val mc: BuiltBuffer) : UBuiltBufferInternal {
            private var closed = false
            override fun close() {
                if (closed) return
                closed = true

                //#if MC>=12100
                //$$ mc.close()
                //#elseif MC>=11900
                //$$ mc.release()
                //#else
                mc.reset()
                UBufferBuilderImpl.POOL.add(mc)
                //#endif
            }

            override fun closedExternally() {
                closed = true
                //#if MC<11900
                UBufferBuilderImpl.POOL.add(mc)
                //#endif
            }
        }
        //#endif
    }
}
