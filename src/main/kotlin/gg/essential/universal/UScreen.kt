package gg.essential.universal

import net.minecraft.client.gui.GuiScreen

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

private const val INPUTHANDLER_DEP_MSG = "Implement [UScreen.InputHandler] for input functions that more closely adhere to newer MC input behaviour."

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
    protected var inputHandler: InputHandler? = this as? InputHandler
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
    //#elseif MC>=11602
    //$$ final override fun render(matrixStack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
    //$$     onDrawScreenCompat(UMatrixStack(matrixStack), mouseX, mouseY, partialTicks)
    //#else
    //$$ final override fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
    //$$     onDrawScreenCompat(UMatrixStack(), mouseX, mouseY, partialTicks)
    //#endif
    //$$ }
    //$$
    //#if MC < 26.1 && MC >= 1.15.2
    //$$ // Smuggle this value for use in super calls, where intermediate functions have dropped it to match 26.1+
    //$$ private var lastCharModifiers = 0
    //#endif
    //#if MC>=12109
    //$$ final override fun keyPressed(input: KeyInput): Boolean {
    //$$     inputHandler?.let {
    //$$         return it.uKeyPressed(input.key, input.scancode, input.modifiers.toModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onKeyPressed(input.key, 0.toChar(), input.modifiers.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun keyReleased(input: KeyInput): Boolean {
    //$$     inputHandler?.let {
    //$$         return it.uKeyReleased(input.key, input.scancode, input.modifiers.toModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onKeyReleased(input.key, 0.toChar(), input.modifiers.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun charTyped(input: CharInput): Boolean {
    //$$     val codepoint = input.codepoint
        //#if MC >= 26.1
        //$$ val modifiers = 0.toModifiers()
        //#else
        //$$ val modifiers = input.modifiers.toModifiers()
        //$$ lastCharModifiers = input.modifiers
        //#endif
    //$$     inputHandler?.let {
    //$$         return it.uCharTyped(codepoint).also {
                //#if MC < 26.1
                //$$ lastCharModifiers = 0
                //#endif
    //$$         }
    //$$     }
    //$$     if (Character.isBmpCodePoint(codepoint)) {
    //$$         @Suppress("DEPRECATION") onKeyPressed(0, input.codepoint.toChar(), modifiers)
    //$$     } else if (Character.isValidCodePoint(codepoint)) {
    //$$         @Suppress("DEPRECATION") onKeyPressed(0, Character.highSurrogate(input.codepoint), modifiers)
    //$$         @Suppress("DEPRECATION") onKeyPressed(0, Character.lowSurrogate(input.codepoint), modifiers)
    //$$     }
        //#if MC < 26.1
        //$$ lastCharModifiers = 0
        //#endif
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
    //$$
    //$$     inputHandler?.let {
    //$$         return it.uMouseClicked(click.x, click.y, click.button(), click.modifiers().toModifiers()).also {
    //$$             lastMouseInput = null
    //$$             lastDoubled = null
    //$$         }
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseClicked(click.x, click.y, click.button())
    //$$     lastMouseInput = null
    //$$     lastDoubled = null
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseReleased(click: Click): Boolean {
    //$$     lastMouseInput = click.buttonInfo
    //$$
    //$$     inputHandler?.let {
    //$$         return it.uMouseReleased(click.x, click.y, click.button(), click.modifiers().toModifiers()).also {
    //$$             lastMouseInput = null
    //$$         }
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseReleased(click.x, click.y, click.button())
    //$$     lastMouseInput = null
    //$$     return false
    //$$ }
    //$$
    //$$ override fun mouseDragged(click: Click, offsetX: Double, offsetY: Double): Boolean {
    //$$     lastMouseInput = click.buttonInfo
    //$$     lastDraggedDx = offsetX
    //$$     lastDraggedDy = offsetY
    //$$
    //$$     inputHandler?.let {
    //$$         return it.uMouseDragged(click.x, click.y, click.button(), click.modifiers().toModifiers()).also {
    //$$             lastMouseInput = null
    //$$         }
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseDragged(click.x, click.y, click.button(), UMinecraft.getTime() - lastClick)
    //$$     lastMouseInput = null
    //$$     return false
    //$$ }
    //#else
    //$$ final override fun keyPressed(keyCode: Int, scanCode: Int, modifierCode: Int): Boolean {
    //$$     inputHandler?.let {
    //$$         return it.uKeyPressed(keyCode, scanCode, modifierCode.toModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onKeyPressed(keyCode, 0.toChar(), modifierCode.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun keyReleased(keyCode: Int, scanCode: Int, modifierCode: Int): Boolean {
    //$$     inputHandler?.let {
    //$$         return it.uKeyReleased(keyCode, scanCode, modifierCode.toModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onKeyReleased(keyCode, 0.toChar(), modifierCode.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun charTyped(char: Char, modifierCode: Int): Boolean {
    //$$     inputHandler?.let {
    //$$         lastCharModifiers = modifierCode
    //$$         return it.uCharTyped(char.code).also { lastCharModifiers = 0 }
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onKeyPressed(0, char, modifierCode.toModifiers())
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
    //$$     if (mouseButton == 1)
    //$$         lastClick = UMinecraft.getTime()
    //$$
    //$$     inputHandler?.let {
    //$$         return it.uMouseClicked(mouseX, mouseY, mouseButton, UKeyboard.getModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseClicked(mouseX, mouseY, mouseButton)
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseReleased(mouseX: Double, mouseY: Double, mouseButton: Int): Boolean {
    //$$     inputHandler?.let {
    //$$         return it.uMouseReleased(mouseX, mouseY, mouseButton, UKeyboard.getModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseReleased(mouseX, mouseY, mouseButton)
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun mouseDragged(x: Double, y: Double, mouseButton: Int, dx: Double, dy: Double): Boolean {
    //$$     lastDraggedDx = dx
    //$$     lastDraggedDy = dy
    //$$
    //$$     inputHandler?.let {
    //$$         return it.uMouseDragged(x, y, mouseButton, UKeyboard.getModifiers())
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseDragged(x, y, mouseButton, UMinecraft.getTime() - lastClick)
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
    //$$
    //$$     inputHandler?.let {
    //$$         return it.uMouseScrolled(mouseX, mouseY, lastScrolledDX, delta)
    //$$     }
    //$$
    //$$     @Suppress("DEPRECATION") onMouseScrolled(delta)
    //$$     return false
    //$$ }
    //$$
    //$$ final override fun tick(): Unit = onTick()
    //$$
    //$$ final override fun onClose() {
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
        inputHandler?.let {
            val handled = if (keyCode != 0) false else {
                it.uKeyPressed(keyCode, 0, UKeyboard.getModifiers())
            }
            if (!handled
                && !typedChar.isISOControl() // Block control code characters. E.G. the 'CTRL + A' character. https://en.wikipedia.org/wiki/Control_character
                && typedChar !in CharCategory.PRIVATE_USE // Block PUA characters. Known to be incorrectly sent by macOS + LWJGL2. https://en.wikipedia.org/wiki/Private_Use_Areas
            ) {
                it.uCharTyped(typedChar.code)
            }
        } ?: @Suppress("DEPRECATION") onKeyPressed(keyCode, typedChar, UKeyboard.getModifiers())
    }

    final override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        inputHandler?.uMouseClicked(mouseX.toDouble(), mouseY.toDouble(), mouseButton, UKeyboard.getModifiers())
            ?: @Suppress("DEPRECATION") onMouseClicked(mouseX.toDouble(), mouseY.toDouble(), mouseButton)
    }

    final override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        inputHandler?.uMouseReleased(mouseX.toDouble(), mouseY.toDouble(), state, UKeyboard.getModifiers())
            ?: @Suppress("DEPRECATION") onMouseReleased(mouseX.toDouble(), mouseY.toDouble(), state)
    }

    final override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        inputHandler?.uMouseDragged(mouseX.toDouble(), mouseY.toDouble(), clickedMouseButton, UKeyboard.getModifiers())
            ?: @Suppress("DEPRECATION") onMouseDragged(mouseX.toDouble(), mouseY.toDouble(), clickedMouseButton, timeSinceLastClick)
    }

    final override fun handleMouseInput() {
        super.handleMouseInput()
        val scrollDelta = Mouse.getEventDWheel()
        if (scrollDelta != 0) {
            inputHandler?.uMouseScrolled(UMouse.Scaled.x, UMouse.Scaled.y, 0.0,
                    // Revert LWJGL 2 delta scaling, see onMouseScrolled(Double) for more info
                    scrollDelta / 120.0)
                ?: @Suppress("DEPRECATION") onMouseScrolled(scrollDelta.toDouble())
        }
    }

    final override fun updateScreen() {
        onTick()
    }

    final override fun onGuiClosed() {
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

    // Calls the deprecated method (for backwards compat) which then calls the new method (read the deprecation message)
    private fun onDrawScreenCompat(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) = UMatrixStack.Compat.runLegacyMethod(matrixStack) {
        @Suppress("DEPRECATION")
        onDrawScreen(mouseX, mouseY, partialTicks)
    }

    @Deprecated(INPUTHANDLER_DEP_MSG)
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

    @Deprecated(INPUTHANDLER_DEP_MSG)
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

    @Deprecated(INPUTHANDLER_DEP_MSG)
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

    @Deprecated(INPUTHANDLER_DEP_MSG)
    open fun onMouseReleased(mouseX: Double, mouseY: Double, state: Int) {
        //#if MC>=12109
        //$$ super.mouseReleased(Click(mouseX, mouseY, MouseInput(state, lastMouseInput?.modifiers ?: 0)))
        //#elseif MC>=11502
        //$$ super.mouseReleased(mouseX, mouseY, state)
        //#else
        super.mouseReleased(mouseX.toInt(), mouseY.toInt(), state)
        //#endif
    }

    @Deprecated(INPUTHANDLER_DEP_MSG)
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
        @Suppress("DEPRECATION")
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
    @Deprecated(INPUTHANDLER_DEP_MSG)
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

    @Suppress("unused")
    private fun superMouseClicked(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean {
        //#if MC >= 1.21.9
        //$$ return super.mouseClicked(Click(x, y, MouseInput(button, modifiers.toInt())), lastDoubled ?: false)
        //#elseif MC >= 1.15.2
        //$$ return super.mouseClicked(x, y, button)
        //#else
        super.mouseClicked(x.toInt(), y.toInt(), button)
        return false
        //#endif
    }

    @Suppress("unused")
    private fun superMouseReleased(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean {
        //#if MC >= 1.21.9
        //$$ return super.mouseReleased(Click(x, y, MouseInput(button, modifiers.toInt())))
        //#elseif MC >= 1.15.2
        //$$ return super.mouseReleased(x, y, button)
        //#else
        super.mouseReleased(x.toInt(), y.toInt(), button)
        return false
        //#endif
    }

    @Suppress("unused")
    private fun superMouseDragged(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean {
        //#if MC >= 1.21.9
        //$$ return super.mouseDragged(Click(x, y, MouseInput(button, modifiers.toInt())), lastDraggedDx, lastDraggedDy)
        //#elseif MC >= 1.15.2
        //$$ return super.mouseDragged(x, y, button, lastDraggedDx, lastDraggedDy)
        //#else
        super.mouseClickMove(x.toInt(), y.toInt(), button, 0L)
        return false
        //#endif
    }

    @Suppress("unused")
    private fun superMouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        //#if MC >= 1.20.2
        //$$ return super.mouseScrolled(x, y, scrollX, scrollY)
        //#elseif MC >= 1.15.2
        //$$ return super.mouseScrolled(x, y, scrollY)
        //#else
        return false // No super
        //#endif
    }

    private fun superCharTyped(codepoint: Int): Boolean {
        //#if MC >= 26.1
        //$$ return super.charTyped(CharacterEvent(codepoint))
        //#elseif MC >= 1.21.9
        //$$ return super.charTyped(CharInput(codepoint, lastCharModifiers))
        //#elseif MC >= 1.15.2
        //$$ return super.charTyped(codepoint.toChar(), lastCharModifiers)
        //#else
        super.keyTyped(codepoint.toChar(), 0)
        return false
        //#endif
    }

    @Suppress("unused")
    private fun superKeyPressed(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean {
        //#if MC >= 1.21.9
        //$$ return super.keyPressed(KeyInput(key, scanCode, modifiers.toInt()))
        //#elseif MC >= 1.15.2
        //$$ return super.keyPressed(key, scanCode, modifiers.toInt())
        //#else
        super.keyTyped(0.toChar(), key)
        return false
        //#endif
    }

    @Suppress("unused")
    private fun superKeyReleased(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean {
        //#if MC >= 1.21.9
        //$$ return super.keyReleased(KeyInput(key, scanCode, modifiers.toInt()))
        //#elseif MC >= 1.15.2
        //$$ return super.keyReleased(key, scanCode, modifiers.toInt())
        //#else
        return false // No super
        //#endif
    }

    @Suppress("unused") // Becomes used if the child class is an instance of [InputHandler]
    fun uSuperInputHandler(): InputHandler = object : InputHandler {
        override fun uSuperInputHandler(): InputHandler = this

        override fun uMouseClicked(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            superMouseClicked(x, y, button, modifiers)

        override fun uMouseReleased(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            superMouseReleased(x, y, button, modifiers)

        override fun uMouseDragged(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            superMouseDragged(x, y, button, modifiers)

        override fun uMouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean =
            superMouseScrolled(x, y, scrollX, scrollY)

        override fun uCharTyped(codepoint: Int): Boolean =
            superCharTyped(codepoint)

        override fun uKeyPressed(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean =
            superKeyPressed(key, scanCode, modifiers)

        override fun uKeyReleased(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean =
            superKeyReleased(key, scanCode, modifiers)
    }

    /** Usually you can simply have your screen implement this interface, [UScreen] will then use it automatically.
     * If you require more control, you can instead also manually set the [inputHandler] property.
     * [UScreen] provides a [uSuperInputHandler] implementation you can call from your handler.
     *
     * On versions below 1.16, the boolean returns are not passed to Minecraft as they are not used,
     * the interface still replaces and executes the same for consistency.
     */
    interface InputHandler {
        fun uSuperInputHandler(): InputHandler

        fun uMouseClicked(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uMouseClicked(x, y, button, modifiers)

        fun uMouseReleased(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uMouseReleased(x, y, button, modifiers)

        fun uMouseDragged(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uMouseDragged(x, y, button, modifiers)

        // Must be called with consistently scaled scroll deltas on all mc/lwjgl versions.
        // This is to ensure a consistent scrolling experience across all versions.
        // See onMouseScrolled(Double) for further explanation.
        fun uMouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean =
            uSuperInputHandler().uMouseScrolled(x, y, scrollX, scrollY)

        fun uCharTyped(codepoint: Int): Boolean =
            uSuperInputHandler().uCharTyped(codepoint)

        fun uKeyPressed(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uKeyPressed(key, scanCode, modifiers)

        fun uKeyReleased(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uKeyReleased(key, scanCode, modifiers)
    }

    companion object {
        @JvmStatic
        val currentScreen: GuiScreen?
            get() = UMinecraft.getMinecraft().currentScreen

        @JvmStatic
        fun displayScreen(screen: GuiScreen?) {
            //#if MC<11200
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            //#endif
            UMinecraft.getMinecraft().displayGuiScreen(screen)
        }
    }
}
