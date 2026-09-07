// MC < 26.2
package gg.essential.universal.render

import org.lwjgl.opengl.ARBSync
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL32.GL_ALREADY_SIGNALED
import org.lwjgl.opengl.GL32.GL_CONDITION_SATISFIED
import org.lwjgl.opengl.GL32.GL_TIMEOUT_EXPIRED
import org.lwjgl.opengl.GL32.GL_WAIT_FAILED

//#if MC >= 1.14
//$$ typealias GLSync = Long
//#else
import org.lwjgl.opengl.GLSync
//#endif

internal class UGpuFenceImpl : UGpuFence {
    private var glId: GLSync? = glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)

    override fun awaitCompletion(timeoutNs: Long): Boolean {
        val glId = glId ?: return true

        return when (val result = glClientWaitSync(glId, 0, timeoutNs)) {
            GL_ALREADY_SIGNALED -> true
            GL_TIMEOUT_EXPIRED -> false
            GL_CONDITION_SATISFIED -> true
            GL_WAIT_FAILED -> throw RuntimeException("glClientWaitSync returned GL_WAIT_FAILED: " + GL11.glGetError())
            else -> throw AssertionError("glClientWaitSync returned unexpected value $result")
        }
    }

    override fun close() {
        glId?.let { glDeleteSync(it) }
        glId = null
    }
}

private fun capabilities() =
    //#if MC >= 1.14
    //$$ org.lwjgl.opengl.GL.getCapabilities()
    //#else
    org.lwjgl.opengl.GLContext.getCapabilities()
    //#endif

private fun glFenceSync(condition: Int, flags: Int): GLSync? {
    return when {
        capabilities().OpenGL32 -> GL32.glFenceSync(condition, flags)
            //#if MC < 1.14
            .takeIf { it.isValid }
            //#endif
        capabilities().GL_ARB_sync -> ARBSync.glFenceSync(condition, flags)
            //#if MC < 1.14
            .takeIf { it.isValid }
            //#endif
        else -> null
    }
}
private fun glDeleteSync(id: GLSync) {
    when {
        capabilities().OpenGL32 -> GL32.glDeleteSync(id)
        capabilities().GL_ARB_sync -> ARBSync.glDeleteSync(id)
        else -> {}
    }
}
private fun glClientWaitSync(id: GLSync, flags: Int, timeoutNs: Long): Int {
    return when {
        capabilities().OpenGL32 -> GL32.glClientWaitSync(id, flags, timeoutNs)
        capabilities().GL_ARB_sync -> ARBSync.glClientWaitSync(id, flags, timeoutNs)
        else -> GL_ALREADY_SIGNALED
    }
}
