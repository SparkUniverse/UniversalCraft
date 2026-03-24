package gg.essential.universal

abstract class UScreen(
    val restoreCurrentGuiOnClose: Boolean = false,
    open var newGuiScale: Int = -1,
    open var unlocalizedName: String? = null
) {
    @JvmOverloads
    constructor(
        restoreCurrentGuiOnClose: Boolean = false,
        newGuiScale: Int = -1,
    ) : this(restoreCurrentGuiOnClose, newGuiScale, null)

    private var guiScaleToRestore = -1
    private val screenToRestore: UScreen? = if (restoreCurrentGuiOnClose) currentScreen else null
    protected var inputHandler: InputHandler? = this as? InputHandler

    fun standaloneGetInputHandler(): InputHandler? = inputHandler

    fun initGui() {
        updateGuiScale()
        initScreen(UResolution.scaledWidth, UResolution.scaledHeight)
    }

    fun onGuiClosed() {
        onScreenClose()
        if (guiScaleToRestore != -1)
            UMinecraft.guiScale = guiScaleToRestore
    }

    constructor(restoreCurrentGuiOnClose: Boolean, newGuiScale: GuiScale) : this(
        restoreCurrentGuiOnClose,
        newGuiScale.ordinal
    )

    fun restorePreviousScreen() {
        displayScreen(screenToRestore)
    }

    open fun updateGuiScale() {
        if (newGuiScale != -1) {
            if (guiScaleToRestore == -1)
                guiScaleToRestore = UMinecraft.guiScale
            UMinecraft.guiScale = newGuiScale
        }
    }

    open fun initScreen(width: Int, height: Int) {
    }

    open fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
    }

    open fun onKeyPressed(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
    }

    open fun onKeyReleased(keyCode: Int, typedChar: Char, modifiers: UKeyboard.Modifiers?) {
    }

    open fun onMouseClicked(mouseX: Double, mouseY: Double, mouseButton: Int) {
    }

    open fun onMouseReleased(mouseX: Double, mouseY: Double, state: Int) {
    }

    open fun onMouseDragged(x: Double, y: Double, clickedButton: Int, timeSinceLastClick: Long) {
    }

    open fun onMouseScrolled(delta: Double) {
    }

    open fun onMouseScrolled(mouseX: Double, mouseY: Double, deltaHorizontal: Double, deltaVertical: Double) {
        onMouseScrolled(deltaVertical)
    }

    open fun onTick() {
    }

    open fun onScreenClose() {
    }

    open fun onDrawBackground(matrixStack: UMatrixStack, tint: Int) {
    }

    fun uSuperInputHandler(): InputHandler = object : InputHandler {
        override fun uSuperInputHandler(): InputHandler = this
        override fun uMouseClicked(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean = false
        override fun uMouseReleased(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean = false
        override fun uMouseDragged(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean = false
        override fun uMouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean = false
        override fun uCharTyped(codepoint: Int): Boolean = false
        override fun uKeyPressed(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean = false
        override fun uKeyReleased(key: Int, scanCode: Int, modifiers: UKeyboard.Modifiers): Boolean = false
    }

    interface InputHandler {
        fun uSuperInputHandler(): InputHandler

        fun uMouseClicked(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uMouseClicked(x, y, button, modifiers)

        fun uMouseReleased(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uMouseReleased(x, y, button, modifiers)

        fun uMouseDragged(x: Double, y: Double, button: Int, modifiers: UKeyboard.Modifiers): Boolean =
            uSuperInputHandler().uMouseDragged(x, y, button, modifiers)

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
        var currentScreen: UScreen? = null
            private set

        fun displayScreen(screen: UScreen?) {
            currentScreen?.onGuiClosed()
            currentScreen = screen
            currentScreen?.initGui()
        }
    }
}
