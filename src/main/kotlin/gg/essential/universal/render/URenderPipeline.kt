package gg.essential.universal.render

import gg.essential.universal.UGraphics
import gg.essential.universal.UGraphics.CommonVertexFormats
import gg.essential.universal.UGraphics.DrawMode
import gg.essential.universal.shader.BlendState
import org.lwjgl.opengl.GL11

//#if STANDALONE
//$$ import gg.essential.universal.shader.GlShader
//$$ import gg.essential.universal.shader.UShader
//$$ import gg.essential.universal.standalone.render.DefaultShader
//$$ import gg.essential.universal.standalone.render.VertexFormat
//$$ import gg.essential.universal.vertex.UBuiltBufferInternal
//$$ typealias ResourceLocation = String
//#else
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.util.ResourceLocation

//#if MC>=12105
//$$ import com.mojang.blaze3d.vertex.VertexFormatElement
//$$ import com.mojang.blaze3d.pipeline.BlendFunction
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline
//#if MC >= 26.1
//$$ import com.mojang.blaze3d.pipeline.ColorTargetState
//$$ import com.mojang.blaze3d.pipeline.DepthStencilState
//$$ import com.mojang.blaze3d.platform.CompareOp
//#else
//$$ import com.mojang.blaze3d.platform.DepthTestFunction
//$$ import com.mojang.blaze3d.platform.LogicOp
//#endif
//$$ import com.mojang.blaze3d.shaders.ShaderType
//$$ import com.mojang.blaze3d.systems.RenderPass
//$$ import gg.essential.universal.shader.ShaderTransformer
//#if MC < 26.1
//$$ import net.minecraft.client.gl.GlResourceManager
//#endif
//$$ import net.minecraft.client.gl.UniformType
//$$ import net.minecraft.client.render.BuiltBuffer
//$$ import org.apache.commons.codec.digest.DigestUtils
//#else
import gg.essential.universal.shader.UShader
import gg.essential.universal.vertex.UBuiltBufferInternal
import net.minecraft.client.renderer.WorldVertexBufferUploader
//#endif

//#if MC>=12102
//$$ import net.minecraft.client.MinecraftClient
//#if MC<12105
//$$ import net.minecraft.client.gl.ShaderProgramKey;
//#endif
//#endif

//#if MC>=11700
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#if MC<12105
//$$ import gg.essential.universal.shader.MCShader
//$$ import net.minecraft.client.render.Shader
//$$ import java.util.function.Supplier
//#endif
//#if MC<12100
//$$ import net.minecraft.client.gl.GlBlendState
//$$ import java.lang.invoke.MethodHandles
//#endif
//#else
import gg.essential.universal.shader.GlShader
import net.minecraft.client.renderer.vertex.VertexFormatElement
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.platform.GlStateManager
//#endif

//#endif

//#if MC>=12105 && !STANDALONE
//#if MC>=12111
//$$ typealias ShaderSourceGetter = net.minecraft.client.gl.ShaderSourceGetter
//#else
//$$ typealias ShaderSourceGetter = java.util.function.BiFunction<Identifier, ShaderType, String?>
//#endif
//#endif

class URenderPipeline private constructor(
    private val id: ResourceLocation,
    internal val format: VertexFormat,
    //#if STANDALONE
    //$$ private val drawMode: DrawMode,
    //#endif
    //#if MC>=12105 && !STANDALONE
    //$$ private var shaderSourceGetter: ShaderSourceGetter?,
    //$$ internal val mcRenderPipeline: RenderPipeline,
    //#else
    private val shader: ShaderSupplier?,
    internal val glState: ManagedGlState,
    //#endif
) {
    //#if MC>=12105 && !STANDALONE
    //$$ internal fun draw(renderPass: RenderPass, builtBuffer: BuiltBuffer) {
    //$$     if (shaderSourceGetter != null) {
    //$$         // Supply our shader sources to the render backend, need to do this each draw (it'll no-op if it's already
    //$$         // cached) because resource reloads will clear it again.
    //$$         RenderSystem.getDevice().precompilePipeline(mcRenderPipeline, shaderSourceGetter)
    //$$     }
    //$$     renderPass.setPipeline(mcRenderPipeline)
    //$$
        //#if MC>=12106
        //$$ renderPass.drawIndexed(0, 0, builtBuffer.drawParameters.indexCount, 1)
        //#else
        //$$ renderPass.drawIndexed(0, builtBuffer.drawParameters.indexCount())
        //#endif
    //$$ }
    //#else
    internal fun draw(builtBuffer: UBuiltBufferInternal) {
        //#if STANDALONE
        //$$ UGraphics.RENDERER.draw(builtBuffer.mc, drawMode, if (shader == null) DefaultShader.get(format.parts) else null)
        //#else
        val mcBuiltBuffer = builtBuffer.mc
        //#if MC>=11900
        //$$ BufferRenderer.drawWithShader(mcBuiltBuffer)
        //#elseif MC>=11600
        //$$ WorldVertexBufferUploader.draw(mcBuiltBuffer)
        //#else
        WorldVertexBufferUploader().draw(mcBuiltBuffer)
        //#endif
        //#endif
        //#if MC>=11900 && MC<12100
        //$$ builtBuffer.closedExternally()
        //#endif
    }

    internal fun bind() {
        shader?.bind(glState.blendState)

        // These seemingly pointless calls are used to bypass the builtin `GlBlendState` applied by Minecraft's
        // `ShaderProgram` class right before the actual draw call (so no way for us to change it afterwards) on these
        // versions.
        // This is so we can apply the BlendState of this URenderPipeline, even when using a pre-existing ShaderProgram
        // (such as all the default programs) which has a different builtin `GlBlendState`.
        // This works by exploiting one of the (many) bugs of `GlBlendState`, namely that it is lazy in that it does
        // not update anything if the previously active blend state matches the new one. In general that behavior is
        // wrong because there's plenty other things which update the global GL state but we use this to our advantage
        // here: We'll apply its GlBlendState now, and then revert the global GL state to our expected state, so that
        // later when MC calls `ShaderProgram.bind` right before the actual draw, its `GlBlendState.enable` call will
        // no-op and our desired blend state will stay active.
        // (Prior to 1.17, we only support the fixed-function pipeline and our own shader class which we call with
        //  `skipBlendState = true`, so it doesn't mess with the global blend state at all)
        // (On 1.21+, MC's `GlBlendState` is still broken, but `ShaderProgram` no longer applies it)
        //#if MC>=11700 && MC<12100
        //$$ RenderSystem.getShader()?.let { shaderProgram ->
        //$$     // Capture actual desired blend state to restore afterwards
        //$$     val prevBlendState = BlendState.active()
        //$$     // This seemingly pointless change of active blend state here is necessary because of another bug in
        //$$     // GlBlendState: It'll only update its global `activeBlendState` when the blend state is enabled or disabled
        //$$     // but not when merely the blend function or one of its parameters is changed. So we need to make sure
        //$$     // we enable a blend state which is the opposite of the blend state of the shader before we enable that one.
        //$$     // We need to do two calls because we don't know the state of the current `activeBlendState`.
        //$$     if (shaderProgram.glBlendState.isBlendDisabled) {
        //$$         BlendState.DISABLED.mc.enable()
        //$$         BlendState.ALPHA.mc.enable()
        //$$     } else {
        //$$         BlendState.ALPHA.mc.enable()
        //$$         BlendState.DISABLED.mc.enable()
        //$$     }
        //$$     // Enable shader's GlBlendState so it becomes the `GlBlendState.activeBlendState`
        //$$     shaderProgram.glBlendState.enable()
        //$$     // Restore desired blend state
        //$$     prevBlendState.activateWithoutChangingMcBlendState()
        //$$ }
        //#endif
    }

    internal fun unbind() {
        shader?.unbind()
    }

    internal fun uniform(name: String, vararg values: Float) {
        when (shader) {
            is ShaderSupplier.LegacySource -> {
                val shader = shader.shader
                when (values.size) {
                    1 -> shader.getFloatUniformOrNull(name)?.setValue(values[0])
                    2 -> shader.getFloat2UniformOrNull(name)?.setValue(values[0], values[1])
                    3 -> shader.getFloat3UniformOrNull(name)?.setValue(values[0], values[1], values[2])
                    4 -> shader.getFloat4UniformOrNull(name)?.setValue(values[0], values[1], values[2], values[3])
                    9, 16 -> shader.getFloatMatrixUniformOrNull(name)?.setValue(values)
                    else -> throw UnsupportedOperationException()
                }
            }
            //#if MC>=11700 && !STANDALONE
            //$$ is ShaderSupplier.Mc -> {
            //$$     val shader = shader.shader.get()
            //$$     shader.getUniform(name)?.set(values)
            //$$     return
            //$$ }
            //#endif
            null -> throw IllegalStateException()
        }
    }

    internal fun uniform(name: String, vararg values: Int) {
        when (shader) {
            is ShaderSupplier.LegacySource -> {
                val shader = shader.shader
                when (values.size) {
                    1 -> shader.getIntUniformOrNull(name)?.setValue(values[0])
                    else -> throw UnsupportedOperationException()
                }
            }
            //#if MC>=11700 && !STANDALONE
            //$$ is ShaderSupplier.Mc -> {
            //$$     val shader = shader.shader.get()
            //$$     shader.getUniform(name)?.set(values[0])
            //$$     return
            //$$ }
            //#endif
            null -> throw IllegalStateException()
        }
    }

    internal fun texture(name: String, glId: Int) {
        when (shader) {
            is ShaderSupplier.LegacySource -> {
                shader.shader.getSamplerUniformOrNull(name)?.setValue(glId)
            }
            //#if MC>=11700 && !STANDALONE
            //$$ is ShaderSupplier.Mc -> {
            //$$     val index = name.removePrefix("Sampler").toIntOrNull() ?: return
            //$$     RenderSystem.setShaderTexture(index, glId)
            //$$     return
            //$$ }
            //#endif
            null -> throw IllegalStateException()
        }
    }

    internal fun texture(index: Int, glId: Int) {
        when (shader) {
            is ShaderSupplier.LegacySource -> {
                shader.shader.getSamplerUniformOrNull("Sampler$index")?.setValue(glId)
            }
            //#if MC>=11700 && !STANDALONE
            //$$ is ShaderSupplier.Mc -> {
            //$$     RenderSystem.setShaderTexture(index, glId)
            //$$     return
            //$$ }
            //#endif
            null -> UGraphics.Globals.bindTexture(index, glId)
        }
    }
    //#endif

    override fun toString(): String {
        return id.toString()
    }

    enum class DepthTest {
        Disabled,
        Always,
        Equal,
        LessOrEqual,
        Less,
        Greater,
    }

    enum class ColorLogic {
        None,
        OrReverse,
    }

    private sealed interface ShaderSupplier {
        //#if MC<12105 || STANDALONE
        fun bind(blendState: BlendState)
        fun unbind()
        //#endif

        class LegacySource(val vertexFormat: VertexFormat, val vertSource: String, val fragSource: String) : ShaderSupplier {
            //#if MC<12105 || STANDALONE
            lateinit var shader: UShader

            override fun bind(blendState: BlendState) {
                if (!::shader.isInitialized) {
                    shader = UShader.fromLegacyShader(vertSource, fragSource, blendState, vertexFormat)
                }

                //#if MC>=12102 && !STANDALONE
                //$$ RenderSystem.setShader((shader as MCShader).mc)
                //#elseif MC>=11700 && !STANDALONE
                //$$ RenderSystem.setShader { (shader as MCShader).mc }
                //#else
                (shader as GlShader).bind(skipBlendState = true)
                //#endif
            }

            override fun unbind() {
                //#if MC>=12102 && !STANDALONE
                //$$ RenderSystem.clearShader()
                //#elseif MC>=11700 && !STANDALONE
                //$$ RenderSystem.setShader { null }
                //#else
                (shader as GlShader).unbind(skipBlendState = true)
                //#endif
            }
            //#endif
        }

        //#if MC>=11700 && !STANDALONE
        //#if MC>=12105
        //$$ class Mc(val vert: Identifier, val frag: Identifier, val samplers: List<String>, val uniforms: Map<String, UniformType>) : ShaderSupplier
        //#else
        //$$ class Mc(val shader: Supplier<Shader>) : ShaderSupplier {
        //$$     override fun bind(blendState: BlendState) {
                //#if MC>=12102
                //$$ RenderSystem.setShader(shader.get())
                //#else
                //$$ RenderSystem.setShader { shader.get() }
                //#endif
        //$$     }
        //$$
        //$$     override fun unbind() {
                //#if MC>=12102
                //$$ RenderSystem.clearShader()
                //#else
                //$$ RenderSystem.setShader { null }
                //#endif
        //$$     }
        //$$ }
        //#endif
        //#endif
    }

    interface BuilderProps {
        var depthTest: DepthTest
        var culling: Boolean
        @Deprecated("Unsupported as of Minecraft 26.1")
        var colorLogic: ColorLogic
        var blendState: BlendState
        var colorMask: Pair</*rgb*/Boolean, /*alpha*/Boolean>
        var depthMask: Boolean
        var polygonOffset: Pair</*factor*/Float, /*units*/Float>
    }

    interface Builder : BuilderProps {
        fun build(): URenderPipeline
    }

    private data class BuilderPropsImpl(
        override var depthTest: DepthTest,
        override var culling: Boolean,
        @Deprecated("Unsupported as of Minecraft 26.1")
        override var colorLogic: ColorLogic,
        override var blendState: BlendState,
        override var colorMask: Pair<Boolean, Boolean>,
        override var depthMask: Boolean,
        override var polygonOffset: Pair<Float, Float>,
    ) : BuilderProps {
        constructor() : this(
            depthTest = DepthTest.Disabled,
            culling = false,
            colorLogic = ColorLogic.None,
            blendState = BlendState.DISABLED,
            colorMask = enabledColorMask,
            depthMask = true,
            polygonOffset = zeroPolygonOffset,
        )

        private companion object {
            private val enabledColorMask = Pair(true, true)
            private val zeroPolygonOffset = Pair(0f, 0f)
        }
    }

    private class BuilderImpl(
        private val id: ResourceLocation,
        private val drawMode: DrawMode,
        private val format: VertexFormat,
        //#if MC>=12105 && !STANDALONE
        //$$ private val shader: ShaderSupplier,
        //#else
        private val shader: ShaderSupplier?,
        //#endif
    ) : Builder, BuilderProps by BuilderPropsImpl() {
        override fun build(): URenderPipeline {
            //#if MC>=12105 && !STANDALONE
            //$$ var shaderSourceGetter: ShaderSourceGetter? = null
            //$$ var mcRenderPipeline = RenderPipeline.builder().apply {
            //$$     withLocation(id)
            //$$     withVertexFormat(format, drawMode.mcMode)
            //$$     when (shader) {
            //$$         is ShaderSupplier.LegacySource -> {
            //$$             val transformer = ShaderTransformer(format, 150)
            //$$
            //$$             val transformedVertSource = transformer.transform(shader.vertSource)
            //$$             val transformedFragSource = transformer.transform(shader.fragSource)
            //$$
            //$$             val vertId = Identifier.of("universalcraft", "shader/generated/" + DigestUtils.sha1Hex(transformedVertSource).lowercase())
            //$$             val fragId = Identifier.of("universalcraft", "shader/generated/" + DigestUtils.sha1Hex(transformedFragSource).lowercase())
            //$$
            //$$             shaderSourceGetter = ShaderSourceGetter { id: Identifier, type: ShaderType ->
            //$$                 when (id) {
            //$$                     vertId -> transformedVertSource
            //$$                     fragId -> transformedFragSource
            //$$                     else -> MinecraftClient.getInstance().shaderLoader.getSource(id, type)
            //$$                 }
            //$$             }
            //$$
            //$$             withVertexShader(vertId)
            //$$             withFragmentShader(fragId)
            //$$
            //$$             transformer.samplers.forEach { withSampler(it) }
            //$$             transformer.uniforms.forEach { withUniform(it.key, it.value.mc) }
            //$$
            //$$             // ShaderProgram calls glBindAttribLocation using the names in the VertexFormat so we need to
            //$$             // construct a custom one based on the original but with our prefixed names
            //$$             val builder = VertexFormat.builder()
            //$$             var expectedOffset = 0
            //$$             format.elements.mapIndexed { index, element ->
            //$$                 val offset = format.getOffset(element)
            //$$                 val padding = offset - expectedOffset
            //$$                 if (padding > 0) {
            //$$                     expectedOffset += padding
            //$$                     builder.padding(padding)
            //$$                 }
            //$$                 expectedOffset += element.byteSize()
            //$$                 val name = transformer.attributes.getOrNull(index) ?: format.getElementName(element)
            //$$                 builder.add(name, element)
            //$$             }
            //$$             withVertexFormat(builder.build(), drawMode.mcMode)
            //$$         }
            //$$         is ShaderSupplier.Mc -> {
            //$$             withVertexShader(shader.vert)
            //$$             withFragmentShader(shader.frag)
            //$$
            //$$             shader.samplers.forEach { withSampler(it) }
            //$$             shader.uniforms.forEach { withUniform(it.key, it.value) }
            //$$         }
            //$$     }
            //#if MC >= 26.1
            //$$     if (depthTest != DepthTest.Disabled) {
            //$$         withDepthStencilState(DepthStencilState(when (depthTest) {
            //$$             DepthTest.Disabled -> throw AssertionError("unreachable")
            //$$             DepthTest.Always -> CompareOp.ALWAYS_PASS
            //$$             DepthTest.Equal -> CompareOp.EQUAL
            //$$             DepthTest.LessOrEqual -> CompareOp.LESS_THAN_OR_EQUAL
            //$$             DepthTest.Less -> CompareOp.LESS_THAN
            //$$             DepthTest.Greater -> CompareOp.GREATER_THAN
            //$$         }, depthMask, polygonOffset.first, polygonOffset.second))
            //$$     }
            //#else
            //$$     withDepthTestFunction(when (depthTest) {
            //$$         DepthTest.Disabled -> DepthTestFunction.NO_DEPTH_TEST
            //$$         DepthTest.Always -> DepthTestFunction.NO_DEPTH_TEST // implemented via raw GlStateManager below
            //$$         DepthTest.Equal -> DepthTestFunction.EQUAL_DEPTH_TEST
            //$$         DepthTest.LessOrEqual -> DepthTestFunction.LEQUAL_DEPTH_TEST
            //$$         DepthTest.Less -> DepthTestFunction.LESS_DEPTH_TEST
            //$$         DepthTest.Greater -> DepthTestFunction.GREATER_DEPTH_TEST
            //$$     })
            //$$     withDepthWrite(depthMask)
            //$$     polygonOffset.let { (factor, units) ->
            //$$         withDepthBias(factor, units)
            //$$     }
            //#endif
            //$$     withCull(culling)
            //#if MC >= 26.1
            //$$     withColorTargetState(ColorTargetState(
            //$$         java.util.Optional.ofNullable(if (blendState.enabled) BlendFunction(
            //$$             blendState.srcRgb.mcSourceFactor,
            //$$             blendState.dstRgb.mcDestFactor,
            //$$             blendState.srcAlpha.mcSourceFactor,
            //$$             blendState.dstAlpha.mcDestFactor,
            //$$         ) else null),
            //$$         colorMask.let { (colorMask, alphaMask) ->
            //$$             var flags = 0
            //$$             if (colorMask) flags += ColorTargetState.WRITE_COLOR
            //$$             if (alphaMask) flags += ColorTargetState.WRITE_ALPHA
            //$$             flags
            //$$         }
            //$$     ))
            //#else
            //$$     @Suppress("DEPRECATION")
            //$$     withColorLogic(when (colorLogic) {
            //$$         ColorLogic.None -> LogicOp.NONE
            //$$         ColorLogic.OrReverse -> LogicOp.OR_REVERSE
            //$$     })
            //$$     val blendState = blendState
            //$$     if (blendState.enabled) {
            //$$         withBlend(BlendFunction(
            //$$             blendState.srcRgb.mcSourceFactor,
            //$$             blendState.dstRgb.mcDestFactor,
            //$$             blendState.srcAlpha.mcSourceFactor,
            //$$             blendState.dstAlpha.mcDestFactor,
            //$$         ))
            //$$     } else {
            //$$         withoutBlend()
            //$$     }
            //$$     colorMask.let { (colorMask, alphaMask) ->
            //$$         withColorWrite(colorMask, alphaMask)
            //$$     }
            //#endif
            //$$ }.build()
            //$$
            //#if MC < 26.1
            //$$ if (depthTest == DepthTest.Always) {
            //$$     abstract class CustomRenderPipeline(inner: RenderPipeline) : RenderPipeline(
            //$$         inner.location, inner.vertexShader, inner.fragmentShader, inner.shaderDefines, inner.samplers, inner.uniforms, inner.blendFunction, inner.depthTestFunction, inner.polygonMode, inner.isCull, inner.isWriteColor, inner.isWriteAlpha, inner.isWriteDepth, inner.colorLogic, inner.vertexFormat, inner.vertexFormatMode, inner.depthBiasScaleFactor, inner.depthBiasConstant,
                    //#if MC>=12106
                    //$$ inner.sortKey,
                    //#endif
            //$$     )
            //$$     mcRenderPipeline = object : CustomRenderPipeline(mcRenderPipeline) {
            //$$         val stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            //$$         override fun isCull(): Boolean {
            //$$             if (stackWalker.callerClass == GlResourceManager::class.java) {
            //$$                 GlStateManager._enableDepthTest()
            //$$                 GlStateManager._depthFunc(GL11.GL_ALWAYS)
            //$$             }
            //$$             return super.isCull()
            //$$         }
            //$$     }
            //$$ }
            //#endif
            //$$
            //$$ return URenderPipeline(
            //$$     id,
            //$$     format,
            //$$     shaderSourceGetter,
            //$$     mcRenderPipeline,
            //$$ )
            //#else
            return URenderPipeline(
                id,
                format,
                //#if STANDALONE
                //$$ drawMode,
                //#endif
                shader,
                ManagedGlState(
                    depthTest = depthTest != DepthTest.Disabled,
                    depthFunc = when (depthTest) {
                        DepthTest.Disabled -> GL11.GL_LESS
                        DepthTest.Always -> GL11.GL_ALWAYS
                        DepthTest.Equal -> GL11.GL_EQUAL
                        DepthTest.LessOrEqual -> GL11.GL_LEQUAL
                        DepthTest.Less -> GL11.GL_LESS
                        DepthTest.Greater -> GL11.GL_GREATER
                    },
                    culling = culling,
                    colorLogicOp = @Suppress("DEPRECATION") colorLogic != ColorLogic.None,
                    colorLogicOpMode = @Suppress("DEPRECATION") when (colorLogic) {
                        ColorLogic.None -> GL11.GL_COPY
                        ColorLogic.OrReverse -> GL11.GL_OR_REVERSE
                    },
                    blendState = blendState,
                    colorMask = listOf(colorMask.first, colorMask.first, colorMask.first, colorMask.second),
                    depthMask = depthMask,
                    polygonOffset = polygonOffset.first != 0f || polygonOffset.second != 0f,
                    polygonOffsetFactor = polygonOffset.first,
                    polygonOffsetUnits = polygonOffset.second,
                    shadeModel = GL11.GL_SMOOTH,
                    alphaTest = true,
                    alphaTestFunc = GL11.GL_ALWAYS,
                    alphaTestRef = 0f,
                    colorR = 1f,
                    colorG = 1f,
                    colorB = 1f,
                    colorA = 1f,
                    //#if MC>=11700
                    //$$ texture2DStates = mutableListOf(),
                    //#else
                    // Vanilla only ever uses two samplers, so we can assume the remainder to be disabled by default
                    // and don't need to check them unless we want them enabled.
                    texture2DStates = mutableListOf(false, false).also { list ->
                        for (elem in format.elements) {
                            if (elem.usage != VertexFormatElement.EnumUsage.UV) continue
                            while (elem.index !in list.indices) list.add(false)
                            list[elem.index] = true
                        }
                    },
                    //#endif
                ),
            )
            //#endif
        }
    }

    companion object {
        //#if MC>=12105 && !STANDALONE
        //$$ val isRequired = true
        //#else
        val isRequired = false
        //#endif

        //#if MC>=11700 && MC<12100
        //$$ private val Shader_glBlendState: (Shader) -> GlBlendState by lazy {
        //$$     try {
        //$$         val field = Shader::class.java.declaredFields
        //$$             .first { it.type == GlBlendState::class.java }
        //$$         field.isAccessible = true
        //$$         val handle = MethodHandles.lookup().unreflectGetter(field)
        //$$         return@lazy { shader: Shader -> handle.invoke(shader) as GlBlendState }
        //$$     } catch (e: Exception) {
        //$$         e.printStackTrace()
        //$$         return@lazy { GlBlendState() }
        //$$     }
        //$$ }
        //$$ private val Shader.glBlendState: GlBlendState get() = Shader_glBlendState(this)
        //#endif

        //#if !STANDALONE
        //#if MC>=12105
        //$$ @JvmStatic
        //$$ fun wrap(mc: RenderPipeline): URenderPipeline =
        //$$     URenderPipeline(mc.location, mc.vertexFormat, null, mc)
        //$$ fun builder(id: Identifier, drawMode: DrawMode, format: VertexFormat, vert: Identifier, frag: Identifier, samplers: List<String>, uniforms: Map<String, UniformType>): Builder {
        //$$     return BuilderImpl(id, drawMode, format, ShaderSupplier.Mc(vert, frag, samplers, uniforms))
        //$$ }
        //#else
        //#if MC>=11700
        //$$ fun builder(id: Identifier, drawMode: DrawMode, format: VertexFormat, shader: Supplier<Shader>?): Builder {
        //$$     return BuilderImpl(id, drawMode, format, shader?.let { ShaderSupplier.Mc(it) })
        //$$ }
        //#endif
        //#if MC>=12102
        //$$ fun builder(id: Identifier, drawMode: DrawMode, format: VertexFormat, shader: ShaderProgramKey): Builder {
        //$$     val shaderSupplier = Supplier { MinecraftClient.getInstance().shaderLoader.getProgramToLoad(shader) }
        //$$     return BuilderImpl(id, drawMode, format, ShaderSupplier.Mc(shaderSupplier))
        //$$ }
        //#endif
        //#endif
        //#endif

        fun builderWithDefaultShader(id: String, drawMode: DrawMode, format: CommonVertexFormats): Builder {
            //#if STANDALONE
            //$$ return BuilderImpl(id, drawMode, format.mc, null)
            //#else
            //#if MC>=12100
            //$$ val mcId = Identifier.of(id)
            //#else
            val mcId = ResourceLocation(id)
            //#endif
            //#if MC>=11700
            //$$ val shader = UGraphics.DEFAULT_SHADERS[format.mc]
                //#if MC>=12102 && MC<12105
                //$$ ?.let { Supplier { MinecraftClient.getInstance().shaderLoader.getProgramToLoad(it) } }
                //#endif
            //$$     ?: throw IllegalArgumentException("No default shader for $format.")
            //#if MC>=12105
            //$$ val shaderId = Identifier.ofVanilla(shader)
            //$$ val samplers = List(format.mc.elements.count {
                //#if MC >= 26.1
                //$$ it == VertexFormatElement.UV0 || it == VertexFormatElement.UV1 || it == VertexFormatElement.UV2
                //#else
                //$$ it.usage == VertexFormatElement.Usage.UV
                //#endif
            //$$ }) { i -> "Sampler$i" }
            //$$ val uniforms = mapOf(
                //#if MC>=12106
                //$$ "DynamicTransforms" to UniformType.UNIFORM_BUFFER,
                //$$ "Projection" to UniformType.UNIFORM_BUFFER,
                //#else
                //$$ "ModelViewMat" to UniformType.MATRIX4X4,
                //$$ "ProjMat" to UniformType.MATRIX4X4,
                //$$ "ColorModulator" to UniformType.VEC4,
                //#endif
            //$$ )
            //$$ return builder(mcId, drawMode, format.mc, shaderId, shaderId, samplers, uniforms)
            //#else
            //$$ return builder(mcId, drawMode, format.mc, shader)
            //#endif
            //#else
            return BuilderImpl(mcId, drawMode, format.mc, null)
            //#endif
            //#endif
        }

        fun builderWithLegacyShader(id: String, drawMode: DrawMode, format: CommonVertexFormats, vertSource: String, fragSource: String): Builder {
            return builderWithLegacyShader(id, drawMode, format.mc, vertSource, fragSource)
        }

        fun builderWithLegacyShader(id: String, drawMode: DrawMode, format: VertexFormat, vertSource: String, fragSource: String): Builder {
            //#if STANDALONE
            //$$ val mcId = id
            //#elseif MC>=12100
            //$$ val mcId = Identifier.of(id)
            //#else
            val mcId = ResourceLocation(id)
            //#endif
            return BuilderImpl(mcId, drawMode, format, ShaderSupplier.LegacySource(format, vertSource, fragSource))
        }
    }
}
