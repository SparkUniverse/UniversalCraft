package gg.essential.universal

import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.universal.utils.TemporaryTextureAllocator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.AbstractTexture
import net.minecraft.util.Identifier

//#if MC >= 26.1
//$$ import net.minecraft.client.renderer.Projection
//$$ import net.minecraft.client.renderer.ProjectionMatrixBuffer
//#else
import net.minecraft.client.render.ProjectionMatrix2
//#endif

/**
 * Allows rendering of raw OpenGL into [DrawContext] by drawing to a temporary texture which is then submitted as a
 * plain textured quad to [DrawContext].
 *
 * You **MUST** call [nextFrame] before the next frame begins but no sooner than MC's GuiRenderer actually using the
 * submitted textures. Repeated calls to [drawImmediate] without [nextFrame] or [close] inbetween will keep allocating
 * more and more gpu memory!
 * If you cannot guarantee further calls to [nextFrame], you must call [close] to release all resources.
 * The [AdvancedDrawContext] remains usable, [close] merely frees all resources, further calls to [drawImmediate] will
 * simply re-allocate them.
 */
internal class AdvancedDrawContext : AutoCloseable {
    //#if MC >= 26.1
    //$$ private val projection = Projection()
    //$$ private var allocatedProjectionMatrix: ProjectionMatrixBuffer? = null
    //#else
    private var allocatedProjectionMatrix: ProjectionMatrix2? = null
    //#endif

    private val textureAllocator = TemporaryTextureAllocator {
        allocatedProjectionMatrix?.close()
        allocatedProjectionMatrix = null
    }

    fun drawImmediate(context: DrawContext, block: (UMatrixStack) -> Unit) {
        val scaleFactor = UResolution.scaleFactor.toFloat()
        val width = UResolution.viewportWidth
        val height = UResolution.viewportHeight

        val texture = textureAllocator.allocate(width, height)

        var projectionMatrix = allocatedProjectionMatrix
        if (projectionMatrix == null) {
            //#if MC >= 26.1
            //$$ projectionMatrix = ProjectionMatrixBuffer("pre-rendered screen")
            //#else
            projectionMatrix = ProjectionMatrix2("pre-rendered screen", 1000f, 21000f, true)
            //#endif
            allocatedProjectionMatrix = projectionMatrix
        }
        //#if MC >= 26.1
        //$$ projection.setupOrtho(1000f, 21000f, width.toFloat() / scaleFactor, height.toFloat() / scaleFactor, true)
        //$$ val projectionMatrixBuffer = projectionMatrix.getBuffer(projection)
        //#else
        val projectionMatrixBuffer = projectionMatrix.set(width.toFloat() / scaleFactor, height.toFloat() / scaleFactor)
        //#endif
        RenderSystem.setProjectionMatrix(projectionMatrixBuffer, ProjectionType.ORTHOGRAPHIC)

        val orgOutputColorTextureOverride = RenderSystem.outputColorTextureOverride
        val orgOutputDepthTextureOverride = RenderSystem.outputDepthTextureOverride
        RenderSystem.outputColorTextureOverride = texture.textureView
        RenderSystem.outputDepthTextureOverride = texture.depthTextureView

        val matrixStack = UMatrixStack()
        matrixStack.translate(0f, 0f, -10000f)
        block(matrixStack)

        RenderSystem.outputColorTextureOverride = orgOutputColorTextureOverride
        RenderSystem.outputDepthTextureOverride = orgOutputDepthTextureOverride

        draw(context, texture)
    }

    fun draw(context: DrawContext, texture: TemporaryTextureAllocator.TextureAllocation) {
        val width = texture.width
        val height = texture.height
        val scaleFactor = UResolution.scaleFactor.toFloat()

        val textureManager = MinecraftClient.getInstance().textureManager
        val identifier = Identifier.of("universalcraft", "__tmp_texture__")
        textureManager.registerTexture(identifier, object : AbstractTexture() {
            init { glTextureView = texture.textureView }
            override fun close() {} // we don't want the later `destroyTexture` to close our texture
        })

        context.matrices.pushMatrix()
        context.matrices.scale(1/scaleFactor, 1/scaleFactor) // drawTexture only accepts `int`s
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            identifier,
            // x, y
            0, 0,
            // u, v
            0f, height.toFloat(),
            // width, height
            width, height,
            // uWidth, vHeight
            width, -height,
            // textureWidth, textureHeight
            width, height,
        )
        context.matrices.popMatrix()

        textureManager.destroyTexture(identifier)
    }

    fun nextFrame() {
        textureAllocator.nextFrame()
    }

    override fun close() {
        textureAllocator.close()
    }
}
