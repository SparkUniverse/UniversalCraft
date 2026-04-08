package gg.essential.universal

//#if STANDALONE
//$$ import kotlin.coroutines.EmptyCoroutineContext
//$$ import kotlinx.coroutines.Dispatchers
//$$ import org.lwjgl.glfw.GLFW
//#else
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiNewChat
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.settings.GameSettings

//#if MC>=11502
//$$ import net.minecraft.client.util.NativeUtil
//#endif
//#endif

object UMinecraft {
    //#if STANDALONE
    //$$ @JvmStatic
    //$$ var guiScale: Int = 1
    //#else
    //#if MC>=11900
    //$$ private var guiScaleValue: Int
    //$$     get() = getSettings().guiScale.value
    //$$     set(value) { getSettings().guiScale.value = value }
    //#else
    private var guiScaleValue: Int
        get() = getSettings().guiScale
        set(value) { getSettings().guiScale = value }
    //#endif

    @JvmStatic
    var guiScale: Int
        get() = guiScaleValue
        set(value) {
            guiScaleValue = value
            //#if MC>=11502 && MC<12005
            //$$ val mc = getMinecraft()
            //$$ val window = mc.mainWindow
            //$$ val scaleFactor = window.calcGuiScale(value, mc.forceUnicodeFont)
            //$$ window.setGuiScale(scaleFactor.toDouble())
            //#endif
        }
    //#endif

    @JvmField
    val isRunningOnMac: Boolean =
        //#if STANDALONE
        //$$ UDesktop.isMac
        //#elseif MC>=12109
        //$$ net.minecraft.client.input.SystemKeycodes.IS_MAC_OS
        //#else
        Minecraft.isRunningOnMac
        //#endif

    //#if !STANDALONE
    @JvmStatic
    fun getMinecraft(): Minecraft {
        return Minecraft.getMinecraft()
    }

    @JvmStatic
    fun getWorld(): WorldClient? {
        return getMinecraft().theWorld
    }

    @JvmStatic
    fun getNetHandler(): NetHandlerPlayClient? {
        return getMinecraft().netHandler
    }

    @JvmStatic
    fun getPlayer(): EntityPlayerSP? {
        return getMinecraft().thePlayer
    }

    @JvmStatic
    fun getFontRenderer(): FontRenderer {
        return getMinecraft().fontRendererObj
    }
    //#endif

    @JvmStatic
    fun getTime(): Long {
        //#if STANDALONE
        //$$ return (GLFW.glfwGetTime() * 1000).toLong()
        //#elseif MC>=11502
        //$$ return (NativeUtil.getTime() * 1000).toLong()
        //#else
        return Minecraft.getSystemTime()
        //#endif
    }

    //#if !STANDALONE
    @JvmStatic
    //#if FORGELIKE
    @Suppress("UNNECESSARY_SAFE_CALL") // Forge adds inappropriate NonNullByDefault
    //#endif
    fun getChatGUI(): GuiNewChat? =
        //#if MC >= 26.2
        //$$ getMinecraft().gui.hud.chat
        //#else
        getMinecraft().ingameGUI?.chatGUI
        //#endif

    @JvmStatic
    fun getSettings(): GameSettings = getMinecraft().gameSettings
    //#endif

    @JvmStatic
    var currentScreenObj: Any?
        //#if STANDALONE
        //$$ get() = UScreen.currentScreen
        //$$ set(value) = UScreen.displayScreen(value as UScreen?)
        //#elseif MC >= 26.2
        //$$ get() = getMinecraft().gui.screen()
        //$$ set(value) = getMinecraft().gui.setScreen(value as Screen?)
        //#else
        get() = getMinecraft().currentScreen
        set(value) {
            //#if MC<11200
            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
            //#endif
            getMinecraft().displayGuiScreen(value as GuiScreen?)
        }
        //#endif


    @JvmStatic
    fun isCallingFromMinecraftThread(): Boolean {
        //#if STANDALONE
        //$$ return !Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext)
        //#elseif MC>=11400
        //$$ return Minecraft.getInstance().isOnExecutionThread
        //#else
        return Minecraft.getMinecraft().isCallingFromMinecraftThread
        //#endif
    }
}
