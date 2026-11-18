package com.sabac.hy.cleaner

import android.view.View
import com.sabac.hy.cleaner.core.FeatureRegistry
import com.sabac.hy.cleaner.core.Live
import com.sabac.hy.cleaner.feature.EntertainmentTab
import com.sabac.hy.cleaner.view.Dom

/**
 * 一次清扫：把一棵窗口根视图上该收的收掉。
 *
 * 幂等，可以反复跑 —— 进页面、列表滚动、弹出面板后都会再跑一次。
 * 具体规则全在各 Feature 里，这里只负责按配置逐条执行。
 */
object Sweep {

    /** 找到顶部页签容器时回调，用来锁掉右划 */
    @Volatile
    var onPager: ((View) -> Unit)? = null

    fun run(root: View?) {
        if (root == null) return
        val s = Live.settings
        if (!s.enabled || s.panic) return

        val d = Dom(root)
        FeatureRegistry.all.forEach { it.applyIfOn(d, s) }

        // 「娱乐」页签被藏掉后，右划也不能再切到那一页
        if (s.on(EntertainmentTab.key)) {
            d.id("channel_tab_viewpager")?.let { onPager?.invoke(it) }
        }
    }
}
