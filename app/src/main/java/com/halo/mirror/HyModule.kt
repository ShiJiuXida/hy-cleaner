package com.halo.mirror

import com.halo.mirror.core.HyLog
import com.halo.mirror.core.Live
import com.halo.mirror.core.Process
import com.halo.mirror.core.Target
import com.halo.mirror.hook.Hooks
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * 模块入口（对应 META-INF/xposed/java_init.list）。
 *
 * 界面清理不去 hook 欢游自己的类 —— 它的 dex 被加固壳保护着，静态拿不到业务类。
 * 这里只 hook 框架层，拿到已经建好的 View 树，按资源 id 和文案把不要的部件收掉。
 */
class HyModule : XposedModule() {

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        HyLog.bind(this)
        HyLog.i("已加载 process=${param.processName} api=$apiVersion")
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        if (param.packageName != Target.PACKAGE || !param.isFirstPackage) return

        // 只在界面进程干活：push / 保活子进程没有 View 树，注进去只是白开一堆 hook
        if (!Process.isMain()) {
            HyLog.i("跳过非界面进程 ${Process.name()}")
            return
        }

        if (!Live.attach(this)) {
            HyLog.w("读取配置失败，跳过")
            return
        }
        val settings = Live.settings
        if (!settings.enabled || settings.panic) {
            HyLog.i("模块未启用，跳过")
            return
        }

        Hooks(this).install(param.classLoader)
    }

    override fun onHotReloading(param: XposedModuleInterface.HotReloadingParam): Boolean = true

    /**
     * 模块 APK 更新后框架会带着新代码重进这个方法，不用重启欢游。
     *
     * 旧 hook 必须自己摘（[onHotReloading] 返回 true 就是「我自己处理」），
     * 摘完立刻用新代码重装 —— 否则热重载之后等于模块被卸掉了。
     */
    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        HyLog.bind(this)
        param.oldHookHandles.forEach { runCatching { it.unhook() } }
        HyLog.i("热重载：旧 hook 已摘，重装新代码")

        runCatching {
            if (!Process.name().startsWith(Target.PACKAGE)) return@runCatching
            if (!Process.isMain()) return@runCatching
            if (!Live.attach(this)) return@runCatching
            val settings = Live.settings
            if (!settings.enabled || settings.panic) return@runCatching
            Hooks(this).install(HyModule::class.java.classLoader)
        }.onFailure { HyLog.w("热重载重装失败 ${it.message}") }
    }
}
