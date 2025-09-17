package net.spaceeye.vmod.utils

import sun.misc.Unsafe

object UnsafeGetter {
    @JvmStatic val unsafe: Unsafe
    init {
        var f = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        unsafe = f.get(null) as Unsafe
    }
}