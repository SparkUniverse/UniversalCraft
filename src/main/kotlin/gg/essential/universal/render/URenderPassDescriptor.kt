package gg.essential.universal.render

import java.lang.IllegalStateException
import java.util.function.Supplier

class URenderPassDescriptor(
    internal val label: Supplier<String>,
) {
    internal val colorAttachments = mutableListOf<Pair<UGpuTextureView, ClearColor?>>()
    internal var depthAttachment: Pair<UGpuTextureView, Double?>? = null
    internal var renderArea: RenderArea? = null

    fun withColorAttachment(textureView: UGpuTextureView, clearColor: ClearColor? = null) = apply {
        check(colorAttachments.isEmpty()) { "Multiple color attachments are not yet supported." }
        check(textureView.baseMipLevel == 0 && textureView.mipLevels == 1) { "Rendering to mipLevel other than 0 is not yet supported." }
        colorAttachments.add(Pair(textureView, clearColor))
    }

    fun withDepthAttachment(textureView: UGpuTextureView, clearDepth: Double? = null) = apply {
        check(textureView.baseMipLevel == 0 && textureView.mipLevels == 1) { "Rendering to mipLevel other than 0 is not yet supported." }
        depthAttachment = Pair(textureView, clearDepth)
    }

    fun withRenderArea(renderArea: RenderArea) = apply {
        this.renderArea = renderArea
    }

    internal fun outputSize(): RenderArea {
        val textureView = colorAttachments.firstNotNullOfOrNull { it.first } ?: depthAttachment?.first
        if (textureView == null) {
            throw IllegalStateException("Neither color nor depth attachments have been set.")
        }
        val width = textureView.texture.width shr textureView.baseMipLevel
        val height = textureView.texture.height shr textureView.baseMipLevel
        return RenderArea(0, 0, width, height)
    }

    data class ClearColor(
        val red: Float,
        val green: Float,
        val blue: Float,
        val alpha: Float,
    )

    data class RenderArea(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    )
}
