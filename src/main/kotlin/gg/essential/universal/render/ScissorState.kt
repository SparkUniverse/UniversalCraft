package gg.essential.universal.render

import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer

//#if MC==12105
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#endif

internal data class ScissorState(val enabled: Boolean, val x: Int, val y: Int, val width: Int, val height: Int) {
    fun activate() {
        //#if MC>=12106 && !STANDALONE
        //$$ active = this
        //#elseif MC==12105
        //$$ if (enabled) {
        //$$     RenderSystem.SCISSOR_STATE.enable(x, y, width, height)
        //$$ } else {
        //$$     RenderSystem.SCISSOR_STATE.disable()
        //$$ }
        //#else
        if (enabled) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST)
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }
        GL11.glScissor(x, y, width, height)
        //#endif
    }

    fun coerceIn(x: Int, y: Int, width: Int, height: Int): ScissorState {
        var x1 = this.x
        var x2 = this.x + this.width
        var y1 = this.y
        var y2 = this.y + this.height
        x1 = x1.coerceAtLeast(x)
        x2 = x2.coerceAtMost(x + width)
        y1 = y1.coerceAtLeast(y)
        y2 = y2.coerceAtMost(y + height)
        return ScissorState(
            enabled,
            x1,
            y1,
            (x2 - x1).coerceAtLeast(0),
            (y2 - y1).coerceAtLeast(0),
        )
    }

    companion object {
        val DISABLED = ScissorState(false, 0, 0, 0, 0)

        //#if MC>=12106
        //$$ // MC no longer has a global scissor state, so we'll have our own until we've migrated away from it too
        //$$ private var active: ScissorState = DISABLED
        //#endif

        // Note: LWJGL2 requires a buffer of 16 elements, even if the property we query only has 4
        private val tmpIntBuffer = ByteBuffer.allocateDirect(16 * Int.SIZE_BYTES).asIntBuffer()

        fun active(): ScissorState {
            //#if MC>=12106 && !STANDALONE
            //$$ return active
            //#elseif MC==12105
            //$$ return with(RenderSystem.SCISSOR_STATE) { ScissorState(isEnabled, x, y, width, height) }
            //#else
            val bounds = tmpIntBuffer
                //#if MC>=11600
                //$$ .also { GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, it) }
                //#else
                .also { GL11.glGetInteger(GL11.GL_SCISSOR_BOX, it) }
                //#endif
            return ScissorState(
                enabled = GL11.glGetBoolean(GL11.GL_SCISSOR_TEST),
                x = bounds[0],
                y = bounds[1],
                width = bounds[2],
                height = bounds[3],
            )
            //#endif
        }
    }
}
