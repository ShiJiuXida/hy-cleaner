package com.sabac.hy.cleaner.feature

import android.view.View
import android.view.ViewGroup
import com.sabac.hy.cleaner.core.Feature
import com.sabac.hy.cleaner.core.FeatureGroup
import com.sabac.hy.cleaner.core.HyLog
import com.sabac.hy.cleaner.core.Settings
import com.sabac.hy.cleaner.view.Dom

/**
 * 顶部滑出来的全服公告条。
 *
 * ViewStub 膨胀之后 stub 自己就不在树里了，藏它没意义；
 * 真正占地方的是膨胀出来的那一层，按 id 收掉。
 */
object ToastBanner : Feature {
    override val key = "room_toast_banner"
    override val group = FeatureGroup.ROOM
    override val title = "全服公告条"
    override val desc = "顶部滑出来的滚动通知"
    override val blockIds = listOf("allNotifyRootLayout", "ctl_notify_native_content")

    override fun apply(d: Dom, s: Settings) {
        d.ids("allNotifyRootLayout").forEach { d.hide(it) }
        d.ids("ctl_notify_native_content").forEach { d.hide(it) }
        d.ids("iv_notify_native_close").forEach { close ->
            d.hide(d.parent(close, "ctl_notify_native_content") ?: close)
        }
    }
}

/** 输入栏右边第一个广告部件 */
object InputGiftAd : Feature {
    override val key = "room_input_ad"
    override val group = FeatureGroup.ROOM
    override val title = "输入栏右侧广告"
    override val desc = "输入框右边第一个部件"
    override val blockIds = listOf("channel_entertainment_gift_layout")

    override fun apply(d: Dom, s: Settings) = d.hide("channel_entertainment_gift_layout")
}

/**
 * 三条杠面板底部的五个付费入口。
 *
 * 面板在 Dialog 自己的窗口里，这里收到的根是 Dialog 的 decor。
 * 认文案；认不到就用「语音降噪」前面五个位置兜底。
 * 用宽高全清而不是只清宽度 —— 只清宽度的话竖向空间还在，能划过去。
 */
object PanelShortcuts : Feature {
    override val key = "room_panel_shortcuts"
    override val group = FeatureGroup.ROOM
    override val title = "三条杠底部付费入口"
    override val desc = "自选商店 / 挚友养成记 / 我的形象 / 超值周卡 / 珍宝馆"

    private val ads = listOf("0.1元自选商店", "挚友养成记", "我的形象", "超值周卡", "珍宝馆")
    private const val MARK = "语音降噪"

    override fun apply(d: Dom, s: Settings) {
        d.ids("channel_bottom_common_list").forEach { node ->
            val list = node as? ViewGroup ?: return@forEach
            val items = d.items(list)
            val markIndex = items.indexOfFirst { title(d, it).contains(MARK) }

            val targets = items.filter { title(d, it) in ads }.toSet() +
                if (markIndex >= 5) items.subList(0, markIndex).toSet() else emptySet()

            items.forEach { item ->
                if (item in targets) d.dropFully(item) else d.restore(item)
            }
            if (targets.isNotEmpty()) HyLog.i("三条杠底排：收掉 ${targets.size} 项")
        }
    }

    /** 优先用布局里那个 id，取不到就取第一段文字 */
    private fun title(d: Dom, item: View): String {
        val scoped = d.within(item, "channel_bottom_detail_content")
        if (scoped != null) return Dom.text(scoped)
        return firstText(item)
    }

    private fun firstText(root: View): String {
        if (root is android.widget.TextView) {
            val t = Dom.text(root)
            if (t.isNotEmpty()) return t
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val t = firstText(root.getChildAt(i))
                if (t.isNotEmpty()) return t
            }
        }
        return ""
    }
}

/** 麦位名称按钮上方那两个来回滚动的广告位 */
object MicFloatAd : Feature {
    override val key = "room_mic_float_ad"
    override val group = FeatureGroup.ROOM
    override val title = "麦位名称上方广告"
    override val desc = "那两个来回滚动的悬浮位"

    override fun apply(d: Dom, s: Settings) {
        val holders = LinkedHashSet<View>()
        listOf("room_adv_banner_layout", "banner_float_adv", "float_layer_img_et").forEach { name ->
            d.ids(name).forEach { leaf ->
                var holder: View = leaf
                var parent = leaf.parent as? View
                // 往上两层，把 46x57 那层小容器一起收掉；碰到大容器就停
                var step = 0
                while (step < 2 && parent != null) {
                    val p = parent!!
                    if (p.width in 1..699 && p.height in 1..699) {
                        holder = p
                        parent = p.parent as? View
                        step++
                    } else {
                        break
                    }
                }
                holders.add(holder)
            }
        }
        holders.forEach { d.hide(it) }
    }
}
