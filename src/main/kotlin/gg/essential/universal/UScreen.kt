package gg.essential.universal

import gg.essential.universal.render.UGpuSampler
import gg.essential.universal.render.UGpuTextureView
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import net.minecraft.client.gui.GuiScreen
import java.awt.Color

//#if MC>=12109
//$$ import net.minecraft.client.gui.Click
//$$ import net.minecraft.client.input.CharInput
//$$ import net.minecraft.client.input.KeyInput
//$$ import net.minecraft.client.input.MouseInput
//#endif

//#if MC>=12106
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#endif

//#if MC>=12000
//$$ import net.minecraft.client.gui.DrawContext
//#endif

//#if MC>=11502
//$$ import gg.essential.universal.UKeyboard.toInt
//$$ import gg.essential.universal.UKeyboard.toModifiers
//$$ import com.mojang.blaze3d.matrix.MatrixStack
//$$ import net.minecraft.util.text.ITextComponent
//#if MC<11900
//$$ import net.minecraft.util.text.TranslationTextComponent
//#endif
//#else
import org.lwjgl.input.Mouse
import java.io.IOException

//#endif

abstract class UScreen(
    val restoreCurrentGuiOnClose: Boolean = false,
    open var newGuiScale: Int = -1,
    open var unlocalizedName: String? = null
) :
//#if MC>=11900
//$$     Screen(Text.translatable(unlocalizedName ?: ""))
//#elseif MC>=11502
//$$     Screen(TranslationTextComponent(unlocalizedName ?: ""))
//#else
    GuiScreen()
//#endif
{
    @JvmOverloads
    constructor(
        restoreCurrentGuiOnClose: Boolean = false,
        newGuiScale: Int = -1,
    ) : this(restoreCurrentGuiOnClose, newGuiScale, null)

    private var guiScaleToRestore = -1
    private var restoringGuiScale = false
    private val screenToRestore: GuiScreen? = if (restoreCurrentGuiOnClose) currentScreen else null
    //#if MC>=12106
    //$$ // Background is now draw from the final `renderWithTooltip` method, before we ever get control, so we need
    //$$ // to suppress by default and can only allow during `onDrawScreen`.
    //$$ private var suppressBackground = true
    //#else
    private var suppressBackground = false
    //#endif

    //#if MC>=12106
    //$$ private val advancedDrawContext = AdvancedDrawContext()
    //#endif

    //#if MC>=12000
    //$$ private var drawContexts = mutableListOf<DrawContext>()
    //$$ private inline fun <R> withDrawContext(matrixStack: UMatrixStack, block: (DrawContext) -> R) {
    //#if MC>=12106
    //$$     val context = drawContexts.last()
    //$$     context.matrices.pushMatrix()
    //$$     matrixStack.to3x2Joml(context.matrices)
    //$$     block(context)
    //$$     context.matrices.popMatrix()
    //#else
    //$$     val client = this.client!!
    //$$     val context = drawContexts.lastOrNull()
    //$$         ?: DrawContext(client, client.bufferBuilders.entityVertexConsumers)
    //$$     context.matrices.push()
    //$$     val mc = context.matrices.peek()
    //$$     val uc = matrixStack.peek()
    //$$     mc.positionMatrix.set(uc.model)
    //$$     mc.normalMatrix.set(uc.normal)
    //$$     block(context)
        //#if MC>=12102
        //$$ context.draw()
        //#endif
    //$$     context.matrices.pop()
    //#endif
    //$$ }
    //#endif

    //#if MC>=11502
    //$$ private var lastClick = 0L
    //$$ private var lastDraggedDx = -1.0
    //$$ private var lastDraggedDy = -1.0
    //$$ private var lastScrolledX = -1.0
    //$$ private var lastScrolledY = -1.0
    //$$ private var lastScrolledDX = 0.0
    //$$
    //$$ final override fun init() {
    //$$     updateGuiScale()
    //$$     initScreen(width, height)
    //$$ }
    //$$
    //#if MC>=11900
    //$$ override fun getTitle(): Text = Text.translatable(unlocalizedName ?: "")
    //#else
    //$$ override fun getTitle(): ITextComponent = TranslationTextComponent(unlocalizedName ?: "")
    //#endif
    //$$
    //#if MC>=12000
    //$$ final override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    //$$     val renderer = renderer ?: uCreateRenderer().also { renderer = it }
    //$$     if (renderer != null) {
    //$$         val renderState = uExtractRenderState(mouseX, mouseY, delta)
    //$$         if (renderState.background) {
    //$$             drawContexts.add(context)
    //$$             onDrawBackground(UMatrixStack(context.matrices), 0)
    //$$             drawContexts.removeLast()
    //$$         }
    //$$         val textureView = renderer.render(renderState)
            //#if MC >= 1.21.6
            //$$ advancedDrawContext.draw(context, textureView)
            //#else
            //$$ drawImmediate(UMatrixStack(context.matrices), textureView)
            //#endif
    //$$         return
    //$$     }
    //#if MC >= 26.3
    //$$     throw NullPointerException("uCreateRenderer returned `null` but `onDrawScreen` is not supported on this version")
    //#else
    //$$     drawContexts.add(context)
        //#if MC>=12106
        //$$ advancedDrawContext.nextFrame()
        //$$ advancedDrawContext.drawImmediate(context) { stack ->
        //$$     suppressBackground = false
        //$$     onDrawScreenCompat(stack, mouseX, mouseY, delta)
        //$$     suppressBackground = true
        //$$ }
        //#else
        //$$ onDrawScreenCompat(UMatrixStack(context.matrices), mouseX, mouseY, delta)
        //#endif
    //$$     drawContexts.removeLast()
    //#endif
    //#elseif MC>=11602
    //$$ final override fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
    //$$     onDrawScreenCompat(UMatrixStack(matrixStack), mouseX, mouseY, partialTicks)
    //#else
    //$$ final override fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
    //$$     onDrawScreenCompat(UMatrixStack(), mouseX, mouseY, partialTicks)
    //#endif
    //$$ }
    //$$
    //#if MC>=12109
    //$$ final override fun keyPressed(input: KeyInput): Boolean {
    //$$     onKeyPressed(input.key, 0.toChar(), input.modifiers.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun keyReleased(input: KeyInput): Boolean {
    //$$     onKeyReleased(input.key, 0.toChar(), input.modifiers.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun charTyped(input: CharInput): Boolean {
    //$$     val codepoint = input.codepoint
        //#if MC >= 26.1
        //$$ val modifiers = 0.toModifiers()
        //#else
        //$$ val modifiers = input.modifiers.toModifiers()
        //#endif
    //$$     if (Character.isBmpCodePoint(codepoint)) {
    //$$         onKeyPressed(0, input.codepoint.toChar(), modifiers)
    //$$     } else if (Character.isValidCodePoint(codepoint)) {
    //$$         onKeyPressed(0, Character.highSurrogate(input.codepoint), modifiers)
    //$$         onKeyPressed(0, Character.lowSurrogate(input.codepoint), modifiers)
    //$$     }
    //$$     return false
    //$$ }
    //$$
    //$$ private var lastMouseInput: MouseInput? = null
    //$$ private var lastDoubled: Boolean? = null
    //$$
    //$$ final override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
    //$$     lastMouseInput = click.buttonInfo
    //$$     lastDoubled = doubled
    //$$     if (click.button() == 1) lastClick = UMinecraft.getTime()
    //$$     onMouseClicked(click.x, click.y, click.button())
    //$$     lastMouseInput = null
    //$$     lastDoubled = null
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseReleased(click: Click): Boolean {
    //$$     lastMouseInput = click.buttonInfo
    //$$     onMouseReleased(click.x, click.y, click.button())
    //$$     lastMouseInput = null
    //$$     return false
    //$$ }
    //$$
    //$$ override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
    //$$     lastMouseInput = click.buttonInfo
    //$$     lastDraggedDx = offsetX
    //$$     lastDraggedDy = offsetY
    //$$     onMouseDragged(click.x, click.y, click.button(), UMinecraft.getTime() - lastClick)
    //$$     lastMouseInput = null
    //$$     return false
    //$$ }
    //#else
    //$$ final override fun keyPressed(keyCode: Int, scanCode: Int, modifierCode: Int): Boolean {
    //$$     onKeyPressed(keyCode, 0.toChar(), modifierCode.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun keyReleased(keyCode: Int, scanCode: Int, modifierCode: Int): Boolean {
    //$$     onKeyReleased(keyCode, 0.toChar(), modifierCode.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun charTyped(char: Char, modifierCode: Int): Boolean {
    //$$     onKeyPressed(0, char, modifierCode.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
    //$$     if (mouseButton == 1)
    //$$         lastClick = UMinecraft.getTime()
    //$$     onMouseClicked(mouseX, mouseY, mouseButton)
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
    //$$     onMouseReleased(mouseX, mouseY, mouseButton)
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseDragged(x: Double, y: Double, mouseButton: Int, dx: Double, dy: Double): Boolean {
    //$$     lastDraggedDx = dx
    //$$     lastDraggedDy = dy
    //$$     onMouseDragged(x, y, mouseButton, UMinecraft.getTime() - lastClick)
    //$$     return false
    //$$ }
    //#endif
    //$$
    //#if MC>=12002
    //$$ override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, delta: Double): Boolean {
    //$$     lastScrolledDX = horizontalAmount
    //#else
    //$$ final override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
    //#endif
    //$$     lastScrolledX = mouseX
    //$$     lastScrolledY = mouseY
    //$$     @Suppress("DEPRECATION")
    //$$     onMouseScrolled(delta)
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun tick(): Unit = onTick()
    //$$
    //$$ final override fun onClose() {
    //$$     renderer?.close()
    //$$     renderer = null
        //#if MC>=12106
        //$$ advancedDrawContext.close()
        //#endif
    //$$     onScreenClose()
    //$$     restoreGuiScale()
    //$$ }
    //$$
    //#if MC>=12000
    //#if MC>=12002
    //$$ private var lastBackgroundMouseX = 0
    //$$ private var lastBackgroundMouseY = 0
    //$$ private var lastBackgroundDelta = 0f
    //$$ final override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    //$$     lastBackgroundMouseX = mouseX
    //$$     lastBackgroundMouseY = mouseY
    //$$     lastBackgroundDelta = delta
    //$$     if (suppressBackground) return
    //#else
    //$$ final override fun renderBackground(context: DrawContext) {
    //#endif
    //$$     drawContexts.add(context)
    //$$     onDrawBackgroundCompat(UMatrixStack(context.matrices), 0)
    //$$     drawContexts.removeLast()
    //$$ }
    //#elseif MC>=11904
    //$$ final override fun renderBackground(matrixStack: MatrixStack) {
    //$$     onDrawBackgroundCompat(UMatrixStack(matrixStack), 0)
    //$$ }
    //#elseif MC>=11602
    //$$ final override fun renderBackground(matrixStack: MatrixStack, vOffset: Int) {
    //$$     onDrawBackgroundCompat(UMatrixStack(matrixStack), vOffset)
    //$$ }
    //#else
    //$$ final override fun renderBackground(vOffset: Int) {
    //$$     onDrawBackgroundCompat(UMatrixStack(), vOffset)
    //$$ }
    //#endif
    //#else
    final override fun initGui() {
        updateGuiScale()
        initScreen(width, height)
    }

    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        onDrawScreenCompat(UMatrixStack(), mouseX, mouseY, partialTicks)
    }

    final override fun keyTyped(typedChar: Char, keyCode: Int) {
        onKeyPressed(keyCode, typedChar, UKeyboard.getModifiers())
    }

    final override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        onMouseClicked(mouseX.toDouble(), mouseY.toDouble(), mouseButton)
    }

    final override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        onMouseReleased(mouseX.toDouble(), mouseY.toDouble(), state)
    }

    final override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        onMouseDragged(mouseX.toDouble(), mouseY.toDouble(), clickedMouseButton, timeSinceLastClick)
    }

    final override fun handleMouseInput() {
        super.handleMouseInput()
        val scrollDelta = Mouse.getEventDWheel()
        @Suppress("DEPRECATION")
        if (scrollDelta != 0)
            onMouseScrolled(scrollDelta.toDouble())
    }

    final override fun updateScreen() {
        onTick()
    }

    final override fun onGuiClosed() {
        renderer?.close()
        renderer = null
        onScreenClose()
        restoreGuiScale()
    }

    final override fun drawWorldBackground(tint: Int) {
        onDrawBackgroundCompat(UMatrixStack(), tint)
    }
    //#endif

    constructor(restoreCurrentGuiOnClose: Boolean, newGuiScale: GuiScale) : this(
        restoreCurrentGuiOnClose,
        newGuiScale.ordinal
    )

    fun restorePreviousScreen() {
        displayScreen(screenToRestore)
    }

    open fun updateGuiScale() {
        if (newGuiScale != -1 && !restoringGuiScale) {
            if (guiScaleToRestore == -1)
                guiScaleToRestore = UMinecraft.guiScale
            UMinecraft.guiScale = newGuiScale
            width = UResolution.scaledWidth
            height = UResolution.scaledHeight
        }
    }

    private fun restoreGuiScale() {
        if (guiScaleToRestore != -1) {
            // This flag is necessary since on 1.20.5 setting the gui scale causes the screen's resize
            // method to be called due to an option change callback. This resize causes the screen to reinitialize,
            // which calls updateGuiScale. To prevent that method for changing the gui scale back,
            // we suppress its behavior with a flag.
            restoringGuiScale = true
            UMinecraft.guiScale = guiScaleToRestore
            restoringGuiScale = false
            guiScaleToRestore = -1
        }
    }

    open fun initScreen(width: Int, height: Int) {
        //#if MC>=11502
        //$$ super.init()
        //#else
        super.initGui()
        //#endif
    }

    interface Renderer : AutoCloseable {
        /**
         * Renders the given screen state, returns a texture containing the result.
         *
         * The given [state] is one returned from [UScreen.uExtractRenderState] of the same screen.
         * The implementation is free to cast it accordingly.
         *
         * The renderer retains ownership over the returned texture, it will not be closed by the caller.
         * It must remain valid until at least the next [render] call, or [close], whichever happens first.
         *
         * Beware that (in a future Minecraft version), this method may be called from a dedicated render thread, and
         * as such must not access any client state that is not safe to access from multiple threads.
         * In particular it should use the given [RenderState], that was created on the client thread, rather than
         * the screen directly.
         */
        fun render(state: RenderState): UGpuTextureView
    }

    /**
     * A snapshot of all state of this screen which is needed to render it.
     */
    interface RenderState {
        /** Whether the default background should be draw behind this screen. */
        val background: Boolean
    }

    /**
     * Creates a [Renderer] capable of rendering this screen to a texture.
     * When this method returns non-null, the legacy [onDrawScreen] method will not be used for this screen. Instead
     * [uExtractRenderState] will be called (and must as such be implemented).
     *
     * The [Renderer] returned by this method must be compatible with the [RenderState] type returned by the
     * [uExtractRenderState] of this screen.
     */
    open fun uCreateRenderer(): Renderer? = null

    /**
     * Takes a snapshot of the current state, as required for rendering it, of this screen.
     *
     * Beware that (in a future Minecraft version), the rendering may happen on a dedicated render thread. As such
     * the returned [RenderState] should be safe to be transferred and used by that thread.
     *
     * @see uCreateRenderer
     * @see RenderState
     */
    open fun uExtractRenderState(mouseX: Int, mouseY: Int, partialTicks: Float): RenderState = throw NotImplementedError()

    open fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        //#if MC<12106
        suppressBackground = true
        //#endif
        //#if MC>=12000
        //$$ withDrawContext(matrixStack) { drawContext ->
        //$$     super.render(drawContext, mouseX, mouseY, partialTicks)
        //$$ }
        //#elseif MC>=11602
        //$$ super.render(matrixStack.toMC(), mouseX, mouseY, partialTicks)
        //#else
        matrixStack.runWithGlobalState {
            //#if MC>=11502
            //$$ super.render(mouseX, mouseY, partialTicks)
            //#else
            super.drawScreen(mouseX, mouseY, partialTicks)
            //#endif
        }
        //#endif
        //#if MC<12106
        suppressBackground = false
        //#endif
    }

    @Deprecated(
        UMatrixStack.Compat.DEPRECATED,
        ReplaceWith("onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)")
    )
    open fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        onDrawScreen(UMatrixStack.Compat.get(), mouseX, mouseY, partialTicks)
    }

    private var renderer: Renderer? = null

    // Calls the deprecated method (for backwards compat) which then calls the new method (read the deprecation message)
    private fun onDrawScreenCompat(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val renderer = renderer ?: uCreateRenderer()?.also { renderer = it }
        if (renderer == null) {
            UMatrixStack.Compat.runLegacyMethod(matrixStack) {
                @Suppress("DEPRECATION")
                onDrawScreen(mouseX, mouseY, partialTicks)
            }
            return
        }

        val state = uExtractRenderState(mouseX, mouseY, partialTicks)
        if (state.background) {
            onDrawBackground(matrixStack, 0)
        }
        val textureView = renderer.render(state)
        drawImmediate(matrixStack, textureView)
    }

    private fun drawImmediate(matrixStack: UMatrixStack, textureView: UGpuTextureView) {
        val x1 = 0.0
        val x2 = UResolution.scaledWidth.toDouble()
        val y1 = 0.0
        val y2 = UResolution.scaledHeight.toDouble()
        val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)
        buffer.pos(matrixStack, x1, y2, 0.0).tex(0.0, 0.0).endVertex()
        buffer.pos(matrixStack, x2, y2, 0.0).tex(1.0, 0.0).endVertex()
        buffer.pos(matrixStack, x2, y1, 0.0).tex(1.0, 1.0).endVertex()
        buffer.pos(matrixStack, x1, y1, 0.0).tex(0.0, 1.0).endVertex()
        buffer.build()?.drawAndClose(PIPELINE_BLIT) {
            texture(0, textureView, SAMPLER_NEAREST)
        }
    }

    open fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        //#if MC>=11502
        //$$ if (keyCode != 0) {
            //#if MC>=12109
            //$$ super.keyPressed(KeyInput(keyCode, 0, modifiers.toInt()))
            //#else
            //$$ super.keyPressed(keyCode, 0, modifiers.toInt())
            //#endif
        //$$ }
        //$$ if (typedChar != 0.toChar()) {
            //#if MC >= 26.1
            //$$ super.charTyped(CharacterEvent(typedChar.code))
            //#elseif MC>=12109
            //$$ super.charTyped(CharInput(typedChar.code, modifiers.toInt()))
            //#else
            //$$ super.charTyped(typedChar, modifiers.toInt())
            //#endif
        //$$ }
        //#else
        try {
            super.keyTyped(typedChar, keyCode)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //#endif
    }

    open fun onKeyReleased(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
        //#if MC>=11502
        //$$ if (keyCode != 0) {
            //#if MC>=12109
            //$$ super.keyReleased(KeyInput(keyCode, 0, modifiers.toInt()))
            //#else
            //$$ super.keyReleased(keyCode, 0, modifiers.toInt())
            //#endif
        //$$ }
        //#endif
    }

    open fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
        //#if MC>=11502
        //$$ if (mouseButton == 1)
        //$$     lastClick = UMinecraft.getTime()
        //#if MC>=12109
        //$$ super.mouseClicked(Click(mouseX, mouseY, MouseInput(mouseButton, lastMouseInput?.modifiers ?: 0)), lastDoubled ?: false)
        //#else
        //$$ super.mouseClicked(mouseX, mouseY, mouseButton)
        //#endif
        //#else
        try {
            super.mouseClicked(mouseX.toInt(), mouseY.toInt(), mouseButton)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //#endif
    }

    open fun onMouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        //#if MC>=12109
        //$$ super.mouseReleased(Click(mouseX, mouseY, MouseInput(state, lastMouseInput?.modifiers ?: 0)))
        //#elseif MC>=11502
        //$$ super.mouseReleased(mouseX, mouseY, state)
        //#else
        super.mouseReleased(mouseX.toInt(), mouseY.toInt(), state)
        //#endif
    }

    open fun onMouseDragged(x: Double, y: Double, clickedButton: Int, timeSinceLastClick: Long) {
        //#if MC>=12109
        //$$ super.mouseDragged(Click(x, y, MouseInput(clickedButton, lastMouseInput?.modifiers ?: 0)), lastDraggedDx, lastDraggedDy)
        //#elseif MC>=11502
        //$$ super.mouseDragged(x, y, clickedButton, lastDraggedDx, lastDraggedDy)
        //#else
        super.mouseClickMove(x.toInt(), y.toInt(), clickedButton, timeSinceLastClick)
        //#endif
    }

    // This function receives the delta from both lwjgl 2 and lwjgl 3.
    // The deltas obtained from lwjgl 2 are scaled by a constant factor and thus much higher than the ones provided by lwjgl 3.
    @Deprecated("Provided `delta` values have different units depending on Minecraft versions.", ReplaceWith("onMouseScrolled(mouseX, mouseY, deltaHorizontal, deltaVertical)"))
    open fun onMouseScrolled(delta: Double) {
        //#if MC>=11502
        //$$ onMouseScrolled(lastScrolledX, lastScrolledY, lastScrolledDX, delta)
        //#else
        // Diving by 120 to revert the scaling which LWJGL 2 applies, so we get consistent deltas across all versions on the onMouseScrolled
        // https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/opengl/LinuxMouse.java#L48
        // https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/opengl/MacOSXNativeMouse.java#L53
        // https://github.com/LWJGL/lwjgl/blob/master/src/java/org/lwjgl/opengl/MouseEventQueue.java#L52
        onMouseScrolled(UMouse.Scaled.x, UMouse.Scaled.y, 0.0, delta / 120.0)
        //#endif
    }

    // Must be called with consistently scaled deltas on all mc/lwjgl versions.
    // This is to ensure a consistent scrolling experience across all versions.
    // See older function above this for further explanation.
    open fun onMouseScrolled(mouseX: Double, mouseY: Double, deltaHorizontal: Double, deltaVertical: Double) {
        //#if MC>=12002
        //$$ super.mouseScrolled(mouseX, mouseY, deltaHorizontal, deltaVertical)
        //#elseif MC>=11502
        //$$ super.mouseScrolled(mouseX, mouseY, deltaVertical)
        //#endif
    }

    open fun onTick() {
        //#if MC>=11502
        //$$ super.tick()
        //#else
        super.updateScreen()
        //#endif
    }

    open fun onScreenClose() {
        //#if MC>=11502
        //$$ super.onClose()
        //#else
        super.onGuiClosed()
        //#endif
    }

    open fun onDrawBackground(matrixStack: UMatrixStack, tint: Int) {
        //#if MC>=12000
        //$$ withDrawContext(matrixStack) { drawContext ->
            //#if MC>=12106
            //$$ drawContext.createNewRootLayer()
            //$$ val orgProjectionMatrixBuffer = RenderSystem.getProjectionMatrixBuffer()
            //$$ val orgProjectionType = RenderSystem.getProjectionType()
            //#endif
            //#if MC>=12002
            //$$ super.renderBackground(drawContext, lastBackgroundMouseX, lastBackgroundMouseY, lastBackgroundDelta)
            //#else
            //$$ super.renderBackground(drawContext)
            //#endif
            //#if MC>=12106
            //$$ @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            //$$ RenderSystem.setProjectionMatrix(orgProjectionMatrixBuffer, orgProjectionType)
            //$$ drawContext.createNewRootLayer()
            //#endif
        //$$ }
        //#elseif MC>=11904
        //$$ super.renderBackground(matrixStack.toMC())
        //#elseif MC>=11602
        //$$ super.renderBackground(matrixStack.toMC(), tint)
        //#else
        matrixStack.runWithGlobalState {
            //#if MC>=11502
            //$$ super.renderBackground(tint)
            //#else
            super.drawWorldBackground(tint)
            //#endif
        }
        //#endif
    }

    @Deprecated(
        UMatrixStack.Compat.DEPRECATED,
        ReplaceWith("onDrawBackground(matrixStack, tint)")
    )
    open fun onDrawBackground(tint: Int) {
        onDrawBackground(UMatrixStack.Compat.get(), tint)
    }

    // Calls the deprecated method (for backwards compat) which then calls the new method (read the deprecation message)
    fun onDrawBackgroundCompat(matrixStack: UMatrixStack, tint: Int) = UMatrixStack.Compat.runLegacyMethod(matrixStack) {
        @Suppress("DEPRECATION")
        onDrawBackground(tint)
    }

    companion object {
        @JvmStatic
        val currentScreen: GuiScreen?
            get() = UMinecraft.currentScreenObj as? GuiScreen?

        @JvmStatic
        fun displayScreen(screen: GuiScreen?) {
            UMinecraft.currentScreenObj = screen
        }

        /** Whether the [uCreateRenderer] API must be used because the old [onDrawScreen] is no longer supported. */
        val isRendererRequired: Boolean
            //#if MC >= 26.3
            //$$ get() = true
            //#else
            get() = false
            //#endif

        private val PIPELINE_BLIT = URenderPipeline.builderWithDefaultShader(
            "universalcraft:blit",
            UGraphics.DrawMode.QUADS,
            UGraphics.CommonVertexFormats.POSITION_TEXTURE,
        ).apply {
            blendState = BlendState.PREMULTIPLIED_ALPHA
        }.build()

        private val SAMPLER_NEAREST = UGpuSampler(
            UGpuSampler.AddressMode.CLAMP_TO_EDGE,
            UGpuSampler.AddressMode.CLAMP_TO_EDGE,
            UGpuSampler.FilterMode.NEAREST,
            UGpuSampler.FilterMode.NEAREST,
            false,
        )
    }
}
