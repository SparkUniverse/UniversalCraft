package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable

@NonExtendable
interface DrawCallBuilder {
    fun noScissor(): DrawCallBuilder
    fun scissor(x: Int, y: Int, width: Int, height: Int): DrawCallBuilder

    fun uniform(name: String, vararg values: Int): DrawCallBuilder
    fun uniform(name: String, vararg values: Float): DrawCallBuilder

    fun texture(name: String, textureView: UGpuTextureView, sampler: UGpuSampler): DrawCallBuilder
    fun texture(index: Int, textureView: UGpuTextureView, sampler: UGpuSampler): DrawCallBuilder

    @Deprecated("Does not support Vulkan; uses hard-coded sampler on 1.21.11+, relies on texture configuration on older versions.")
    fun texture(name: String, textureGlId: Int): DrawCallBuilder
    @Deprecated("Does not support Vulkan; uses hard-coded sampler on 1.21.11+, relies on texture configuration on older versions.")
    fun texture(index: Int, textureGlId: Int): DrawCallBuilder
}
