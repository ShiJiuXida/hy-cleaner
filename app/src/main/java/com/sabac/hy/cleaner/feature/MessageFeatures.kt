package com.sabac.hy.cleaner.feature

import android.view.ViewGroup
import com.sabac.hy.cleaner.core.Feature
import com.sabac.hy.cleaner.core.FeatureGroup
import com.sabac.hy.cleaner.core.Settings
import com.sabac.hy.cleaner.view.Dom

/**
 * 「我的房间」右边那张频道卡（恋爱营地）。
 *
 * 卡名由服务端下发、会改，所以按位置认：找到「我的房间」那一项，
 * 它后面第一张好友频道卡就是目标。整项收掉并清宽度，让后面的卡顶上来。
 */
object RoomChannelCard : Feature {
    override val key = "msg_friend_camp"
    override val group = FeatureGroup.MESSAGE
    override val title = "「我的房间」右侧频道卡"
    override val desc = "按位置识别，卡名被改也有效"

    override fun apply(d: Dom, s: Settings) {
        val anchor = d.ids("room_title_tv").firstOrNull() ?: return
        val list = d.listOf(anchor) ?: return

        val items = d.items(list)
        val anchorIndex = items.indexOfFirst { d.has(it, "room_title_tv") }
        if (anchorIndex < 0) return

        val target = items.drop(anchorIndex + 1)
            .firstOrNull { d.has(it, "v_channel_friend_name") }
            ?: return

        d.sweep(list, horizontal = true) { it === target }
    }
}

/** 聊天列表上方固定的「我的好友印记」「欢游活动中心」 */
object PinnedEntries : Feature {
    override val key = "msg_pinned_entries"
    override val group = FeatureGroup.MESSAGE
    override val title = "我的好友印记 / 欢游活动中心"
    override val desc = "聊天列表上方固定的两个入口"

    override fun apply(d: Dom, s: Settings) {
        val lists = LinkedHashSet<ViewGroup>()
        (d.ids("entranceTitle") + d.ids("tv_assign_title")).forEach { marker ->
            val list = d.listOf(marker)
            if (list != null) {
                lists.add(list)
            } else {
                d.hide(d.parent(marker, "item_layout_root") ?: marker)
            }
        }
        lists.forEach { list ->
            d.sweep(list, horizontal = false) { item ->
                d.has(item, "entranceTitle") || d.has(item, "tv_assign_title")
            }
        }
    }
}
