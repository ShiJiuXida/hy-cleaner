package com.sabac.hy.cleaner.view

import android.view.View
import android.view.ViewGroup
import com.sabac.hy.cleaner.core.Target
import java.util.WeakHashMap

/**
 * View 树的变更动作。全部是纯 View 操作，不依赖 Dom，方便单独复用与测试。
 *
 * 只有两种语义：
 *  - [hide] 只 GONE；
 *  - [drop] / [dropFully] 在 GONE 之外把尺寸清零。
 *
 * 为什么要区分：RecyclerView 的 LayoutManager 对 GONE 子项照样测量排布，
 * 只 GONE 会留下原尺寸的空白（「广告没了但位置还在」）；
 * 而对普通容器清尺寸又会让整行空掉。所以按目标是不是列表项严格区分。
 */
object Mutate {

    /** 列表项被折叠前的原始尺寸与可见性；键是 View 本身，随 View 一起回收 */
    private val origin = WeakHashMap<View, IntArray>()

    /** 只 GONE，最安全，用在普通容器里的部件上 */
    fun hide(view: View?) {
        if (view == null) return
        if (view.visibility != View.GONE && Block.allow(view)) {
            view.visibility = View.GONE
        }
    }

    /** 列表项：清掉该方向的尺寸，让后面的项补上来 */
    fun drop(item: View?, horizontal: Boolean) = shrink(item, horizontal, both = false)

    /** 宽高都清掉：连滑动空间都不留，用在必须彻底消失的项上 */
    fun dropFully(item: View?) = shrink(item, horizontal = false, both = true)

    /** 同一个 View 被回收去显示正常内容时恢复原样 */
    fun restore(item: View?) {
        if (item == null) return
        val saved = origin.remove(item) ?: return
        item.layoutParams?.let {
            it.width = saved[0]
            it.height = saved[1]
            item.layoutParams = it
        }
        if (item.visibility != saved[2]) item.visibility = saved[2]
    }

    /** 遍历一个列表：命中的收掉，没命中的复原（防止回收复用串位） */
    fun sweep(list: ViewGroup?, horizontal: Boolean, hit: (View) -> Boolean) {
        if (list == null) return
        for (i in 0 until list.childCount) {
            val item = list.getChildAt(i) ?: continue
            if (hit(item)) drop(item, horizontal) else restore(item)
        }
    }

    private fun shrink(item: View?, horizontal: Boolean, both: Boolean) {
        if (item == null) return
        val lp = item.layoutParams ?: run { hide(item); return }
        if (!origin.containsKey(item)) {
            origin[item] = intArrayOf(lp.width, lp.height, item.visibility)
        }
        hide(item)

        var changed = false
        if ((both || horizontal) && lp.width != 0) {
            lp.width = 0
            changed = true
        }
        if ((both || !horizontal) && lp.height != 0) {
            lp.height = 0
            changed = true
        }
        if (changed) {
            item.layoutParams = lp
            requestNearestScroll(item)
        }
    }

    /** 让最近的滚动容器重量一次，否则横向滚动容器仍按旧内容宽度算可滚范围 */
    private fun requestNearestScroll(item: View) {
        var cursor: View? = item.parent as? View
        var depth = 0
        while (cursor != null && depth < 5) {
            val group = cursor as? ViewGroup
            if (cursor is android.widget.HorizontalScrollView || (group != null && Dom.isList(group))) {
                cursor.requestLayout()
                return
            }
            cursor = cursor.parent as? View
            depth++
        }
    }

    /** 给 Block 用：这个 id 属于哪个功能（日志里显示名字） */
    fun packageId(name: String): String = "${Target.PACKAGE}:id/$name"
}
