package gg.essential.universal.render

import org.jetbrains.annotations.ApiStatus.NonExtendable

//#if MC >= 1.21.11 && !STANDALONE
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#endif

@NonExtendable
sealed interface UGpuSampler {
    enum class AddressMode {
        REPEAT,
        CLAMP_TO_EDGE,
        ;
    }

    enum class FilterMode {
        NEAREST,
        LINEAR,
        ;
    }

    companion object {
        @JvmName("get")
        operator fun invoke(
            addressModeU: AddressMode,
            addressModeV: AddressMode,
            minFilter: FilterMode,
            magFilter: FilterMode,
            useMipmaps: Boolean,
        ): UGpuSampler {
            //#if MC >= 1.21.11 && !STANDALONE
            //$$ return UGpuSamplerImpl(RenderSystem.getSamplerCache()
            //$$     .get(addressModeU.mc, addressModeV.mc, minFilter.mc, magFilter.mc, useMipmaps))
            //#else
            return UGpuSamplerImpl(addressModeU, addressModeV, minFilter, magFilter, useMipmaps)
            //#endif
        }

        //#if MC >= 1.21.11 && !STANDALONE
        //$$ private val AddressMode.mc: com.mojang.blaze3d.textures.AddressMode
        //$$     get() = when (this) {
        //$$         AddressMode.REPEAT -> com.mojang.blaze3d.textures.AddressMode.REPEAT
        //$$         AddressMode.CLAMP_TO_EDGE -> com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE
        //$$     }
        //$$ private val FilterMode.mc: com.mojang.blaze3d.textures.FilterMode
        //$$     get() = when (this) {
        //$$         FilterMode.NEAREST -> com.mojang.blaze3d.textures.FilterMode.NEAREST
        //$$         FilterMode.LINEAR -> com.mojang.blaze3d.textures.FilterMode.LINEAR
        //$$     }
        //#endif
    }
}
