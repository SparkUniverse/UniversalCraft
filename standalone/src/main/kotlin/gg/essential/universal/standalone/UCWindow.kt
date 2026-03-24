package gg.essential.universal.standalone

import gg.essential.universal.standalone.glfw.Glfw
import gg.essential.universal.standalone.glfw.GlfwWindow
import gg.essential.universal.UKeyboard
import gg.essential.universal.UKeyboard.toModifiers
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMouse
import gg.essential.universal.UResolution
import gg.essential.universal.UScreen
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

        GLFW.glfwSetMouseButtonCallback(glfwWindow.glfwId) { _, button, action, modifiers ->
            uiScope.launch {
                when (action) {
                    GLFW.GLFW_PRESS -> {
                        UKeyboard.keysDown.add(button)
                        UScreen.currentScreen?.run {
                            standaloneGetInputHandler()
                                ?.uMouseClicked(UMouse.Scaled.x, UMouse.Scaled.y, button, modifiers.toModifiers())
                                ?: onMouseClicked(UMouse.Scaled.x, UMouse.Scaled.y, button)
                        }
                    }

                    GLFW.GLFW_RELEASE -> {
                        UKeyboard.keysDown.remove(button)
                        UScreen.currentScreen?.run {
                            standaloneGetInputHandler()
                                ?.uMouseReleased(UMouse.Scaled.x, UMouse.Scaled.y, button, modifiers.toModifiers())
                                ?: onMouseReleased(UMouse.Scaled.x, UMouse.Scaled.y, button)
                        }
                    }
                }
            }
        }

        GLFW.glfwSetScrollCallback(glfwWindow.glfwId) { _, x, y ->
            uiScope.launch {
                UScreen.currentScreen?.run {
                    standaloneGetInputHandler()
                        ?.uMouseScrolled(UMouse.Scaled.x, UMouse.Scaled.y, x, y)
                        ?: onMouseScrolled(UMouse.Scaled.x, UMouse.Scaled.y, x, y)
                }
            }
        }

        GLFW.glfwSetCharModsCallback(glfwWindow.glfwId) { _, codepoint, modifiers ->
            uiScope.launch {
                for (char in Character.toChars(codepoint)) {
                    UScreen.currentScreen?.run {
                        standaloneGetInputHandler()
                            ?.uCharTyped(codepoint)
                            ?: onKeyPressed(0, char, modifiers.toModifiers())
                    }
                }
            }
        }

        GLFW.glfwSetKeyCallback(glfwWindow.glfwId) { _, key, scancode, action, modifiers ->
            uiScope.launch {
                when (action) {
                    GLFW.GLFW_PRESS, GLFW.GLFW_REPEAT -> {
                        UKeyboard.keysDown.add(key)
                        UScreen.currentScreen?.run {
                            standaloneGetInputHandler()
                                ?.uKeyPressed(key, scancode, modifiers.toModifiers())
                                ?: onKeyPressed(key, 0.toChar(), modifiers.toModifiers())
                        }
                    }

                    GLFW.GLFW_RELEASE -> {
                        UKeyboard.keysDown.remove(key)
                        UScreen.currentScreen?.run {
                            standaloneGetInputHandler()
                                ?.uKeyReleased(key, scancode, modifiers.toModifiers())
                                ?: onKeyReleased(key, 0.toChar(), modifiers.toModifiers())
                        }
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
        var nextTick = 0f
        renderLoop { _, deltaTime ->
            val screen = UScreen.currentScreen ?: return@renderLoop false

            nextTick += deltaTime
            while (nextTick >= 0) {
                nextTick -= 1 / 20f
                screen.onTick()
            }

            screen.onDrawScreen(UMatrixStack(), UMouse.Scaled.x.toInt(), UMouse.Scaled.y.toInt(), nextTick + 1 / 20f)

            return@renderLoop true
        }
    }
}
