package com.sabac.hy.cleaner.view

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sabac.hy.cleaner.core.Target

/**
 * View 树查询。只读，不改任何东西；变更走 [Mutate]。
 *
 * 隐藏规则用到两种定位：资源 id（稳定）和文案（兜底，例如被服务端改名的入口）。
 */
class Dom(root: View) {

    val view: View = root
    private val res = root.resources

    // ---------------------------------------------------------------- 查

    /** 按资源 id 名找第一个 */
    fun id(name: String): View? = rawId(name).takeIf { it != 0 }?.let { view.findViewById(it) }

    /** 按资源 id 名找全部（同名控件可能在多个页面各有一套） */
    fun ids(name: String): List<View> {
        val key = rawId(name)
        if (key == 0) return emptyList()
        val out = ArrayList<View>(2)
        walk { if (it.id == key) out.add(it) }
        return out
    }

    /** 在某个子树里按 id 名找 */
    fun within(root: View?, name: String): View? {
        val key = rawId(name)
        if (root == null || key == 0) return null
        var hit: View? = null
        each(root) { if (hit == null && it.id == key) hit = it }
        return hit
    }

    /** 按文案找全部（去掉首尾空白后完全相等） */
    fun texts(value: String): List<View> = collect { text(it) == value }

    /** 按文案找全部（包含即可） */
    fun textsContaining(value: String): List<View> = collect { text(it).contains(value) }

    /** 列表的直接子项 */
    fun items(list: ViewGroup?): List<View> {
        if (list == null) return emptyList()
        return (0 until list.childCount).mapNotNull { list.getChildAt(it) }
    }

    /** 往上找最近的指定 id 的祖先（含自身） */
    fun parent(node: View?, name: String): View? {
        val key = rawId(name)
        if (node == null || key == 0) return null
        var cursor: View? = node
        var depth = 0
        while (cursor != null && depth < 10) {
            if (cursor.id == key) return cursor
            cursor = cursor.parent as? View
            depth++
        }
        return null
    }

    /** 往上找最近的列表容器 */
    fun listOf(node: View?): ViewGroup? {
        var cursor = node?.parent
        var depth = 0
        while (cursor != null && depth < 10) {
            if (cursor is ViewGroup && isList(cursor)) return cursor
            cursor = cursor.parent
            depth++
        }
        return null
    }

    /** 往上找第一个可点击的祖先（含自身） */
    fun clickableOf(node: View?): View? {
        var cursor = node
        var depth = 0
        while (cursor != null && depth < 6) {
            if (cursor.isClickable) return cursor
            cursor = cursor.parent as? View
            depth++
        }
        return cursor
    }

    /** 子树里有没有某个 id 的控件 */
    fun has(node: View?, name: String): Boolean = within(node, name) != null

    /** 给 Block 用：直接拿 id 整数 */
    fun rawId(name: String): Int = CACHE.getOrPut(name) {
        runCatching { res.getIdentifier(name, "id", Target.PACKAGE) }.getOrDefault(0)
    } ?: 0

    // ---------------------------------------------------------------- 变更（转发给 Mutate）

    fun hide(node: View?) = Mutate.hide(node)

    /** 同名控件可能有多份，全部收掉 */
    fun hide(name: String) = ids(name).forEach { Mutate.hide(it) }

    fun drop(item: View?, horizontal: Boolean) = Mutate.drop(item, horizontal)

    fun dropFully(item: View?) = Mutate.dropFully(item)

    fun restore(item: View?) = Mutate.restore(item)

    fun sweep(list: ViewGroup?, horizontal: Boolean, hit: (View) -> Boolean) =
        Mutate.sweep(list, horizontal, hit)

    // ---------------------------------------------------------------- 遍历

    fun walk(visit: (View) -> Unit) = each(view, visit)

    private fun collect(predicate: (View) -> Boolean): List<View> {
        val out = ArrayList<View>(2)
        walk { if (predicate(it)) out.add(it) }
        return out
    }

    private fun each(node: View?, visit: (View) -> Unit) {
        if (node == null) return
        visit(node)
        if (node is ViewGroup) {
            for (i in 0 until node.childCount) each(node.getChildAt(i), visit)
        }
    }

    companion object {
        private val CACHE = HashMap<String, Int>(32)

        fun text(node: View?): String =
            (node as? TextView)?.text?.toString()?.trim() ?: ""

        fun isList(group: ViewGroup): Boolean = extendsClass(group.javaClass, RECYCLER) ||
            extendsClass(group.javaClass, GRID) ||
            extendsClass(group.javaClass, ABS_LIST)

        /** 是不是翻页器。必须沿父类找：欢游用的是 ViewPager 的子类 */
        fun hasPager(node: View): Boolean {
            if (extendsClass(node.javaClass, PAGER)) return true
            if (node is ViewGroup) {
                for (i in 0 until node.childCount) {
                    if (hasPager(node.getChildAt(i))) return true
                }
            }
            return false
        }

        fun extendsClass(start: Class<*>, target: String): Boolean {
            var cursor: Class<*>? = start
            while (cursor != null && cursor != Any::class.java) {
                if (target == cursor.name) return true
                cursor = cursor.superclass
            }
            return false
        }

        private const val RECYCLER = "androidx.recyclerview.widget.RecyclerView"
        private const val GRID = "android.widget.GridView"
        private const val ABS_LIST = "android.widget.AbsListView"
        private const val PAGER = "androidx.viewpager.widget.ViewPager"
    }
}
