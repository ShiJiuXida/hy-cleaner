package com.halo.mirror.feature

import android.view.View
import com.halo.mirror.core.Feature
import com.halo.mirror.core.FeatureGroup
import com.halo.mirror.core.Settings
import com.halo.mirror.view.Dom
import com.halo.mirror.view.Layouts

/** 顶部「娱乐」页签。只 GONE：页签栏高度是行内定死的，动尺寸会整行空掉 */
object EntertainmentTab : Feature {
    override val key = "home_ent_tab"
    override val group = FeatureGroup.HOME
    override val title = "顶部「娱乐」页签"
    override val desc = "页签去掉，右划也进不去"
    override val blockIds = listOf("channel_tab_title_ll")

    override fun apply(d: Dom, s: Settings) {
        d.ids("channel_tab_title")
            .filter { Dom.text(it) == "娱乐" }
            .forEach { d.hide(it.parent as? View ?: it) }
    }
}

/** 顶栏最右那个抽奖广告位 */
object TopRightAd : Feature {
    override val key = "home_top_right_ad"
    override val group = FeatureGroup.HOME
    override val title = "顶栏右侧抽奖位"
    override val desc = "顶栏最右那个限时豆豆入口"
    override val blockIds = listOf("loginLotteryEntranceLayout")

    override fun apply(d: Dom, s: Settings) = d.hide("loginLotteryEntranceLayout")
}

/** 立即开局 / 心动速配 / 音乐专区 / 娱乐听歌 / 听Pia戏 整排 */
object EntryRow : Feature {
    override val key = "home_entry_row"
    override val group = FeatureGroup.HOME
    override val title = "入口整排"
    override val desc = "立即开局 / 心动速配 / 音乐专区 / 娱乐听歌 / 听Pia戏"
    override val blockIds = listOf("ll_stub_group")

    /** 容器 id 变了也能兜住 */
    private val fallback = listOf(
        "立即开局", "立即开玩", "心动速配", "音乐专区", "娱乐听歌", "听Pia戏", "听PAI戏",
    )

    override fun apply(d: Dom, s: Settings) {
        d.ids("ll_stub_group").forEach { d.hide(it) }
        fallback.forEach { text ->
            d.texts(text).forEach { d.hide(d.clickableOf(it)) }
        }
    }
}

/**
 * 筛选条里的「为你推荐」。
 *
 * 藏到这一项自己的根就停：卡片底图一起收掉，又不会碰到所有页签共用的容器
 * （动了那一层，整行会空出一大片）。
 */
object FilterRecommend : Feature {
    override val key = "home_filter_recommend"
    override val group = FeatureGroup.HOME
    override val title = "「为你推荐」"
    override val desc = "进房一起玩下面筛选里的那一项"

    override fun apply(d: Dom, s: Settings) {
        d.ids("tab_filter_item_name_tv")
            .filter { Dom.text(it) == "为你推荐" }
            .forEach { label -> d.hide(d.parent(label, "tab_filter_item_root") ?: label) }
    }
}

/** 筛选下方来回滚动的那条横幅。它是列表项，必须清高度，否则位置空着 */
object RollBanner : Feature {
    override val key = "home_roll_banner"
    override val group = FeatureGroup.HOME
    override val title = "筛选下方滚动横幅"
    override val desc = "列表顶部来回滚动的那条广告"

    override fun apply(d: Dom, s: Settings) {
        // 每个页签各有一套列表，都得处理
        d.ids("gameChannelRecyclerView").forEach { node ->
            val list = node as? android.view.ViewGroup ?: return@forEach
            d.sweep(list, horizontal = false) { item ->
                if (d.has(item, "gameChannelBanner")) return@sweep true
                // 房间卡片里没有翻页器；横幅是自带翻页器的那种条
                Dom.hasPager(item) && !d.has(item, "gameRootBg")
            }
        }
    }
}

/** 底栏「广场」与它右边的开房入口，摘掉后剩下三个平分宽度 */
object BottomShortcuts : Feature {
    override val key = "home_bottom_shortcuts"
    override val group = FeatureGroup.HOME
    override val title = "底栏广场与开房"
    override val desc = "两个一起去掉，剩下三个平分"
    override val blockIds = listOf("home_discover_rel", "home_room_rel")

    override fun apply(d: Dom, s: Settings) {
        val home = d.ids("home_channel").firstOrNull() ?: return
        val message = d.ids("home_message_rel").firstOrNull() ?: return
        val mine = d.ids("home_myself_rel").firstOrNull() ?: return
        val bar = home.parent as? android.view.ViewGroup ?: return

        d.hide("home_discover_rel")
        d.hide("home_room_rel")

        val width = if (bar.width > 0) bar.width else bar.measuredWidth
        if (width <= 0) return
        val slot = width / 3
        Layouts.pin(home, slot, 0f)
        Layouts.pin(message, slot, 0.5f)
        Layouts.pin(mine, slot, 1f)
    }
}
