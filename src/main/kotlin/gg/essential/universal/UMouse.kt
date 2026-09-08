package gg.essential.universal

//#if MC<=11202
import org.lwjgl.input.Mouse
//#endif

import kotlin.math.max

object UMouse {
    object Raw {
        //#if STANDALONE
        //$$ @JvmStatic
        //$$ var x: Double = 0.0
        //$$     internal set
        //$$ @JvmStatic
        //$$ var y: Double = 0.0
        //$$     internal set
        //#else
        @JvmStatic
        val x: Double
            get() {
                //#if MC>=11502
                //$$ return UMinecraft.getMinecraft().mouseHelper.mouseX
                //#else
                return Mouse.getX().toDouble()
                //#endif
            }

        @JvmStatic
        val y: Double
            get() {
                //#if MC>=11400
                //$$ return UMinecraft.getMinecraft().mouseHelper.mouseY
                //#else
                return UResolution.windowHeight - Mouse.getY().toDouble() - 1
                //#endif
            }
        //#endif
    }

    object Scaled {
        @JvmStatic
        val x: Double
            get() = Raw.x * UResolution.scaledWidth / max(1, UResolution.windowWidth)

        @JvmStatic
        val y: Double
            get() = Raw.y * UResolution.scaledHeight / max(1, UResolution.windowHeight)
    }

    @JvmStatic
    @Deprecated("Orientation is different between Minecraft versions.", replaceWith = ReplaceWith("UMouse.Raw.x"))
    fun getTrueX(): Double {
        //#if MC>=11502
        //$$ return Raw.x
        //#else
        return Mouse.getX().toDouble()
        //#endif
    }

    @JvmStatic
    @Deprecated("Orientation is different between Minecraft versions.", replaceWith = ReplaceWith("UMouse.Scaled.x"))
    @Suppress("DEPRECATION")
    fun getScaledX(): Double {
        return getTrueX() * UResolution.scaledWidth / max(1, UResolution.windowWidth)
    }

    @JvmStatic
    @Deprecated("Orientation is different between Minecraft versions.", replaceWith = ReplaceWith("UMouse.Raw.y"))
    fun getTrueY(): Double {
        //#if MC>=11502
        //$$ return Raw.y
        //#else
        return Mouse.getY().toDouble()
        //#endif
    }

    @JvmStatic
    @Deprecated("Orientation is different between Minecraft versions.", replaceWith = ReplaceWith("UMouse.Scaled.y"))
    @Suppress("DEPRECATION")
    fun getScaledY(): Double {
        return getTrueY() * UResolution.scaledHeight / max(1, UResolution.windowHeight)
    }

    fun buttonGlfwToSdl(glfw: Int): Int = when (glfw) {
        0 -> 1 // left
        1 -> 3 // right
        2 -> 2 // middle
        3 -> 4 // X1
        4 -> 5 // X2
        else -> 1
    }
    fun buttonSdlToGlfw(sdl: Int): Int = when (sdl) {
        1 -> 0 // left
        3 -> 1 // right
        2 -> 2 // middle
        4 -> 3 // X1
        5 -> 4 // X2
        else -> 0
    }
}
