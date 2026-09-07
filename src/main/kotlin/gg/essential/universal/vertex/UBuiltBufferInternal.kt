package gg.essential.universal.vertex

import java.nio.ByteBuffer

//#if STANDALONE
//$$ import gg.essential.universal.standalone.render.BufferBuilder as BuiltBuffer
//#elseif MC>=12100
//$$ import net.minecraft.client.render.BuiltBuffer
//#elseif MC>=11900
//$$ import net.minecraft.client.render.BufferBuilder.BuiltBuffer
//#else
import net.minecraft.client.renderer.WorldRenderer as BuiltBuffer
//#endif

internal interface UBuiltBufferInternal : UBuiltBuffer {
    val mc: BuiltBuffer
    fun closedExternally()

    override fun toByteBuffer(): ByteBuffer {
        //#if STANDALONE
        //$$ return mc.byteBuffer
        //#elseif MC >= 1.21
        //$$ return mc.buffer
        //#elseif MC >= 1.19
        //$$ return mc.vertexBuffer
        //#elseif MC >= 1.16
        //$$ return mc.nextBuffer.second
        //#else
        return mc.byteBuffer
        //#endif
        //#if MC >= 1.20.4 && !STANDALONE
        //$$     // Buffers used exclusively for sorting won't have any vertex data.
        //$$     // Our API doesn't provide any way to construct such buffers, so we'll just throw when someone used `wrap`
        //$$     // on such a buffer.
        //$$     ?: throw IllegalStateException("Cannot get vertex data of index-only buffer")
        //#endif
    }
}
