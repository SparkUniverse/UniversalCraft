// MC 1.17 - 1.21.4
package gg.essential.universal.shader

import com.google.common.collect.ImmutableMap
import gg.essential.universal.UGraphics
import net.minecraft.client.gl.GlUniform
import net.minecraft.client.render.Shader
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormatElement
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import org.apache.commons.codec.digest.DigestUtils
import java.io.FileNotFoundException
import kotlin.NoSuchElementException

//#if MC>=12102
//$$ import net.minecraft.client.gl.CompiledShader
//$$ import net.minecraft.client.gl.ShaderProgramDefinition
//#endif

//#if MC>=11903 && MC<12102
//$$ import gg.essential.universal.DummyPack
//#endif

//#if MC>=11900
//$$ import net.minecraft.resource.Resource
//$$ import java.util.Optional
//#else
import net.minecraft.resource.ResourceImpl
//#endif

internal class MCShader(
    val mc: Shader,
    private val blendState: BlendState
) : UShader {
    override var usable = true

    override fun bind() {
        UGraphics.Globals.setShader(::mc)

        // MC's GlBlendState is fundamentally broken because it is lazy in that it does not update anything
        // if the previously active blend state matches this one. But that assumes that it is the only method which
        // can modify the global GL state, which is just a horrible assumption and MC itself immediately violates
        // it in RenderLayer.
        // So, to actually get our state applied, we gotta do it ourselves.
        UGraphics.Globals.blendState(blendState)
    }

    override fun unbind() {
        UGraphics.Globals.setShader { null }
    }

    private fun getUniformOrNull(name: String) = mc.getUniform(name)?.let(::MCShaderUniform)
    override fun getIntUniformOrNull(name: String) = getUniformOrNull(name)
    override fun getFloatUniformOrNull(name: String) = getUniformOrNull(name)
    override fun getFloat2UniformOrNull(name: String) = getUniformOrNull(name)
    override fun getFloat3UniformOrNull(name: String) = getUniformOrNull(name)
    override fun getFloat4UniformOrNull(name: String) = getUniformOrNull(name)
    override fun getFloatMatrixUniformOrNull(name: String) = getUniformOrNull(name)
    override fun getSamplerUniformOrNull(name: String) = MCSamplerUniform(mc, name)

    companion object {
        private val DEBUG_LEGACY = System.getProperty("universalcraft.shader.legacy.debug", "") == "true"

        fun fromLegacyShader(vertSource: String, fragSource: String, blendState: BlendState, vertexFormat: VertexFormat?): MCShader {
            val transformer = ShaderTransformer(vertexFormat, 150)

            val transformedVertSource = transformer.transform(vertSource)
            val transformedFragSource = transformer.transform(fragSource)

            val json = """
                {
                    "blend": {
                        "func": "${blendState.equation.mcStr}",
                        "srcrgb": "${blendState.srcRgb.mcStr}",
                        "dstrgb": "${blendState.dstRgb.mcStr}",
                        "srcalpha": "${blendState.srcAlpha.mcStr}",
                        "dstalpha": "${blendState.dstAlpha.mcStr}"
                    },
                    "vertex": "${DigestUtils.sha1Hex(transformedVertSource).lowercase()}",
                    "fragment": "${DigestUtils.sha1Hex(transformedFragSource).lowercase()}",
                    "attributes": [ ${transformer.attributes.joinToString { "\"$it\"" }} ],
                    "samplers": [
                        ${transformer.samplers.joinToString(",\n") { "{ \"name\": \"$it\" }" }}
                    ],
                    "uniforms": [
                        ${transformer.uniforms.map { (name, type) -> """
                            { "name": "$name", "type": "${type.typeName}", "count": ${type.default.size}, "values": [ ${type.default.joinToString()} ] }
                        """.trimIndent() }.joinToString(",\n")}
                    ]
                }
            """.trimIndent()

            if (DEBUG_LEGACY) {
                println(transformedVertSource)
                println(transformedFragSource)
                println(json)
            }

            //#if MC<12102
            val factory = { id: Identifier ->
                val content = when {
                    id.path.endsWith(".json") -> json
                    id.path.endsWith(".vsh") -> transformedVertSource
                    id.path.endsWith(".fsh") -> transformedFragSource
                    else -> throw FileNotFoundException(id.toString())
                }
                //#if MC>=11903
                //$$ Optional.of(Resource(DummyPack, content::byteInputStream))
                //#elseif MC>=11900
                //$$ Optional.of(Resource("__generated__", content::byteInputStream))
                //#else
                ResourceImpl("__generated__", id, content.byteInputStream(), null)
                //#endif
            }
            //#endif

            fun buildVertexFormat(elements: Map<String, VertexFormatElement>): VertexFormat {
                //#if MC>=12100
                //$$ val builder = VertexFormat.builder()
                //$$ elements.forEach { (name, element) -> builder.add(name, element) }
                //$$ return builder.build()
                //#else
                return VertexFormat(ImmutableMap.copyOf(elements))
                //#endif
            }

            val shaderVertexFormat = if (vertexFormat != null) {
                // Shader calls glBindAttribLocation using the names in the VertexFormat, not the shader json...
                // Easiest way to work around this is to construct a custom VertexFormat with our prefixed names.
                buildVertexFormat(transformer.attributes.withIndex()
                        .associate { it.value to vertexFormat.elements[it.index] })
            } else {
                // Legacy fallback: The actual element doesn't matter here, Shader only cares about the names
                buildVertexFormat(transformer.attributes.associateWith {
                    //#if MC>=12100
                    //$$ VertexFormatElement.POSITION
                    //#else
                    VertexFormats.POSITION_ELEMENT
                    //#endif
                })
            }


            //#if MC>=12102
            //$$ val shaderProgram = ShaderProgram.create(
            //$$     CompiledShader.compile(
            //$$         Identifier.of("universalcraft", DigestUtils.sha1Hex(transformedVertSource).lowercase()),
            //$$         CompiledShader.Type.VERTEX, transformedVertSource
            //$$     ),
            //$$     CompiledShader.compile(
            //$$         Identifier.of("universalcraft", DigestUtils.sha1Hex(transformedFragSource).lowercase()),
            //$$         CompiledShader.Type.FRAGMENT, transformedFragSource
            //$$     ),
            //$$     shaderVertexFormat,
            //$$ )
            //$$ shaderProgram.set(transformer.uniforms.map { (name, type) ->
            //$$     ShaderProgramDefinition.Uniform(name, type.typeName, type.default.size, type.default.map { it.toFloat() })
            //$$ }, transformer.samplers.map { name -> ShaderProgramDefinition.Sampler(name) })
            //$$ return MCShader(shaderProgram, blendState)
            //#else
            val name = DigestUtils.sha1Hex(json).lowercase()
            //#if FORGELIKE
            //$$ @Suppress("DEPRECATION") // Forge wants us to use its overload, but we don't care
            //#endif
            return MCShader(Shader(factory, name, shaderVertexFormat), blendState)
            //#endif
        }
    }
}

internal class MCShaderUniform(val mc: GlUniform) : ShaderUniform, IntUniform, FloatUniform, Float2Uniform, Float3Uniform, Float4Uniform, FloatMatrixUniform {
    override val location: Int
        get() = mc.location

    override fun setValue(value: Int) = mc.set(value)

    override fun setValue(value: Float) = mc.set(value)

    override fun setValue(v1: Float, v2: Float) = mc.set(v1, v2)

    override fun setValue(v1: Float, v2: Float, v3: Float) = mc.set(v1, v2, v3)

    override fun setValue(v1: Float, v2: Float, v3: Float, v4: Float) = mc.set(v1, v2, v3, v4)

    override fun setValue(array: FloatArray) = mc.set(array)
}

internal class MCSamplerUniform(val mc: Shader, val name: String) : SamplerUniform {
    override val location: Int = 0

    override fun setValue(textureId: Int) {
        mc.addSampler(name, textureId)
    }
}
