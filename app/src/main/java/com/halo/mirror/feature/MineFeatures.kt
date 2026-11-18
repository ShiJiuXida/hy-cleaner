package com.halo.mirror.feature

import com.halo.mirror.core.Feature
import com.halo.mirror.core.FeatureGroup
import com.halo.mirror.core.Settings
import com.halo.mirror.view.Dom

/** 「我的」页顶部贵族入口 */
object Nobility : Feature {
    override val key = "mine_nobility"
    override val group = FeatureGroup.MINE
    override val title = "贵族特权"
    override val desc = "顶部贵族入口"
    override val blockIds = listOf("fl_user_detail_nobility", "rv_user_detail_nobility")

    override fun apply(d: Dom, s: Settings) {
        d.hide("fl_user_detail_nobility")
        d.hide("rv_user_detail_nobility")
        d.hide("nobilityTipArrow")
    }
}

/** 「我的」页那条开通超级玩家横幅 */
object SuperPlayer : Feature {
    override val key = "mine_super_player"
    override val group = FeatureGroup.MINE
    override val title = "开通欢游超级玩家"
    override val desc = "那条横幅"
    override val blockIds = listOf("fl_user_detail_vip_entry")

    override fun apply(d: Dom, s: Settings) = d.hide("fl_user_detail_vip_entry")
}
