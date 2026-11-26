package com.halo.mirror.hook

import android.app.Dialog
import android.view.View
import com.halo.mirror.core.FeatureRegistry
import com.halo.mirror.core.HyLog
import com.halo.mirror.core.Live
import com.halo.mirror.core.Settings
import com.halo.mirror.feature.PopupFeature
import com.halo.mirror.view.Dom
import java.util.Collections
import java.util.WeakHashMap

/**
 * 弹窗拦截与清扫。
 *
 * 两条路径一起用，缺一不可：
 *
 * 1. `Dialog.show()` 之前问一次（[blocked]）—— 命中就直接不 show，弹窗根本没有机会出现；
 * 2. 常驻循环里扫所有窗口根（[sweep]）—— 覆盖「模块装上之前就已经弹出来」的实例。
 *    热重载后当前这两个弹窗就是靠这一条关掉的。
 *
 * 关的方式是点它自己的关闭按钮，不是把窗口摘掉：`Dialog.dismiss()` 需要 Dialog 实例，
 * 而已经在屏上的窗口拿不到那个实例；从 WindowManagerGlobal 里硬删窗口又会让应用后面的
 * `dismiss()` 抛 "View not attached"。点关闭按钮是应用自己的正常路径，最稳。
 */
object PopupGuard {

    /** 已经处理过的窗口根，避免 500ms 一轮重复点 */
    private val handled = Collections.newSetFromMap(WeakHashMap<View, Boolean>())

    private fun active(s: Settings): List<PopupFeature> =
        FeatureRegistry.all.filterIsInstance<PopupFeature>().filter { s.on(it.key) }

    /** `Dialog.show()` 之前问一次：true = 这个弹窗不要显示 */
    fun blocked(dialog: Dialog): Boolean {
        val s = Live.settings
        if (!s.enabled || s.panic) return false
        val decor = runCatching { dialog.window?.decorView }.getOrNull() ?: return false
        if ((decor as? android.view.ViewGroup)?.childCount == 0) return false

        val d = Dom(decor)
        val hit = active(s).firstOrNull { rule ->
            runCatching { rule.matches(d) }.getOrDefault(false)
        } ?: return false

        HyLog.i("拦截弹窗 ${hit.key}")
        return true
    }

    /**
     * `show()` 之后再判一次。
     *
     * 必须要有这一道：Dialog 的内容视图是 `show()` 内部的 `onCreate()` 才 inflate 的，
     * 在 show 之前看 decor 往往还是空的（AlertDialog.Builder 那种写法）。
     * 这时已经拿到 Dialog 实例，直接 `dismiss()` 最干净，不会留窗口也不会有事后异常。
     */
    fun handleShown(dialog: Dialog): Boolean {
        val s = Live.settings
        if (!s.enabled || s.panic) return false
        val decor = runCatching { dialog.window?.decorView }.getOrNull() ?: return false

        val d = Dom(decor)
        val hit = active(s).firstOrNull { rule ->
            runCatching { rule.matches(d) }.getOrDefault(false)
        } ?: return false

        // 优先点它自己的关闭按钮：应用侧的「关闭」回调还能正常走一遍
        val clicked = runCatching { hit.close(d) }.getOrDefault(false)
        if (!clicked) runCatching { dialog.dismiss() }
        HyLog.i("弹窗已拦下 ${hit.key}${if (clicked) "（点关闭）" else "（dismiss）"}")
        return true
    }

    /** 已经弹出来的：扫一遍窗口根，命中就点关闭 */
    fun sweep() {
        val s = Live.settings
        if (!s.enabled || s.panic) return
        val rules = active(s)
        if (rules.isEmpty()) return

        Windows.roots().forEach { root ->
            if (handled.contains(root)) return@forEach
            val d = Dom(root)
            for (rule in rules) {
                if (!runCatching { rule.matches(d) }.getOrDefault(false)) continue
                if (runCatching { rule.close(d) }.getOrDefault(false)) {
                    handled.add(root)
                    HyLog.i("关闭已显示的弹窗 ${rule.key}")
                    break
                }
            }
        }
    }
}
