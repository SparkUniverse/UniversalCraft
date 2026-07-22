// MC >= 26.2
package gg.essential.universal.render

import com.mojang.blaze3d.systems.RenderSystem

internal class UGpuFenceImpl : UGpuFence {
    private var mc = RenderSystem.getDevice().createCommandEncoder().createFence()
    override fun awaitCompletion(timeoutNs: Long): Boolean = mc.awaitCompletion(timeoutNs)
    override fun close() = mc.close()
}
