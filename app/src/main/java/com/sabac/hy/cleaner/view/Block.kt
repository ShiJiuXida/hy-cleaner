package com.sabac.hy.cleaner.view

import android.view.View
import com.sabac.hy.cleaner.core.FeatureRegistry
import com.sabac.hy.cleaner.core.HyLog
import com.sabac.hy.cleaner.core.Live
import com.sabac.hy.cleaner.core.Settings

/**
 * 可见性拦截：名单里的控件永远不允许变可见。
 *
 * 为什么需要它：只在每帧里 GONE 是治不住闪的 —— 切页/重建时欢游会先把控件
 * 重新设成 VISIBLE，再等我们下一次清扫，中间那一帧就露出来了。
 * 所以直接在 View.setVisibility 上把门，控件根本没机会显示。
 *
 * 名单来自各 Feature 的 [com.sabac.hy.cleaner.core.Feature.blockIds]，
 * 所以「哪个开关开着才拦」这件事天然跟着功能定义走。
 */
object Block {

    private val ids = HashMap<Int, String>(16)

    @Volatile
    private var ready = false

    /** 每次配置刷新后重建名单；需要资源表，所以得有真实界面才能解析 */
    fun refresh(d: Dom, s: Settings) {
        ids.clear()
        FeatureRegistry.all.forEach { feature ->
            if (!s.on(feature.key)) return@forEach
            feature.blockIds.forEach { name ->
                val id = d.rawId(name)
                if (id != 0) ids[id] = name
            }
        }
        ready = ids.isNotEmpty()
        if (ready) HyLog.i("可见性名单 ${ids.size} 项")
    }

    /** 给 setVisibility 钩子用：热路径，只查表 */
    fun blocked(id: Int): Boolean = ready && ids.containsKey(id)

    fun size(): Int = ids.size

    /** 兜底：还没建过名单时用当前窗口建一次 */
    fun ensure(root: View?) {
        if (size() > 0 || root == null) return
        runCatching { refresh(Dom(root), Live.settings) }
    }

    /** 这个控件允不允许被隐藏（避免自己跟自己打架） */
    fun allow(view: View): Boolean = true
}
