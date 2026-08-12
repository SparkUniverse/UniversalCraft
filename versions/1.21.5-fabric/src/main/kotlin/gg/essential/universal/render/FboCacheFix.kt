//#if MC < 26.2
package gg.essential.universal.render

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.textures.GpuTexture
import gg.essential.universal.utils.UnsafeHacks
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import it.unimi.dsi.fastutil.ints.Int2IntFunction
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.ints.IntLists
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import it.unimi.dsi.fastutil.objects.Object2IntFunction
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import net.minecraft.client.texture.GlTexture
import org.lwjgl.opengl.GL30

//#if MC >= 1.21.6
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import com.mojang.blaze3d.textures.GpuTextureView
//$$ import net.minecraft.client.texture.GlTextureView
//#endif

/**
 * This class hijacks / re-implements MC's FrameBufferObject management code on 1.21.5 - 26.1.
 *
 * This is because MC's management approach is broken:
 * MC stores, in the color GlTexture object, a simple Int2IntMap which maps depth texture gl ids to FBO gl ids.
 * When the color texture is closed, it then deletes all the associated FBOs.
 * The issue is that it does not delete the FBOs when the depth texture is closed. By itself, this is merely a resource
 * leak. The problem becomes larger due to the fact that MC uses the raw OpenGL id of the depth texture, as opposed to
 * the depth GlTexture object, as they key in its Map; but OpenGL implementations are free to re-use ids. So if a depth
 * texture is deleted, then a new one is created and happens to receive the same id, MC will return the stale
 * framebuffer for it, and therefore not actually render to the desired texture.
 *
 * This class works around that by hijacking the Map of each GlTexture that it can get its hands on.
 * Instead it stores FBOs in a global Map keyed by both color and depth texture objects, and then also deleting any
 * associated FBOs when the color or depth texture is closed.
 *
 * To do that, given the Map it replaces never actually gets access to the depth texture object, only its GL id, it also
 * maintains a global map of GL ids to GlTexture instances.
 *
 * Some thought has also been given to compatibility: Only one mod can replace the Map, so there's an inherent risk
 * of incompatibility here.
 * The current implementation has been designed to still function correctly should another mod (or another, relocated,
 * instance of UniversalCraft) try to do the same thing.
 */
internal object FboCacheFix {
    data class FboKey(val color: FboCacheHook, val depth: FboCacheHook)
    private val cachedFbos = Object2IntOpenHashMap<FboKey>()

    private val trackedTextures = Int2ObjectOpenHashMap<FboCacheHook>()

    // If a FBO is currently bound, we need to delay its deletion, since it may still be in use
    // (there is a small chance a mod stores the id is somewhere and uses it only later, but there's not much we can do
    //  about that)
    private val delayedDeletion = mutableListOf<Int>()
    private fun deleteFbo(id: Int) {
        val drawFbo = GlStateManager._getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GlStateManager._getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)

        if (delayedDeletion.isNotEmpty()) {
            delayedDeletion.removeIf { otherId ->
                if (otherId != drawFbo && otherId != readFbo) {
                    GlStateManager._glDeleteFramebuffers(otherId)
                    true
                } else {
                    false
                }
            }
        }

        if (id != drawFbo && id != readFbo) {
            GlStateManager._glDeleteFramebuffers(id)
            return
        }
        delayedDeletion.add(id)
    }

    fun track(texture: GpuTexture) {
        if (texture !is GlTexture) return

        val glId = texture.glId
        val orgFboCache = texture.fboCache
        if (orgFboCache is FboCacheHook) {
            return
        }

        if (orgFboCache != null) {
            // Note: Intentionally using `.values.iterator()` to match the vanilla code in case another mod (or a
            //       relocated instance of UC) has already hooked this texture.
            orgFboCache.values
                .iterator()
                .forEachRemaining(::deleteFbo)
        }

        val hook = FboCacheHook(texture)
        trackedTextures[glId] = hook
        texture.fboCache = hook

        //#if MC >= 1.21.11
        //$$ // We also need to prevent GlTexture from storing anything in its `firstFboId`/`firstFboDepthId` fields.
        //$$ // Firstly, clean up whatever is already in there
        //$$ val firstFboId = texture.firstFboId
        //$$ if (firstFboId != -1) deleteFbo(firstFboId)
        //$$ // Then, to prevent it from reading that entry, we'll set `firstFboDepthId` to a huge random number, that
        //$$ // hopefully will never be used in practice.
        //$$ texture.firstFboDepthId = 1869032957
        //$$ // Finally, to prevent it from storing a new entry (it checks `firstFboId != -1` for that), we'll set
        //$$ // `firstFboId` to 0. MC will think it's occupied; and then later, when the texture is closed, OpenGL will
        //$$ // ignore the 0 passed to `glDeleteFramebuffers` as per its contract.
        //$$ texture.firstFboId = 0
        //#endif
    }

    // Very similar to above, just for GpuTexture**View** (which as of 1.21.11 stores the FBOs for createRenderPass)
    // Only difference is that views don't get put in `trackedTextures` (but their corresponding GlTexture is).
    //#if MC >= 1.21.11
    //$$ fun track(texture: GpuTextureView) {
    //$$     if (texture !is GlTextureView) return
    //$$
    //$$     val orgFboCache = texture.fboCache
    //$$     if (orgFboCache is FboCacheHook) {
    //$$         return
    //$$     }
    //$$
    //$$     if (orgFboCache != null) {
    //$$         // Note: Intentionally using `.values.iterator()` to match the vanilla code in case another mod (or a
    //$$         //       relocated instance of UC) has already hooked this texture.
    //$$         orgFboCache.values
    //$$             .iterator()
    //$$             .forEachRemaining(::deleteFbo)
    //$$     }
    //$$
    //$$     texture.fboCache = FboCacheHook(texture.texture())
    //$$
    //$$     // We also need to prevent GlTexture from storing anything in its `firstFboId`/`firstFboDepthId` fields.
    //$$     // Firstly, clean up whatever is already in there
    //$$     val firstFboId = texture.firstFboId
    //$$     if (firstFboId != -1) deleteFbo(firstFboId)
    //$$     // Then, to prevent it from reading that entry, we'll set `firstFboDepthId` to a huge random number, that
    //$$     // hopefully will never be used in practice.
    //$$     texture.firstFboDepthId = 1869032957
    //$$     // Finally, to prevent it from storing a new entry (it checks `firstFboId != -1` for that), we'll set
    //$$     // `firstFboId` to 0. MC will think it's occupied; and then later, when the texture is closed, OpenGL will
    //$$     // ignore the 0 passed to `glDeleteFramebuffers` as per its contract.
    //$$     texture.firstFboId = 0
    //$$
    //$$     track(texture.texture())
    //$$ }
    //#elseif MC >= 1.21.6
    //$$ fun track(texture: GpuTextureView): Unit = track(texture.texture())
    //#endif

    class FboCacheHook(val texture: GlTexture) : Int2IntMap {
        var noDepthFbo = 0
        val associatedFboKeys = mutableListOf<FboKey>()
        val untrustedFbos = Int2IntArrayMap()

        // Called by vanilla to query for an existing cache fbo, and to register a new fbo
        override fun computeIfAbsent(key: Int, mappingFunction: Int2IntFunction): Int {
            if (key == 0) {
                if (noDepthFbo == 0) {
                    noDepthFbo = mappingFunction.get(key)
                }
                return noDepthFbo
            }

            var depth = trackedTextures[key]
            //#if MC >= 1.21.6
            //$$ if (depth == null) {
            //$$     // Good chance we can find the depth texture set as the current override
            //$$     val maybeDepth = RenderSystem.outputDepthTextureOverride
            //$$     if (maybeDepth != null && maybeDepth is GlTextureView && maybeDepth.texture().glId == key) {
            //$$         track(maybeDepth.texture())
            //$$         depth = trackedTextures[key]
            //$$     }
            //$$ }
            //#endif
            if (depth == null) {
                // If we haven't hooked the depth texture, we won't get to know when it is closed either,
                // so we can't really cache the fbo because we won't know when it gets invalidated.
                // In such a case, we'll fall back to generating a new FBO on each call.
                val oldFboGlId = untrustedFbos[key]
                if (oldFboGlId != 0) deleteFbo(oldFboGlId)
                val newFboGlId = mappingFunction.get(key)
                untrustedFbos[key] = newFboGlId
                return newFboGlId
            }

            val fboKey = FboKey(this, depth)
            return cachedFbos.computeIfAbsent(fboKey, Object2IntFunction { _ ->
                associatedFboKeys.add(fboKey)
                depth.associatedFboKeys.add(fboKey)
                return@Object2IntFunction mappingFunction.get(key)
            })
        }

        // Called by vanilla on `close`, so we'll invalidate all our cache framebuffer here
        override val values: IntCollection
            get() {
                if (noDepthFbo != 0) {
                    deleteFbo(noDepthFbo)
                    noDepthFbo = 0
                }

                associatedFboKeys.forEach { key ->
                    val fboGlId = cachedFbos.removeInt(key)
                    if (fboGlId != 0) {
                        deleteFbo(fboGlId)
                        val other = if (key.color == this) key.depth else key.color
                        other.associatedFboKeys.remove(key)
                    }
                }
                associatedFboKeys.clear()

                untrustedFbos.values.forEach(::deleteFbo)
                untrustedFbos.clear()

                // Note: Using the key+value version of `remove` because `FboCacheHook`s are also created for
                //       texture views on 1.21.11+, but we don't want to un-track a texture just because one of its
                //       views was closed.
                trackedTextures.remove(texture.glId, this as Any)

                return IntLists.emptyList()
            }

        //
        // Unused methods (potentially used by mods)
        //

        override fun get(key: Int): Int {
            if (key == 0) return noDepthFbo
            val depth = trackedTextures[key] ?: return 0
            return cachedFbos.getInt(FboKey(this, depth))
        }

        // We rely on `values` being called to clean up our `trackedTextures`, even when we don't have any associated
        // FBOs, so we'll pretend to never be empty, just in case a mod makes that call conditional on `isEmpty`/`size`.
        override fun isEmpty(): Boolean = false
        override val size: Int
            get() = 1

        override val keys: IntSet
            get() = IntSets.emptySet()

        override fun int2IntEntrySet(): ObjectSet<Int2IntMap.Entry> = ObjectSets.emptySet()
        override fun containsKey(key: Int): Boolean = false
        override fun containsValue(value: Int): Boolean = false
        override fun putAll(from: Map<out Int, Int>) = throw UnsupportedOperationException()
        override fun defaultReturnValue(rv: Int) = throw UnsupportedOperationException()
        override fun defaultReturnValue(): Int = 0
        @Suppress("RedundantOverride") // otherwise kotlinc complains about a "Inherited platform declarations clash"
        override fun getOrDefault(key: Int?, defaultValue: Int?): Int? = super.getOrDefault(key, defaultValue)
    }
}

private val GlTexture_fboCacheField: UnsafeHacks.Accessor<GlTexture, Int2IntMap?> by lazy {
    val field = GlTexture::class.java.declaredFields.first { it.type == Int2IntMap::class.java }
    UnsafeHacks.makeAccessor(field)
}
private var GlTexture.fboCache: Int2IntMap?
    get() = GlTexture_fboCacheField.get(this)
    set(value) = GlTexture_fboCacheField.set(this, value)

//#if MC >= 1.21.11
//$$ private val dummyTexture by lazy {
//$$     object : GlTexture(0, "", com.mojang.blaze3d.textures.TextureFormat.RGBA8, 1, 1, 1, 1, 0) {}
//$$ }
//$$ private val GlTexture_firstFbo_Id_DepthId: List<java.lang.invoke.VarHandle> by lazy {
//$$     val cls = GlTexture::class.java
//$$     val lookup = java.lang.invoke.MethodHandles.lookup()
//$$     val privateLookup = java.lang.invoke.MethodHandles.privateLookupIn(cls, lookup)
//$$     cls.declaredFields
//$$         .filter { it.type == Int::class.java && java.lang.reflect.AccessFlag.STATIC !in it.accessFlags() }
//$$         .filter { it.isAccessible = true; it.get(dummyTexture) == -1 }
//$$         .map { privateLookup.unreflectVarHandle(it) }
//$$ }
//$$ private var GlTexture.firstFboId: Int // "framebufferId" in yarn 1.21.11
//$$     get() = GlTexture_firstFbo_Id_DepthId[0].get(this) as Int
//$$     set(value) = GlTexture_firstFbo_Id_DepthId[0].set(this, value)
//$$ private var GlTexture.firstFboDepthId: Int // "depthGlId" in yarn 1.21.11
//$$     get() = GlTexture_firstFbo_Id_DepthId[1].get(this) as Int
//$$     set(value) = GlTexture_firstFbo_Id_DepthId[1].set(this, value)
//#endif

//#if MC >= 1.21.11
//$$ private val GlTextureView_fboCacheField: UnsafeHacks.Accessor<GlTextureView, Int2IntMap?> by lazy {
//$$     val field = GlTextureView::class.java.declaredFields.first { it.type == Int2IntMap::class.java }
//$$     UnsafeHacks.makeAccessor(field)
//$$ }
//$$ private var GlTextureView.fboCache: Int2IntMap?
//$$     get() = GlTextureView_fboCacheField.get(this)
//$$     set(value) = GlTextureView_fboCacheField.set(this, value)
//$$
//$$ private val GlTextureView_firstFbo_Id_DepthId: List<java.lang.invoke.VarHandle> by lazy {
//$$     val dummyInstance = object : GlTextureView(dummyTexture, 0, 1) {}
//$$     val cls = GlTextureView::class.java
//$$     val lookup = java.lang.invoke.MethodHandles.lookup()
//$$     val privateLookup = java.lang.invoke.MethodHandles.privateLookupIn(cls, lookup)
//$$     cls.declaredFields
//$$         .filter { it.type == Int::class.java && java.lang.reflect.AccessFlag.STATIC !in it.accessFlags() }
//$$         .filter { it.isAccessible = true; it.get(dummyInstance) == -1 }
//$$         .map { privateLookup.unreflectVarHandle(it) }
//$$ }
//$$ private var GlTextureView.firstFboId: Int
//$$     get() = GlTextureView_firstFbo_Id_DepthId[0].get(this) as Int
//$$     set(value) = GlTextureView_firstFbo_Id_DepthId[0].set(this, value)
//$$ private var GlTextureView.firstFboDepthId: Int
//$$     get() = GlTextureView_firstFbo_Id_DepthId[1].get(this) as Int
//$$     set(value) = GlTextureView_firstFbo_Id_DepthId[1].set(this, value)
//#endif

//#endif
