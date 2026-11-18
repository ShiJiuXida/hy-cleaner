package com.sabac.hy.cleaner.hook

import android.app.Activity
import android.view.View
import java.util.Collections
import java.util.WeakHashMap

/**
 * 进程内的窗口根视图。
 *
 * 为什么不用 Activity.getWindow().getDecorView()：欢游会把页面内容挂在额外窗口里，
 * 只拿 Activity 的 decor 会扫到一个空列表（子项 0），广告永远收不掉。
 *
 * WindowManagerGlobal.mViews 里有进程内所有窗口的顶层 View（Activity、Dialog、
 * PopupWindow、悬浮层都算），所以以它为准，拿不到再退回 Activity decor。
 */
object Windows {

    private val activities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    fun track(activity: Activity) {
        activities.add(activity)
        if (activity.isFinishing || activity.isDestroyed) activities.remove(activity)
    }

    fun forget(activity: Activity) = activities.remove(activity)

    fun roots(): List<View> {
        val out = ArrayList<View>(4)
        runCatching {
            val cls = Class.forName("android.view.WindowManagerGlobal")
            val instance = cls.getMethod("getInstance").invoke(null)
            val field = cls.getDeclaredField("mViews").apply { isAccessible = true }
            (field.get(instance) as? List<*>)?.forEach { v -> (v as? View)?.let { out.add(it) } }
        }
        if (out.isEmpty()) {
            activities.toList().forEach { a ->
                runCatching { a.window?.decorView?.let { out.add(it) } }
            }
        }
        return out
    }
}
