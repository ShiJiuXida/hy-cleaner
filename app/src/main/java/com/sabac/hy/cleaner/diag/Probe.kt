package com.sabac.hy.cleaner.diag

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sabac.hy.cleaner.core.HyLog
import com.sabac.hy.cleaner.view.Dom

/**
 * 诊断输出。只在需要看清树上有什么时用，正常流程里不会调用。
 *
 * 代码先留着（用户要求）：定位新的广告位、确认某个 id 在不在树上，都靠它。
 */
object Probe {

    private var panelPasses = 0
    private var windowPasses = 0

    /** 三条杠这类面板：把每一项的文案、尺寸、可见性打出来 */
    fun panel(d: Dom) {
        if (panelPasses >= 8) return
        panelPasses++

        listOf("channel_bottom_common_list", "channel_bottom_manager_list_1", "channel_bottom_manager_list_2")
            .forEach { name ->
                d.ids(name).forEachIndexed { index, node ->
                    val group = node as? ViewGroup ?: return@forEachIndexed
                    val kids = d.items(group)
                    val desc = kids.joinToString(" | ") { item ->
                        val text = d.within(item, "channel_bottom_detail_content")?.let { Dom.text(it) } ?: "?"
                        "「$text」${item.width}x${item.height} v${item.visibility}"
                    }
                    HyLog.i("面板 $name#$index 子=${kids.size}  $desc")
                }
            }
    }

    /** 窗口概况：确认扫到的是屏幕上那个窗口 */
    fun window(root: View) {
        if (windowPasses >= 30) return
        windowPasses++
        val d = Dom(root)
        val lists = d.ids("gameChannelRecyclerView")
        val info = lists.joinToString(",") { l ->
            "${(l as? ViewGroup)?.childCount ?: -1}/${l.height}"
        }
        var nodes = 0
        d.walk { nodes++ }
        HyLog.i("窗口 ${System.identityHashCode(root)} 节=$nodes 列表=${lists.size}[$info]")
    }

    /** 整棵树导出，用来找没有 id 的广告位 */
    fun tree(root: View, limit: Int = 600) {
        val count = intArrayOf(0)
        dump(root, 0, count, limit)
        HyLog.i("树导出完成 ${count[0]} 节")
    }

    private fun dump(node: View?, depth: Int, count: IntArray, limit: Int) {
        if (node == null || count[0] > limit) return
        count[0]++
        val id = runCatching { node.resources.getResourceEntryName(node.id) }.getOrNull()
        val text = if (node is TextView) Dom.text(node).take(20) else ""
        HyLog.i(
            "节 d=$depth ${node.javaClass.simpleName}" +
                (id?.let { " id=$it" } ?: "") +
                " v=${node.visibility} ${node.width}x${node.height}" +
                (if (text.isNotEmpty()) " t=$text" else ""),
        )
        if (node is ViewGroup) {
            for (i in 0 until node.childCount) dump(node.getChildAt(i), depth + 1, count, limit)
        }
    }
}
