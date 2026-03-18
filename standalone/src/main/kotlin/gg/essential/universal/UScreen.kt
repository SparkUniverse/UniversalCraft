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
