package com.halo.mirror.hook

import android.os.Handler
import android.os.Looper
import com.halo.mirror.Sweep
import com.halo.mirror.core.HyLog
import com.halo.mirror.diag.Probe
import com.halo.mirror.view.Block
import com.halo.mirror.view.Dom
import io.github.libxposed.api.XposedModule

/**
 * 挂载编排。
 *
 * 常驻的 500ms 循环是必需的兜底：屏幕静止时没有绘制回调，只靠 preDraw
 * 会漏掉「加载完成后才出现的列表」。它在模块加载时启动，不随页面切换而停。
 */
class Hooks(private val xposed: XposedModule) {

    private val ui = Handler(Looper.getMainLooper())
    private val installer = Installer(xposed)
    private val uiHooks = UiHooks(installer)

    private var started = false

    private val tick = object : Runnable {
        override fun run() {
            sweepAll()
            ui.postDelayed(this, TICK_MS)
        }
    }

    fun install(loader: ClassLoader) {
        val count = uiHooks.install()
        HyLog.i("挂载完成 $count/4")

        // 装完先扫一遍：模块装上之前就弹出来的窗口（热重载时最常见）也一并收掉
        ui.post { PopupGuard.sweep() }

        if (!started) {
            started = true
            ui.postDelayed(tick, TICK_MS)
        }
    }

    private fun sweepAll() {
        runCatching { PopupGuard.sweep() }
        val roots = Windows.roots()
        roots.forEach { root ->
            runCatching { Sweep.run(root) }
            // debug 包：界面出现新东西时把树打进日志，用来定位弹窗/新广告位
            runCatching { Probe.snapshot(root, "loop") }
            uiHooks.watch(root)
        }
        roots.firstOrNull()?.let { Block.ensure(it) }
        uiHooks.tryListHook()
        uiHooks.tryVisibilityHook()
    }

    companion object {
        private const val TICK_MS = 500L
    }
}
