package gg.essential.universal.render

import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class ViewportState(val x: Int, val y: Int, val width: Int, val height: Int) {
    fun activate() {
        GL11.glViewport(x, y, width, height)
    }

    companion object {
        // Note: LWJGL2 requires a buffer of 16 elements, even if the property we query only has 4
        private val tmpIntBuffer = ByteBuffer.allocateDirect(16 * Int.SIZE_BYTES).order(ByteOrder.nativeOrder()).asIntBuffer()

        fun active(): ViewportState {
            val viewport = tmpIntBuffer
                //#if MC>=11600
                //$$ .also { GL11.glGetIntegerv(GL11.GL_VIEWPORT, it) }
                //#else
                .also { GL11.glGetInteger(GL11.GL_VIEWPORT, it) }
                //#endif
            return ViewportState(
                x = viewport[0],
                y = viewport[1],
                width = viewport[2],
                height = viewport[3],
            )
        }
    }
}
