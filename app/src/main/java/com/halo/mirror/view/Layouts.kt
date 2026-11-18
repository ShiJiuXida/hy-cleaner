package com.halo.mirror.view

import android.view.View
import java.lang.reflect.Field

/**
 * ConstraintLayout 的布局参数没法静态引用（工程里没引这个依赖），按字段名反射设置。
 * 目前只有底栏「三个 tab 平分宽度」用到。
 */
object Layouts {

    fun pin(view: View, width: Int, bias: Float) {
        val lp = view.layoutParams ?: return
        if (!lp.javaClass.name.contains("ConstraintLayout")) return

        var changed = false
        changed = setInt(lp, "width", width) or changed
        changed = setFloat(lp, "horizontalWeight", 0f) or changed
        changed = setFloat(lp, "horizontalBias", bias) or changed
        // 退回到父容器约束，让三个各占三分之一
        changed = setInt(lp, "startToStart", 0) or changed
        changed = setInt(lp, "endToEnd", 0) or changed
        changed = setInt(lp, "startToEnd", -1) or changed
        changed = setInt(lp, "endToStart", -1) or changed
        changed = setInt(lp, "leftToLeft", 0) or changed
        changed = setInt(lp, "rightToRight", 0) or changed
        changed = setInt(lp, "leftToRight", -1) or changed
        changed = setInt(lp, "rightToLeft", -1) or changed

        if (changed) view.layoutParams = lp
    }

    private fun setInt(target: Any, name: String, value: Int): Boolean = guarded {
        field(target, name, Int::class.javaPrimitiveType)?.takeIf { f ->
            f.getInt(target) != value
        }?.let { f ->
            f.setInt(target, value)
            true
        } ?: false
    }

    private fun setFloat(target: Any, name: String, value: Float): Boolean = guarded {
        field(target, name, Float::class.javaPrimitiveType)?.takeIf { f ->
            f.getFloat(target) != value
        }?.let { f ->
            f.setFloat(target, value)
            true
        } ?: false
    }

    private inline fun guarded(body: () -> Boolean): Boolean =
        try {
            body()
        } catch (t: Throwable) {
            false
        }

    private fun field(target: Any, name: String, type: Class<*>?): Field? {
        var cursor: Class<*>? = target.javaClass
        while (cursor != null && cursor != Any::class.java) {
            runCatching { cursor.getDeclaredField(name) }.getOrNull()?.let { f ->
                if (f.type == type) {
                    f.isAccessible = true
                    return f
                }
            }
            cursor = cursor.superclass
        }
        return null
    }
}
