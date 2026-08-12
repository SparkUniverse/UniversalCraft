package gg.essential.universal.utils

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.Function

@Suppress("DEPRECATION")
internal object UnsafeHacks {
    private val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
        .also { it.isAccessible = true }
        .get(null) as Unsafe

    fun <O, T> makeAccessor(field: Field): Accessor<O, T> {
        if (field.type.isPrimitive) {
            throw UnsupportedOperationException("Only Object types are supported.")
        }
        if ((field.modifiers and Modifier.STATIC) != 0) {
            val base = unsafe.staticFieldBase(field)
            val offset = unsafe.staticFieldOffset(field)
            return object : Accessor<O, T> {
                override fun get(owner: O): T {
                    @Suppress("UNCHECKED_CAST")
                    return unsafe.getObject(base, offset) as T
                }

                override fun set(owner: O, value: T) {
                    unsafe.putObject(base, offset, value)
                }
            }
        } else {
            val offset = unsafe.objectFieldOffset(field)
            return object : Accessor<O, T> {
                override fun get(owner: O): T {
                    @Suppress("UNCHECKED_CAST")
                    return unsafe.getObject(owner, offset) as T
                }

                override fun set(owner: O, value: T) {
                    unsafe.putObject(owner, offset, value)
                }
            }
        }
    }

    interface Accessor<O, T> {
        fun get(owner: O): T
        fun set(owner: O, value: T)

        fun update(owner: O, func: Function<T, T>) {
            set(owner, func.apply(get(owner)))
        }
    }
}
