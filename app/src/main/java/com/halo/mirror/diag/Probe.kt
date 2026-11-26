package com.halo.mirror.diag

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.halo.mirror.core.HyLog
import com.halo.mirror.view.Dom

/**
 * 诊断输出。只在 debug 包生效（release 里被 -assumenosideeffects 移除）。
 *
 * 用途：定位新的广告位、确认某个 id 在不在树上、看清楚某个弹窗里到底是什么。
 */
object Probe {

    private var panelPasses = 0
    private var lastSignature = ""
    private var lastAt = 0L

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

    /**
     * 界面树快照。
     *
     * 只在「树上出现了新东西」时才打，避免刷屏：把可见节点的 id/类名拼成一个签名，
     * 签名变了才输出一次完整结构。弹窗出现正好会让签名变化。
     */
    fun snapshot(root: View, tag: String) {
        val now = System.currentTimeMillis()
        if (now - lastAt < 400) return

        val signature = StringBuilder()
        collectSignature(root, signature, 0)
        val sig = signature.toString()
        if (sig == lastSignature) return

        lastSignature = sig
        lastAt = now
        HyLog.i("=== 界面变化 [$tag] 根=${System.identityHashCode(root)} ===")
        tree(root, 400)
    }

    private fun collectSignature(node: View, out: StringBuilder, depth: Int) {
        if (depth > 12) return
        if (node.visibility == View.VISIBLE) {
            val id = runCatching { node.resources.getResourceEntryName(node.id) }.getOrNull()
            out.append(id ?: node.javaClass.simpleName).append(',')
        }
        if (node is ViewGroup) {
            for (i in 0 until node.childCount) collectSignature(node.getChildAt(i), out, depth + 1)
        }
    }

    /** 整棵树导出，用来找没有 id 的广告位或看清弹窗结构 */
    fun tree(root: View, limit: Int = 400) {
        val count = intArrayOf(0)
        dump(root, 0, count, limit)
        HyLog.i("树导出结束 ${count[0]} 节")
    }

    private fun dump(node: View?, depth: Int, count: IntArray, limit: Int) {
        if (node == null || count[0] > limit) return
        // 只打可见分支，减少噪音
        if (node.visibility != View.VISIBLE) return
        count[0]++

        val id = runCatching { node.resources.getResourceEntryName(node.id) }.getOrNull()
        val text = if (node is TextView) Dom.text(node).take(24) else ""
        val pad = "  ".repeat(minOf(depth, 12))
        HyLog.i(
            "$pad${node.javaClass.simpleName}" +
                (id?.let { "#$it" } ?: "") +
                " ${node.width}x${node.height}" +
                (if (text.isNotEmpty()) " 「$text」" else ""),
        )

        if (node is ViewGroup) {
            for (i in 0 until node.childCount) dump(node.getChildAt(i), depth + 1, count, limit)
        }
    }
}
