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

    override fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam) {
        param.oldHookHandles.forEach { runCatching { it.unhook() } }
        HyLog.i("热重载完成，需重启欢游生效")
    }
}
