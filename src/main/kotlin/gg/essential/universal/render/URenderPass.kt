package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
interface URenderPass : AutoCloseable {
    fun scissor(x: Int, y: Int, width: Int, height: Int)

    fun pipeline(pipeline: URenderPipeline)
    // Note: Currently only slot 0 is supported
    fun vertexBuffer(slot: Int, buffer: UGpuBufferSlice)
    fun indexBuffer(buffer: UGpuBuffer, type: IndexType)
    fun texture(name: String, textureView: UGpuTextureView, sampler: UGpuSampler)
    fun uniform(name: String, vararg values: Float)
    fun uniform(name: String, vararg values: Int)
    fun uniform(name: String, buffer: UGpuBufferSlice) // Note: Currently only supported on 1.21.6+

    fun projectionMatrix(matrix: FloatArray)
    fun modelViewMatrix(matrix: FloatArray)

    // Note: `instanceCount`, `firstVertex`, `firstInstance` are not yet universally supported
    fun draw(vertexCount: Int, instanceCount: Int = 1, firstVertex: Int = 0, firstInstance: Int = 0)
    // Note: `instanceCount`, `firstIndex`, `vertexOffset`, `firstInstance` are not yet universally supported
    fun drawIndexed(indexCount: Int, instanceCount: Int = 1, firstIndex: Int = 0, vertexOffset: Int = 0, firstInstance: Int = 0)

    enum class IndexType {
        SHORT,
        INT,
    }
}
