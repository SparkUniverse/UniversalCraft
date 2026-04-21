package gg.essential.universal.utils

import gg.essential.universal.UGraphics
import gg.essential.universal.render.UGpuFormat
import gg.essential.universal.render.UGpuTexture
import gg.essential.universal.render.UGpuTextureView
import gg.essential.universal.render.impl

//#if STANDALONE
//$$ import org.lwjgl.BufferUtils
//$$ import org.lwjgl.opengl.GL20C
//$$ import java.nio.Buffer
//#else
//#if MC>=12111
//$$ import com.mojang.blaze3d.textures.AddressMode
//#endif

//#if MC>=12106
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//#endif

//#if MC>=12105
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import com.mojang.blaze3d.textures.FilterMode
//$$ import com.mojang.blaze3d.textures.GpuTexture
//$$ import com.mojang.blaze3d.textures.TextureFormat
//$$ import net.minecraft.client.texture.GlTexture
//#endif

import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.resources.IResourceManager
//#endif

//#if MC<11502 || STANDALONE
import java.awt.image.BufferedImage
//#else
//$$ import com.mojang.blaze3d.platform.GlStateManager
//$$ import net.minecraft.client.renderer.texture.NativeImage
//$$ import org.lwjgl.opengl.GL11
//#endif


import java.io.Closeable
import java.io.IOException
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class ReleasedDynamicTexture private constructor(
    val width: Int,
    val height: Int,
    //#if MC>=11400 && !STANDALONE
    //$$ textureData: NativeImage?,
    //#else
    textureData: IntArray?,
    //#endif
//#if STANDALONE
//$$ ) {
//#else
) : AbstractTexture() {
//#endif

    private var resources = Resources(this)

    //#if MC>=11400 && !STANDALONE
    //$$ init {
    //$$     resources.textureData = textureData ?: NativeImage(width, height, true)
    //$$ }
    //$$ private var textureData by resources::textureData
    //#else
    var textureData: IntArray = textureData ?: IntArray(width * height)
    //#endif

    var uploaded: Boolean = false

    constructor(width: Int, height: Int) : this(width, height, null)

    //#if MC>=11400 && !STANDALONE
    //$$ constructor(nativeImage: NativeImage) : this(nativeImage.width, nativeImage.height, nativeImage)
    //#else
    constructor(bufferedImage: BufferedImage) : this(bufferedImage.width, bufferedImage.height) {
        bufferedImage.getRGB(0, 0, bufferedImage.width, bufferedImage.height, textureData, 0, bufferedImage.width)
    }
    //#endif

    //#if MC<12104 && !STANDALONE
    @Throws(IOException::class)
    override fun loadTexture(resourceManager: IResourceManager) {
    }
    //#endif

    fun updateDynamicTexture() {
        uploadTexture()
    }

    fun uploadTexture() {
        if (!uploaded) {
            val texture = UGraphics.getDevice().createTexture(
                null,
                UGpuTexture.Usage.TEXTURE_BINDING + UGpuTexture.Usage.COPY_SRC + UGpuTexture.Usage.COPY_DST,
                UGpuFormat.DEFAULT_RGBA,
                width,
                height,
            ).impl

            //#if STANDALONE
            //$$ GL20C.glBindTexture(GL20C.GL_TEXTURE_2D, texture.glId)
            //$$
            //$$ GL20C.glTexParameteri(GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MIN_FILTER, GL20C.GL_LINEAR)
            //$$ GL20C.glTexParameteri(GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_MAG_FILTER, GL20C.GL_NEAREST)
            //$$ GL20C.glTexParameteri(GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_WRAP_S, GL20C.GL_CLAMP_TO_EDGE)
            //$$ GL20C.glTexParameteri(GL20C.GL_TEXTURE_2D, GL20C.GL_TEXTURE_WRAP_T, GL20C.GL_CLAMP_TO_EDGE)
            //$$
            //$$ val nativeBuffer = BufferUtils.createIntBuffer(textureData.size)
            //$$ nativeBuffer.put(textureData)
            //$$ (nativeBuffer as Buffer).rewind()
            //$$ GL20C.glTexImage2D(
            //$$     GL20C.GL_TEXTURE_2D,
            //$$     0,
            //$$     GL20C.GL_RGBA,
            //$$     width,
            //$$     height,
            //$$     0,
            //$$     GL20C.GL_BGRA,
            //$$     GL20C.GL_UNSIGNED_BYTE,
            //$$     nativeBuffer
            //$$ )
            //#elseif MC>=12105
            //$$ val device = RenderSystem.getDevice()
            //#if MC>=12111
            //$$ sampler = RenderSystem.getSamplerCache().get(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.LINEAR, FilterMode.NEAREST, true);
            //#else
            //$$ texture.mc.setTextureFilter(FilterMode.NEAREST, true)
            //$$ UGraphics.configureTexture((texture.mc as GlTexture).glId) {
                //#if MC>=12106
                //$$ texture.mc.checkDirty(GL11.GL_TEXTURE_2D)
                //#else
                //$$ texture.mc.checkDirty()
                //#endif
            //$$ }
            //#endif
            //$$ device.createCommandEncoder().writeToTexture(texture.mc, textureData!!)
            //$$ this.glTexture = texture.mc
            //#else
            //#if MC>=11400
            //$$ UGraphics.configureTexture(texture.glId) {
            //$$     textureData?.uploadTextureSub(0, 0, 0, false)
            //$$     GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            //$$     GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
            //$$ }
            //#else
            TextureUtil.uploadTexture(
                texture.glId, textureData,
                width, height
            )
            //#endif
            glTextureId = texture.glId
            //#endif

            val textureView = UGraphics.getDevice().createTextureView(texture)
            resources.gpuTextureView = textureView
            //#if MC>=12106 && !STANDALONE
            //$$ this.glTextureView = textureView.impl.mc
            //#endif

            //#if MC>=11400 && !STANDALONE
            //$$ textureData = null
            //#else
            textureData = IntArray(0)
            //#endif

            uploaded = true

            Resources.drainCleanupQueue()
        }
    }

    val gpuTexture: UGpuTexture
        get() = gpuTextureView.texture

    val gpuTextureView: UGpuTextureView
        get() {
            uploadTexture()
            return resources.gpuTextureView ?: throw IllegalStateException("Texture has been closed.")
        }

    val dynamicGlId: Int
        //#if MC>=12105 && !STANDALONE
        //$$ get() {
        //$$     uploadTexture()
        //$$     return (resources.gpuTextureView?.texture?.impl?.mc as GlTexture?)?.glId ?: -1
        //$$ }
        //#else
        get() = getGlTextureId()
        //#endif

    //#if STANDALONE
    //$$ fun getGlTextureId(): Int {
    //$$     uploadTexture()
    //$$     return resources.gpuTextureView?.texture?.impl?.glId ?: -1
    //$$ }
    //$$
    //$$ fun deleteGlTexture() {
    //$$     resources.gpuTextureView = null
    //$$ }
    //#elseif MC>=12105
    //#if MC>=12106
    //$$ override fun getGlTextureView(): GpuTextureView {
    //$$     uploadTexture()
    //$$     return super.getGlTextureView()
    //$$ }
    //$$
    //#if MC<12111
    //$$ override fun setUseMipmaps(mipmaps: Boolean) {
    //$$     uploadTexture()
    //$$     super.setUseMipmaps(mipmaps)
    //$$ }
    //#endif
    //#endif
    //$$
    //#if MC<12111
    //$$ override fun setClamp(clamp: Boolean) {
    //$$     uploadTexture()
    //$$     super.setClamp(clamp)
    //$$ }
    //$$
    //$$ override fun setFilter(bilinear: Boolean, mipmap: Boolean) {
    //$$     uploadTexture()
    //$$     super.setFilter(bilinear, mipmap)
    //$$ }
    //#endif
    //$$
    //$$ override fun getGlTexture(): GpuTexture {
    //$$     uploadTexture()
    //$$     return super.getGlTexture()
    //$$ }
    //#else
    override fun getGlTextureId(): Int {
        uploadTexture()
        return super.getGlTextureId()
    }

    override fun deleteGlTexture() {
        super.deleteGlTexture()
        resources.gpuTextureView = null
    }
    //#endif

    //#if STANDALONE
    //#elseif MC>=12105
    //$$ override fun close() {
    //$$     super.close()
    //$$     resources.close()
    //$$ }
    //#elseif MC>=11600
    //$$ override fun close() {
    //$$     deleteGlTexture()
    //$$     resources.close()
    //$$ }
    //#endif

    private class Resources(referent: ReleasedDynamicTexture) : PhantomReference<ReleasedDynamicTexture>(referent, referenceQueue), Closeable {
        var gpuTextureView: UGpuTextureView? = null
           set(value) {
               field?.texture?.close()
               field?.close()
               field = value
           }

        //#if MC>=11400 && !STANDALONE
        //$$ var textureData: NativeImage? = null
        //$$    set(value) {
        //$$        field?.close()
        //$$        field = value
        //$$    }
        //#endif

        init {
            toBeCleanedUp.add(this)
        }

        override fun close() {
            toBeCleanedUp.remove(this)

            gpuTextureView = null

            //#if MC>=11400 && !STANDALONE
            //$$ textureData = null
            //#endif
        }

        companion object {
            val referenceQueue: ReferenceQueue<ReleasedDynamicTexture> = ReferenceQueue()
            val toBeCleanedUp: MutableSet<Resources> = Collections.newSetFromMap(ConcurrentHashMap())

            fun drainCleanupQueue() {
                while (true) {
                    ((referenceQueue.poll() ?: break) as Resources).close()
                }
            }
        }
    }
}
