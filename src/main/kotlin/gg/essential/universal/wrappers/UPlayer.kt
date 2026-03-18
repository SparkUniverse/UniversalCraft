package gg.essential.universal.wrappers

import gg.essential.universal.UMinecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.util.IChatComponent
import java.util.*

object UPlayer {
    @JvmStatic
    fun getPlayer(): EntityPlayerSP? {
        return UMinecraft.getMinecraft().thePlayer
    }

    @JvmStatic
    fun hasPlayer() = getPlayer() != null

    //#if MC<12105
    @Suppress("DEPRECATION")
    @JvmStatic
    fun sendClientSideMessage(message: gg.essential.universal.wrappers.message.UTextComponent) = sendClientSideMessage(message as IChatComponent)
    //#endif

    @JvmStatic
    fun sendClientSideMessage(message: IChatComponent) {
        //#if MC >= 26.1
        //$$ UMinecraft.getMinecraft().gui.chat.addClientSystemMessage(message)
        //#elseif MC>=12102
        //$$ getPlayer()!!.sendMessage(message, false)
        //#elseif MC>=11900
        //$$ getPlayer()!!.sendMessage(message)
        //#elseif MC>=11602
        //#if FORGE
        //$$ @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // Forge adds inappropriate NonNullByDefault
        //#endif
        //$$ getPlayer()!!.sendMessage(message, null)
        //#elseif MC>=11202
        //$$ getPlayer()!!.sendMessage(message)
        //#else
        getPlayer()!!.addChatMessage(message)
        //#endif
    }

    @JvmStatic
    fun getUUID(): UUID {
        //#if MC>=12002
        //$$ return UMinecraft.getMinecraft().session.uuidOrNull // misnamed, should not actually ever be null
        //#else
        return UMinecraft.getMinecraft().session.profile.id
        //#endif
    }

    @JvmStatic
    fun getPosX(): Double {
        return getPlayer()?.posX
            ?: throw NullPointerException("UPlayer.getPosX() called with no existing Player")
    }

    @JvmStatic
    fun getPosY(): Double {
        return getPlayer()?.posY
            ?: throw NullPointerException("UPlayer.getPosY() called with no existing Player")
    }

    @JvmStatic
    fun getPosZ(): Double {
        return getPlayer()?.posZ
            ?: throw NullPointerException("UPlayer.getPosZ() called with no existing Player")
    }

    @JvmStatic
    fun getPrevPosX(): Double {
        return getPlayer()?.prevPosX
            ?: throw NullPointerException("UPlayer.getPrevPosX() called with no existing Player")
    }

    @JvmStatic
    fun getPrevPosY(): Double {
        return getPlayer()?.prevPosY
            ?: throw NullPointerException("UPlayer.getPrevPosY() called with no existing Player")
    }

    @JvmStatic
    fun getPrevPosZ(): Double {
        return getPlayer()?.prevPosZ
            ?: throw NullPointerException("UPlayer.getPrevPosZ() called with no existing Player")
    }
}
