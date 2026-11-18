package com.sabac.hy.cleaner.hook

import android.view.View
import com.sabac.hy.cleaner.core.HyLog
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * hook 安装的统一入口。
 *
 * 一条 hook 失败只记日志，不影响其它 hook —— 加固壳环境下个别方法有可能找不到。
 */
class Installer(private val xposed: XposedModule) {

    fun on(method: java.lang.reflect.Executable, id: String, body: (XposedInterface.Chain) -> Any?): Boolean =
        runCatching {
            xposed.deoptimize(method)
            xposed.hook(method).setId(id).intercept { chain -> body(chain) }
            true
        }.getOrElse {
            HyLog.w("挂载 $id 失败 ${it.message}")
            false
        }

    fun on(method: java.lang.reflect.Executable?, id: String): Boolean =
        method != null && on(method, id) { it.proceed() }

    /** 在类层级里找第一个声明了该方法签名的类，找不到返回 null */
    fun find(start: Class<*>, name: String, vararg types: Class<*>): java.lang.reflect.Method? {
        var cursor: Class<*>? = start
        while (cursor != null && cursor != Any::class.java) {
            runCatching { cursor.getDeclaredMethod(name, *types) }.getOrNull()?.let { return it }
            cursor = cursor.superclass
        }
        return null
    }

    companion object {
        fun viewOf(chain: XposedInterface.Chain): View? = chain.thisObject as? View
    }
}
