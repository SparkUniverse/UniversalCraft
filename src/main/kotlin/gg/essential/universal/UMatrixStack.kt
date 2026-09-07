package gg.essential.universal

import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Matrix3f
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import java.nio.Buffer
import java.nio.FloatBuffer
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

//#if MC>=11700
//$$ import com.mojang.blaze3d.systems.RenderSystem
//#else
import net.minecraft.client.renderer.GLAllocation
//#endif

//#if MC>=11600
//$$ import com.mojang.blaze3d.matrix.MatrixStack
//#endif

//#if MC>=11400
//$$ import net.minecraft.util.math.MathHelper
//#else
import kotlin.math.acos
import kotlin.math.sqrt
//#endif

/**
 * A stack of matrices which can be manipulated via common transformations, just like MC's MatrixStack.
 *
 * For MC versions 1.16 and above, methods exist to convert from (via the constructor) and to (via Entry.toMCStack) the
 * vanilla stack type if required.
 * For MC versions below 1.17, the *GlobalState methods can be used to transfer the state of this matrix stack into the
 * global GL state. For 1.17, they transfer state into Mojang's global MatrixStack in RenderSystem.
 */
class UMatrixStack private constructor(
    private val stack: Deque<Entry>
) {

    constructor() : this(ArrayDeque<Entry>().apply {
        add(Entry(
            Matrix4f().apply { setIdentity() },
            Matrix3f().apply { setIdentity() }
        ))
    })

    //#if MC>=11600
    //$$ constructor(mc: MatrixStack) : this(mc.last)
    //$$ constructor(mc: MatrixStack.Entry) : this(ArrayDeque<Entry>().apply {
    //$$     add(Entry(mc.matrix, mc.normal))
    //$$ })
    //$$ fun toMC() = peek().toMCStack()
    //#endif

    //#if MC>=12106
    //$$ constructor(mc: org.joml.Matrix3x2f) : this() {
    //$$     peek().model.apply {
    //$$         m00(mc.m00)
    //$$         m01(mc.m01)
    //$$         m10(mc.m10)
    //$$         m11(mc.m11)
    //$$         m30(mc.m20)
    //$$         m31(mc.m21)
    //$$     }
    //$$ }
    //$$ fun to3x2Joml(dst: org.joml.Matrix3x2f = org.joml.Matrix3x2f()): org.joml.Matrix3x2f {
    //$$     val uc = peek().model
    //$$     dst.set(uc.m00(), uc.m01(), uc.m10(), uc.m11(), uc.m30(), uc.m31())
    //$$     return dst
    //$$ }
    //#endif

    fun translate(x: Double, y: Double, z: Double) = translate(x.toFloat(), y.toFloat(), z.toFloat())

    fun translate(x: Float, y: Float, z: Float) {
        if (x == 0f && y == 0f && z == 0f) return
        stack.last.run {
            //#if MC>=11903
            //$$ model.translate(x, y, z)
            //#elseif MC>=11400
            //$$ model.mul(Matrix4f.makeTranslate(x, y, z))
            //#else
            Matrix4f.translate(Vector3f(x, y, z), model, model)
            //#endif
        }
    }

    fun scale(x: Double, y: Double, z: Double) = scale(x.toFloat(), y.toFloat(), z.toFloat())

    fun scale(x: Float, y: Float, z: Float) {
        if (x == 1f && y == 1f && z == 1f) return
        return stack.last.run {
            //#if MC>=11903
            //$$ model.scale(x, y, z)
            //#elseif MC>=11400
            //$$ model.mul(Matrix4f.makeScale(x, y, z))
            //#else
            Matrix4f.scale(Vector3f(x, y, z), model, model)
            //#endif
            if (x == y && y == z) {
                if (x < 0f) {
                    //#if MC>=11903
                    //$$ normal.scale(-1f)
                    //#elseif MC>=11400
                    //$$ normal.mul(-1f)
                    //#else
                    Matrix3f.negate(normal, normal)
                    //#endif
                }
            } else {
                val ix = 1f / x
                val iy = 1f / y
                val iz = 1f / z
                //#if MC>=11400
                //$$ val rt = MathHelper.fastInvCubeRoot(ix * iy * iz)
                //#else
                val rt = Math.cbrt((ix * iy * iz).toDouble()).toFloat()
                //#endif
                //#if MC>=11903
                //$$ normal.scale(rt * ix, rt * iy, rt * iz)
                //#elseif MC>=11400
                //$$ normal.mul(Matrix3f.makeScaleMatrix(rt * ix, rt * iy, rt * iz))
                //#else
                val scale = Matrix3f()
                scale.m00 = rt * ix
                scale.m11 = rt * iy
                scale.m22 = rt * iz
                Matrix3f.mul(normal, scale, normal)
                //#endif
            }
        }
    }

    @JvmOverloads
    fun rotate(angle: Float, x: Float, y: Float, z: Float, degrees: Boolean = true) {
        if (angle == 0f) return
        stack.last.run {
            //#if MC>=11903
            //$$ val angleRadians = if (degrees) Math.toRadians(angle.toDouble()).toFloat() else angle
            //$$ multiply(Quaternionf().rotateAxis(angleRadians, x, y, z))
            //#elseif MC>=11400
            //$$ multiply(Quaternion(Vector3f(x, y, z), angle, degrees));
            //#else
            val angleRadians = if (degrees) Math.toRadians(angle.toDouble()).toFloat() else angle
            val axis = Vector3f(x, y, z)
            Matrix4f.rotate(angleRadians, axis, model, model)

            // There appears to be no method to rotate a Matrix3f, so we'll have to do it manually
            fun makeRotationMatrix(angle: Float, axis: Vector3f) = Matrix3f().apply {
                val c = cos(angle)
                val s = sin(angle)
                val oneMinusC = 1 - c
                val xx = axis.x * axis.x
                val xy = axis.x * axis.y
                val xz = axis.x * axis.z
                val yy = axis.y * axis.y
                val yz = axis.y * axis.z
                val zz = axis.z * axis.z
                val xs = axis.x * s
                val ys = axis.y * s
                val zs = axis.z * s

                m00 = xx * oneMinusC + c
                m01 = xy * oneMinusC + zs
                m02 = xz * oneMinusC - ys
                m10 = xy * oneMinusC - zs
                m11 = yy * oneMinusC + c
                m12 = yz * oneMinusC + xs
                m20 = xz * oneMinusC + ys
                m21 = yz * oneMinusC - xs
                m22 = zz * oneMinusC + c
            }
            Matrix3f.mul(normal, makeRotationMatrix(angleRadians, axis), normal)
            //#endif
        }
    }

    fun multiply(quaternion: Quaternion) {
        //#if MC>=11903
        //$$ stack.last.run {
        //$$     model.rotate(quaternion)
        //$$     normal.rotate(quaternion)
        //$$ }
        //#elseif MC>=11400
        //$$ stack.last.run {
        //$$     model.mul(quaternion)
        //$$     normal.mul(quaternion)
        //$$ }
        //#else
        // Calculate the angle and axis for the rotate() function
        // Note: this implementation only works for unit quaternions, whereas the above will accept any, the input quaternion should always be normalized anyway
        val angle = 2 * acos(quaternion.w)
        val s = sqrt(1.0 - quaternion.w * quaternion.w).toFloat()
        if (s < 1e-8) {
            // If the quaternion is close to zero, just treat it as no rotation
            return
        }
        val sInverse = 1 / s // Slight loss of precision with floats, balanced by performance gain
        val x = quaternion.x * sInverse
        val y = quaternion.y * sInverse
        val z = quaternion.z * sInverse

        // Apply to stack.last
        rotate(angle, x, y, z, degrees = false)
        //#endif
    }

    fun fork() = UMatrixStack(ArrayDeque<Entry>().apply {
        add(stack.last.deepCopy())
    })

    fun push(): Unit = stack.addLast(stack.last.deepCopy())

    fun pop() {
        stack.removeLast()
    }

    fun peek(): Entry = stack.last

    fun isEmpty(): Boolean = stack.size == 1

    fun applyToGlobalState() {
        //#if MC>=11700
        //#if MC>=12005
        //$$ RenderSystem.getModelViewStack().mul(stack.last.model)
        //#else
        //$$ RenderSystem.getModelViewStack().method_34425(stack.last.model)
        //#endif
        //#if MC<12102
        //$$ RenderSystem.applyModelViewMatrix()
        //#endif
        //#else
        stack.last.model.store(MATRIX_BUFFER)
        // Explicit cast to Buffer required so we do not use the JDK9+ override in FloatBuffer
        (MATRIX_BUFFER as Buffer).rewind()
        GL11.glMultMatrix(MATRIX_BUFFER)
        //#endif
    }

    fun replaceGlobalState() {
        //#if MC>=12005
        //$$ RenderSystem.getModelViewStack().identity()
        //#elseif MC>=11700
        //$$ RenderSystem.getModelViewStack().loadIdentity()
        //#else
        GL11.glLoadIdentity()
        //#endif
        applyToGlobalState()
    }

    fun runWithGlobalState(block: Runnable) = runWithGlobalState { block.run() }

    fun <R> runWithGlobalState(block: () -> R): R  = withGlobalStackPushed {
        applyToGlobalState()
        block()
    }

    fun runReplacingGlobalState(block: Runnable) = runReplacingGlobalState { block.run() }

    fun <R> runReplacingGlobalState(block: () -> R): R = withGlobalStackPushed {
        replaceGlobalState()
        block()
    }

    private inline fun <R> withGlobalStackPushed(block: () -> R) : R {
        //#if MC>=11700
        //$$ val stack = RenderSystem.getModelViewStack()
        //#if MC>=12005
        //$$ stack.pushMatrix()
        //#else
        //$$ stack.push()
        //#endif
        //#else
        UGraphics.GL.pushMatrix()
        //#endif
        return block().also {
            //#if MC>=11700
            //#if MC>=12005
            //$$ stack.popMatrix()
            //#else
            //$$ stack.pop()
            //#endif
            //#if MC<12102
            //$$ RenderSystem.applyModelViewMatrix()
            //#endif
            //#else
            UGraphics.GL.popMatrix()
            //#endif
        }
    }

    data class Entry(val model: Matrix4f, val normal: Matrix3f) {
        //#if MC>=11600
        //$$ fun toMCStack() = MatrixStack().also {
        //$$     it.last.matrix.mul(model)
        //$$     it.last.normal.mul(normal)
        //$$ }
        //#endif

        fun deepCopy() =
            //#if MC>=11903
            //$$ Entry(Matrix4f(model), Matrix3f(normal))
            //#elseif MC>=11400
            //$$ Entry(model.copy(), normal.copy())
            //#else
            Entry(Matrix4f.load(model, null), Matrix3f.load(normal, null))
            //#endif

        /**
         * Returns the model matrix in row-major order.
         */
        val modelAsArray: FloatArray
            get() = with(model) {
                //#if MC>=11400
                //$$ FloatArray(16).also { write(FloatBuffer.wrap(it)) }
                //#else
                floatArrayOf(
                    m00, m10, m20, m30,
                    m01, m11, m21, m31,
                    m02, m12, m22, m32,
                    m03, m13, m23, m33,
                )
                //#endif
            }
    }

    object Compat {
        const val DEPRECATED = """For 1.17 this method requires you pass a UMatrixStack as the first argument.

If you are currently extending this method, you should instead extend the method with the added argument.
Note however for this to be non-breaking, your parent class needs to transition before you do.

If you are calling this method and you cannot guarantee that your target class has been fully updated (such as when
calling an open method on an open class), you should instead call the method with the "Compat" suffix, which will
call both methods, the new and the deprecated one.
If you are sure that your target class has been updated (such as when calling the super method), you should
(for super calls you must!) instead just call the method with the original name and added argument."""

        private val stack = mutableListOf<UMatrixStack>()

        /**
         * To preserve backwards compatibility with old subclasses of UScreen or similar hierarchies,
         * this method allows one to sneak in an artificial matrix stack argument when calling the legacy method
         * which can then later be retrieved via [get] when the base legacy method calls the new one.
         *
         * For an example see [UScreen.onDrawScreenCompat].
         */
        fun <R> runLegacyMethod(matrixStack: UMatrixStack, block: () -> R): R {
            stack.add(matrixStack)
            return block().also {
                stack.removeAt(stack.lastIndex)
            }
        }

        fun get(): UMatrixStack = stack.lastOrNull() ?: UMatrixStack()
    }

    companion object {
        //#if MC<11700
        private val MATRIX_BUFFER: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)
        //#endif

        /**
         * Represents an empty matrix stack. That is, a stack with the identity matrix as its sole entry.
         *
         * This stack may be passed to consuming APIs which may then assume that the stack is in fact a unit stack and
         * can therefore skip math that would be redundant in such cases.
         *
         * **This stack must not be modified.**
         * Consumers may compare this stack by reference and ignore its content.
         * Consumers which are not aware of this stack must still behave correctly, so its content must be correct.
         * [fork] is fine, [push] is not!
         */
        @JvmField
        val UNIT = UMatrixStack()
    }
}