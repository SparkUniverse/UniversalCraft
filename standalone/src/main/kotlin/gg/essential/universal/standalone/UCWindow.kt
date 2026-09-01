package gg.essential.universal.standalone

import gg.essential.universal.UGraphics
import gg.essential.universal.standalone.glfw.Glfw
import gg.essential.universal.standalone.glfw.GlfwWindow
import gg.essential.universal.UKeyboard
import gg.essential.universal.UKeyboard.toModifiers
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import gg.essential.universal.UScreen
import gg.essential.universal.render.UGpuSampler
import gg.essential.universal.render.UGpuSampler.Companion.invoke
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.shader.BlendState
import gg.essential.universal.vertex.UBufferBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack

/** Must be initialized on the GLFW main thread! */
class UCWindow(val glfwWindow: GlfwWindow, val uiScope: CoroutineScope) {
    init {
        GLFW.glfwSetWindowSizeCallback(glfwWindow.glfwId) { _, width, height ->
            uiScope.launch {
                UResolution.windowWidth = width
                UResolution.windowHeight = height
            }
        }

        GLFW.glfwSetFramebufferSizeCallback(glfwWindow.glfwId) { _, width, height ->
            uiScope.launch {
                UResolution.viewportWidth = width
                UResolution.viewportHeight = height
            }
        }

        GLFW.glfwSetCursorPosCallback(glfwWindow.glfwId) { _, x, y ->
            uiScope.launch {
                UMouse.Raw.x = x
                UMouse.Raw.y = y
            }
        }

        GLFW.glfwSetMouseButtonCallback(glfwWindow.glfwId) { _, button, action, _ ->
            uiScope.launch {
                when (action) {
                    GLFW.GLFW_PRESS -> {
                        UKeyboard.keysDown.add(button)
                        UScreen.currentScreen?.onMouseClicked(UMouse.Scaled.x, UMouse.Scaled.y, button)
                    }

                    GLFW.GLFW_RELEASE -> {
                        UKeyboard.keysDown.remove(button)
                        UScreen.currentScreen?.onMouseReleased(UMouse.Scaled.x, UMouse.Scaled.y, button)
                    }
                }
            }
        }

        GLFW.glfwSetScrollCallback(glfwWindow.glfwId) { _, x, y ->
            uiScope.launch {
                UScreen.currentScreen?.onMouseScrolled(UMouse.Scaled.x, UMouse.Scaled.y, x, y)
            }
        }

        GLFW.glfwSetCharModsCallback(glfwWindow.glfwId) { _, codepoint, modifiers ->
            uiScope.launch {
                for (char in Character.toChars(codepoint)) {
                    UScreen.currentScreen?.onKeyPressed(0, char, modifiers.toModifiers())
                }
            }
        }

        GLFW.glfwSetKeyCallback(glfwWindow.glfwId) { _, key, _, action, modifiers ->
            uiScope.launch {
                when (action) {
                    GLFW.GLFW_PRESS, GLFW.GLFW_REPEAT -> {
                        UKeyboard.keysDown.add(key)
                        UScreen.currentScreen?.onKeyPressed(key, 0.toChar(), modifiers.toModifiers())
                    }

                    GLFW.GLFW_RELEASE -> {
                        UKeyboard.keysDown.remove(key)
                        UScreen.currentScreen?.onKeyReleased(key, 0.toChar(), modifiers.toModifiers())
                    }
                }
            }
        }

        MemoryStack.stackPush().use { stack ->
            val width = stack.mallocInt(1)
            val height = stack.mallocInt(1)
            GLFW.glfwGetWindowSize(glfwWindow.glfwId, width, height)
            UResolution.windowWidth = width.get(0)
            UResolution.windowHeight = height.get(0)
            GLFW.glfwGetFramebufferSize(glfwWindow.glfwId, width, height)
            UResolution.viewportWidth = width.get(0)
            UResolution.viewportHeight = height.get(0)
        }
    }

    suspend fun renderLoop(render: (time: Float, deltaTime: Float) -> Boolean) {
        var firstFrame = true
        var lastTime = 0f
        while (!GLFW.glfwWindowShouldClose(glfwWindow.glfwId)) {
            val time = GLFW.glfwGetTime().toFloat()
            val deltaTime = time - lastTime
            lastTime = time

            glfwWindow.prepareFrame(UResolution.viewportWidth, UResolution.viewportHeight)
            if (!render(time, deltaTime)) {
                break
            }
            GLFW.glfwSwapBuffers(glfwWindow.glfwId)

            if (firstFrame) {
                firstFrame = false
                withContext(Dispatchers.Glfw) {
                    GLFW.glfwShowWindow(glfwWindow.glfwId)
                }
            }

            withContext(Dispatchers.Glfw) {
                GLFW.glfwPollEvents()
            }
        }
    }

    suspend fun renderScreenUntilClosed() {
        var prevScreen: UScreen? = null
        var renderer: UScreen.Renderer? = null
        var nextTick = 0f
        renderLoop { _, deltaTime ->
            val screen = UScreen.currentScreen ?: return@renderLoop false

            nextTick += deltaTime
            while (nextTick >= 0) {
                nextTick -= 1 / 20f
                screen.onTick()
            }

            if (prevScreen != screen) {
                prevScreen = screen
                renderer?.close()
                renderer = null
            }
            val renderer = renderer ?: screen.uCreateRenderer()?.also { renderer = it }
            if (renderer != null) {
                val renderState = screen.uExtractRenderState(UMouse.Scaled.x.toInt(), UMouse.Scaled.y.toInt(), nextTick + 1 / 20f)
                val textureView = renderer.render(renderState)
                val x1 = 0.0
                val x2 = UResolution.scaledWidth.toDouble()
                val y1 = 0.0
                val y2 = UResolution.scaledHeight.toDouble()
                val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE)
                buffer.pos(UMatrixStack.UNIT, x1, y2, 0.0).tex(0.0, 0.0).endVertex()
                buffer.pos(UMatrixStack.UNIT, x2, y2, 0.0).tex(1.0, 0.0).endVertex()
                buffer.pos(UMatrixStack.UNIT, x2, y1, 0.0).tex(1.0, 1.0).endVertex()
                buffer.pos(UMatrixStack.UNIT, x1, y1, 0.0).tex(0.0, 1.0).endVertex()
                buffer.build()?.drawAndClose(PIPELINE_BLIT) {
                    texture(0, textureView, SAMPLER_NEAREST)
                }
            } else {
                screen.onDrawScreen(UMatrixStack(), UMouse.Scaled.x.toInt(), UMouse.Scaled.y.toInt(), nextTick + 1 / 20f)
            }

            return@renderLoop true
        }
    }
}

private val PIPELINE_BLIT = URenderPipeline.builderWithDefaultShader(
    "universalcraft:blit",
    UGraphics.DrawMode.QUADS,
    UGraphics.CommonVertexFormats.POSITION_TEXTURE,
).apply {
    blendState = BlendState.PREMULTIPLIED_ALPHA
}.build()

private val SAMPLER_NEAREST = UGpuSampler(
    UGpuSampler.AddressMode.CLAMP_TO_EDGE,
    UGpuSampler.AddressMode.CLAMP_TO_EDGE,
    UGpuSampler.FilterMode.NEAREST,
    UGpuSampler.FilterMode.NEAREST,
    false,
)
