package gg.essential.universal.vertex

import gg.essential.universal.render.DrawCallBuilder
import gg.essential.universal.render.URenderPassLegacyImpl
import gg.essential.universal.render.URenderPipeline
import org.jetbrains.annotations.ApiStatus.NonExtendable
import java.nio.ByteBuffer

//#if STANDALONE
//#else
//#if MC>=11600
//$$ import net.minecraft.client.renderer.RenderType;
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
 * A list of vertices to be rendered.
 *
 * Note that instances of this class should generally be used immediately and may not be valid across frames.
 * Note that drawing the same buffer multiple times is not currently supported (but may be in the future).
 */
@NonExtendable // will be cast to UBuiltBufferInternal
interface UBuiltBuffer : AutoCloseable {
    /**
     * Returns the raw [ByteBuffer] data.
     * This consumes the underlying [UBuiltBuffer], no method other than [close] may be called afterwards.
     * The returned [ByteBuffer] is only guaranteed to be valid until [close] is called.
     */
    fun toByteBuffer(): ByteBuffer

    fun drawAndClose(pipeline: URenderPipeline, configure: DrawCallBuilder.() -> Unit = {}): Unit =
        use { draw(pipeline, configure) }

    fun draw(pipeline: URenderPipeline, configure: DrawCallBuilder.() -> Unit = {}) {
        URenderPassLegacyImpl().use { renderPass ->
            renderPass.draw(this, pipeline, configure)
        }
    }

    //#if MC >= 1.16 && MC < 26.2
    //$$ fun drawAndClose(renderLayer: RenderType): Unit =
    //$$     use { draw(renderLayer) }
    //$$
    //$$ fun draw(renderLayer: RenderType) {
    //$$     val mc = (this as UBuiltBufferInternal).mc
        //#if MC>=12100
        //$$ renderLayer.draw(mc)
        //#else
        //$$ renderLayer.setupRenderState()
        //#if MC>=11900
        //$$ net.minecraft.client.render.BufferRenderer.drawWithShader(mc)
        //#else
        //$$ net.minecraft.client.renderer.WorldVertexBufferUploader.draw(mc)
        //#endif
        //$$ renderLayer.clearRenderState()
        //#endif
    //$$ }
    //#endif

    companion object {
        //#if !STANDALONE
        /** Wraps the given MC buffer into a [UBuiltBuffer]. */
        @JvmStatic
        fun wrap(mc: BuiltBuffer): UBuiltBuffer = WrapperImpl(mc)

        private class WrapperImpl(override val mc: BuiltBuffer) : UBuiltBufferInternal {
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
                //#endif
            }

            override fun closedExternally() {
                closed = true
            }
        }
        //#endif
    }
}
