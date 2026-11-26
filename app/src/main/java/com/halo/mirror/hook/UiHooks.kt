package com.halo.mirror.hook

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewTreeObserver
import android.widget.PopupWindow
import com.halo.mirror.Sweep
import com.halo.mirror.core.HyLog
import com.halo.mirror.core.Live
import com.halo.mirror.core.Target
import com.halo.mirror.diag.Probe
import com.halo.mirror.view.Block
import com.halo.mirror.view.Dom
import java.util.Collections
import java.util.WeakHashMap

/**
 * 界面相关的 hook：页面、弹层、列表、可见性、页签手势。
 *
 * 时机都选在「界面已建好、还没画出来」的时候，这样处理完看不出闪。
 */
class UiHooks(private val installer: Installer) {

    /** 已挂过 preDraw 的根视图，一个根只挂一次 */
    private val watched = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    private val lockedPagers = Collections.newSetFromMap(WeakHashMap<View, Boolean>())
    private val lockedPagerClasses = Collections.newSetFromMap(WeakHashMap<Class<*>, Boolean>())

    private var listHookTries = 0
    private var listHooked = false
    private var visibilityHooked = false

    fun install(): Int {
        Sweep.onPager = { pager -> lockSwipe(pager) }
        return listOf(
            hookResume(),
            hookPause(),
            hookDialog(),
            hookPopup(),
        ).count { it }
    }

    // ---------------------------------------------------------------- 页面

    private fun hookResume(): Boolean {
        val m = runCatching { Activity::class.java.getDeclaredMethod("onResume") }.getOrNull()
            ?: return false
        return installer.on(m, "hy-resume") { chain ->
            val result = chain.proceed()
            val activity = chain.thisObject as? Activity
            if (activity != null && activity.packageName == Target.PACKAGE) {
                Live.reload()
                Windows.track(activity)
                runCatching {
                    activity.window?.decorView?.let { root ->
                        Block.refresh(Dom(root), Live.settings)
                        Sweep.run(root)
                        watch(root)
                    }
                }
            }
            result
        }
    }

    private fun hookPause(): Boolean {
        val m = runCatching { Activity::class.java.getDeclaredMethod("onPause") }.getOrNull()
            ?: return false
        return installer.on(m, "hy-pause") { chain ->
            val result = chain.proceed()
            (chain.thisObject as? Activity)?.let {
                if (it.isFinishing || it.isDestroyed) Windows.forget(it)
            }
            result
        }
    }

    /** 绘制前不节流：切页那一帧也要赶上，否则会闪 */
    fun watch(root: View) {
        if (!watched.add(root)) return
        runCatching {
            root.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    runCatching { Sweep.run(root) }
                    return true
                }
            })
        }
    }

    // ---------------------------------------------------------------- 弹层

    private fun hookDialog(): Boolean {
        val m = runCatching { Dialog::class.java.getDeclaredMethod("show") }.getOrNull()
            ?: return false
        return installer.on(m, "hy-dialog") { chain ->
            val dialog = chain.thisObject as? Dialog
            // 命中弹窗规则就不 show：连一帧都不出现，也不用事后再关
            if (dialog != null && PopupGuard.blocked(dialog)) {
                null
            } else {
                val result = chain.proceed()
                runCatching { if (dialog != null) PopupGuard.handleShown(dialog) }
                runCatching {
                    val decor = dialog?.window?.decorView
                    if (decor != null) {
                        Sweep.run(decor)
                        Probe.panel(Dom(decor))
                        Probe.snapshot(decor, "dialog")
                        watch(decor)
                    }
                }
                result
            }
        }
    }

    private fun hookPopup(): Boolean {
        val m = runCatching {
            PopupWindow::class.java.getDeclaredMethod(
                "showAtLocation",
                View::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
        }.getOrNull() ?: return false
        return installer.on(m, "hy-popup") { chain ->
            val result = chain.proceed()
            runCatching { Sweep.run((chain.thisObject as? PopupWindow)?.contentView) }
            result
        }
    }

    // ---------------------------------------------------------------- 列表

    /**
     * RecyclerView.onLayout 返回后扫一次。
     *
     * 列表项绑定发生在这里；在绘制之前收掉，消息页就不会「先闪一下再消失」。
     * androidx 的类可能还没加载，所以每次扫描都试一次，成功就停。
     */
    fun tryListHook() {
        if (listHooked || listHookTries > 200) return
        listHookTries++
        runCatching {
            val cls = Class.forName("androidx.recyclerview.widget.RecyclerView")
            val m = cls.getDeclaredMethod(
                "onLayout",
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            listHooked = installer.on(m, "hy-list-layout") { chain ->
                val result = chain.proceed()
                runCatching { Sweep.run(chain.thisObject as? View) }
                result
            }
            if (listHooked) HyLog.i("列表布局钩子已挂（第 $listHookTries 次尝试）")
        }
    }

    // ---------------------------------------------------------------- 可见性

    /**
     * View.setVisibility 设卡：名单里的控件一旦被设成可见，立刻改回 GONE。
     * 这是治「闪」的根本手段 —— 不等下一帧，控件根本没机会显示。
     */
    fun tryVisibilityHook() {
        if (visibilityHooked) return
        val m = runCatching {
            View::class.java.getDeclaredMethod("setVisibility", Int::class.javaPrimitiveType)
        }.getOrNull() ?: return
        visibilityHooked = installer.on(m, "hy-visibility") { chain ->
            val view = chain.thisObject as? View
            val want = chain.getArg(0) as? Int ?: 0
            if (view != null && want != View.GONE && Block.blocked(view.id)) {
                View.GONE
            } else {
                chain.proceed()
            }
        }
        if (visibilityHooked) HyLog.i("可见性设卡已挂")
    }

    // ---------------------------------------------------------------- 页签手势

    /** 顶部页签容器不再接管横向手势，右划进不去。只拦认出来的那个实例 */
    private fun lockSwipe(pager: View) {
        if (!lockedPagers.add(pager)) return
        val cls = pager.javaClass
        if (!lockedPagerClasses.add(cls)) return

        var done = 0
        listOf("onInterceptTouchEvent", "onTouchEvent").forEach { name ->
            val m = installer.find(cls, name, android.view.MotionEvent::class.java) ?: return@forEach
            val ok = installer.on(m, "hy-swipe-$name") { chain ->
                val self = chain.thisObject
                if (self is View && lockedPagers.contains(self)) false else chain.proceed()
            }
            if (ok) done++
        }
        HyLog.i("页签手势已锁定 ($done)")
    }
}
