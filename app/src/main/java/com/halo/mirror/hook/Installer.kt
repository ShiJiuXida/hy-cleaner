package com.halo.mirror.hook

import com.halo.mirror.core.HyLog
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * hook 安装。
 *
 * 关于 [deoptimize]：它会让目标方法脱离 AOT/JIT 优化，ArtMethod 的 entrypoint 移到桥接层，
 * 是「方法被动过」最直接的证据。框架只负责隐藏注入和栈帧，不负责隐藏这件事。
 *
 * 所以默认**不调用**它 —— 只有确实被内联、hook 不生效时才按方法单独打开。
 * [DEOPT_TARGETS] 里就是那类必须 deopt 才能生效的方法 id。
 */
class Installer(private val xposed: XposedModule) {

    fun on(
        method: java.lang.reflect.Executable,
        id: String,
        body: (XposedInterface.Chain) -> Any?,
    ): Boolean = runCatching {
        if (id in DEOPT_TARGETS) {
            runCatching { xposed.deoptimize(method) }
        }
        xposed.hook(method).setId(id).intercept { chain -> body(chain) }
        true
    }.getOrElse {
        HyLog.w("hook 失败 $id ${it.message}")
        false
    }

    /** 在类层级里找第一个声明了该方法签名的类 */
    fun find(start: Class<*>, name: String, vararg types: Class<*>): java.lang.reflect.Method? {
        var cursor: Class<*>? = start
        while (cursor != null && cursor != Any::class.java) {
            runCatching { cursor.getDeclaredMethod(name, *types) }.getOrNull()?.let { return it }
            cursor = cursor.superclass
        }
        return null
    }

    companion object {
        /**
         * 必须 deoptimize 才能生效的 hook id。
         *
         * 目前为空：这些方法都没被内联，去掉 deopt 后功能实测正常。
         * 以后某个开关失效、日志显示 hook 成功但行为没变，就往这里加。
         */
        private val DEOPT_TARGETS = emptySet<String>()
    }
}
