package gg.essential.universal.shader

//#if STANDALONE
//$$ import gg.essential.universal.standalone.render.VertexFormat
//#else
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.renderer.vertex.VertexFormatElement

//#if MC >= 26.2
//$$ import com.mojang.blaze3d.GpuFormat
//#endif
//#endif

internal class ShaderTransformer(private val vertexFormat: VertexFormat?, private val targetVersion: Int) {
    init {
        check(targetVersion in listOf(110, 130, 150))
    }

    val attributes = mutableListOf<String>()
    val samplers = mutableSetOf<String>()
    val uniforms = mutableMapOf<String, UniformType>()

    fun transform(originalSource: String): String {
        var source = originalSource

        source = source.replace("gl_ModelViewProjectionMatrix", "gl_ProjectionMatrix * gl_ModelViewMatrix")
        if (targetVersion >= 130) {
            source = source.replace("texture2D", "texture")
        }

        val replacements = mutableMapOf<String, String>()
        val transformed = mutableListOf<String>()
        transformed.add("#version $targetVersion")

        val frag = "gl_FragColor" in source
        val vert = !frag

        val attributeIn = if (targetVersion >= 130) "in" else "attribute"
        val varyingIn = if (targetVersion >= 130) "in" else "varying"
        val varyingOut = if (targetVersion >= 130) "out" else "varying"

        if (frag && targetVersion >= 130) {
            transformed.add("$varyingOut vec4 uc_FragColor;")
            replacements["gl_FragColor"] = "uc_FragColor"
        }

        if (vert && "gl_FrontColor" in source) {
            transformed.add("$varyingOut vec4 uc_FrontColor;")
            replacements["gl_FrontColor"] = "uc_FrontColor"
        }
        if (frag && "gl_Color" in source) {
            transformed.add("$varyingIn vec4 uc_FrontColor;")
            replacements["gl_Color"] = "uc_FrontColor"
        }

        fun replaceAttribute(newAttributes: MutableList<Pair<String, String>>, needle: String, type: String, replacementName: String = "uc_" + needle.substringAfter("_"), replacement: String = replacementName) {
            if (needle in source) {
                replacements[needle] = replacement
                newAttributes.add(replacementName to "$attributeIn $type $replacementName;")
            }
        }
        if (vert) {
            //#if STANDALONE
            //$$ fun uvVecType(index: Int) = "vec2"
            //#else
            fun uvVecType(index: Int) =
                if (vertexFormat?.uvIsFloat(index) ?: true) "vec2" else "ivec2"
            //#endif

            val newAttributes = mutableListOf<Pair<String, String>>()
            replaceAttribute(newAttributes, "gl_Vertex", "vec3", "uc_Position", replacement = "vec4(uc_Position, 1.0)")
            replaceAttribute(newAttributes, "gl_Color", "vec4")
            replaceAttribute(newAttributes, "gl_MultiTexCoord0.st", uvVecType(0), "uc_UV0")
            replaceAttribute(newAttributes, "gl_MultiTexCoord1.st", uvVecType(1), "uc_UV1")
            replaceAttribute(newAttributes, "gl_MultiTexCoord2.st", uvVecType(2), "uc_UV2")

            if (vertexFormat != null) {
                //#if MC>=11700 && !STANDALONE
                //#if MC >= 26.2
                //$$ fun orgIndexOfAttr(name: String) = vertexFormat.elements.indexOfFirst { it.name == name }
                //#else
                //$$ fun orgIndexOfAttr(name: String) = vertexFormat.shaderAttributes.indexOf(name)
                //#endif
                //$$ newAttributes.sortedBy { orgIndexOfAttr(it.first.removePrefix("uc_")) }
                //$$     .forEach {
                //$$         attributes.add(it.first)
                //$$         transformed.add(it.second)
                //$$     }
                //#else
                newAttributes.forEach {
                    attributes.add(it.first)
                    transformed.add(it.second)
                }
                //#endif
            } else {
                newAttributes.forEach {
                    attributes.add(it.first)
                    transformed.add(it.second)
                }
            }
        }

        //#if MC>=12106 && !STANDALONE
        //$$ transformed.add("""
        //$$     layout(std140) uniform Projection {
        //$$         mat4 ProjMat;
        //$$     };
        //$$     layout(std140) uniform DynamicTransforms {
        //$$         mat4 ModelViewMat;
        //$$         vec4 ColorModulator;
        //$$         vec3 ModelOffset;
        //$$         mat4 TextureMat;
        //$$         float LineWidth;
        //$$     };
        //$$ """.trimIndent())
        //$$ uniforms["Projection"] = UniformType.Mat4
        //$$ uniforms["DynamicTransforms"] = UniformType.Mat4
        //$$ replacements["gl_ModelViewMatrix"] = "ModelViewMat"
        //$$ replacements["gl_ProjectionMatrix"] = "ProjMat"
        //#else
        fun replaceUniform(needle: String, type: UniformType, replacementName: String, replacement: String = replacementName) {
            if (needle in source) {
                replacements[needle] = replacement
                if (replacementName !in uniforms) {
                    uniforms[replacementName] = type
                    transformed.add("uniform ${type.glslName} $replacementName;")
                }
            }
        }
        replaceUniform("gl_ModelViewMatrix", UniformType.Mat4, "ModelViewMat")
        replaceUniform("gl_ProjectionMatrix", UniformType.Mat4, "ProjMat")
        //#endif


        for (line in source.lines()) {
            transformed.add(when {
                line.startsWith("#version") -> continue
                line.startsWith("varying ") && targetVersion >= 130 -> (if (frag) "in " else "out ") + line.substringAfter("varying ")
                line.startsWith("uniform ") -> {
                    val (_, glslType, name) = line.trimEnd(';').split(" ")
                    if (glslType == "sampler2D") {
                        samplers.add(name)
                        line
                    } else {
                        uniforms[name] = UniformType.fromGlsl(glslType)
                        //#if MC>=12106 && !STANDALONE
                        //$$ replacements[name] = "u_$name"
                        //$$ "layout(std140) uniform $name { $glslType u_$name; };"
                        //#else
                        line
                        //#endif
                    }
                }
                else -> replacements.entries.fold(line) { acc, (needle, replacement) -> acc.replace(needle, replacement) }
            })
        }

        return transformed.joinToString("\n")
    }
}

//#if !STANDALONE
private fun VertexFormat.uvIsFloat(i: Int): Boolean =
    //#if MC >= 26.2
    //$$ this.getElement("UV$i")
    //#elseif MC >= 26.1
    //$$ this.elements.getOrNull(this.elementAttributeNames.indexOf("UV$i"))
    //#else
    this.elements
        .asSequence()
        .filter { it.usage == VertexFormatElement.EnumUsage.UV }
        .elementAtOrNull(i)
    //#endif
        ?.isFloat()
        ?: true

private fun VertexFormatElement.isFloat() =
    //#if MC >= 26.2
    //$$ this.format == GpuFormat.RG16_FLOAT || this.format == GpuFormat.RG32_FLOAT
    //#else
    this.type == VertexFormatElement.EnumType.FLOAT
    //#endif
//#endif

internal enum class UniformType(val typeName: String, val glslName: String, val default: IntArray) {
    Int1("int", "int", intArrayOf(0)),
    Float1("float", "float", intArrayOf(0)),
    Float2("float", "vec2", intArrayOf(0, 0)),
    Float3("float", "vec3", intArrayOf(0, 0, 0)),
    Float4("float", "vec4", intArrayOf(0, 0, 0, 0)),
    Mat2("matrix2x2", "mat2", intArrayOf(1, 0, 0, 1)),
    Mat3("matrix3x3", "mat3", intArrayOf(1, 0, 0, 0, 1, 0, 0, 0, 1)),
    Mat4("matrix4x4", "mat4", intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)),
    ;

    //#if MC>=12105 && !STANDALONE
    //$$ val mc: net.minecraft.client.gl.UniformType
    //#if MC>=12106
    //$$     get() = net.minecraft.client.gl.UniformType.UNIFORM_BUFFER
    //#else
    //$$     get() = when (this) {
    //$$         Int1 -> net.minecraft.client.gl.UniformType.INT
    //$$         Float1 -> net.minecraft.client.gl.UniformType.FLOAT
    //$$         Float2 -> net.minecraft.client.gl.UniformType.VEC2
    //$$         Float3 -> net.minecraft.client.gl.UniformType.VEC3
    //$$         Float4 -> net.minecraft.client.gl.UniformType.VEC4
    //$$         Mat2 -> throw UnsupportedOperationException("This version of Minecraft does not support Mat2 uniforms.")
    //$$         Mat3 -> throw UnsupportedOperationException("This version of Minecraft does not support Mat3 uniforms.")
    //$$         Mat4 -> net.minecraft.client.gl.UniformType.MATRIX4X4
    //$$     }
    //#endif
    //#endif

    companion object {
        fun fromGlsl(glslName: String): UniformType =
            values().find { it.glslName == glslName } ?: throw NoSuchElementException(glslName)
    }
}
