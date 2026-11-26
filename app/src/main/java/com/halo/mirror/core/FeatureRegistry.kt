package com.halo.mirror.core

import com.halo.mirror.feature.BottomShortcuts
import com.halo.mirror.feature.EntertainmentTab
import com.halo.mirror.feature.EntryRow
import com.halo.mirror.feature.FilterRecommend
import com.halo.mirror.feature.InputGiftAd
import com.halo.mirror.feature.LoginLotteryAd
import com.halo.mirror.feature.MicFloatAd
import com.halo.mirror.feature.Nobility
import com.halo.mirror.feature.PanelShortcuts
import com.halo.mirror.feature.PinnedEntries
import com.halo.mirror.feature.RollBanner
import com.halo.mirror.feature.RoomChannelCard
import com.halo.mirror.feature.SuperPlayer
import com.halo.mirror.feature.ToastBanner
import com.halo.mirror.feature.TopRightAd
import com.halo.mirror.feature.UpdatePrompt

/**
 * 所有功能的唯一清单。
 *
 * 加一个功能 = 写一个 Feature 实现 + 在这里加一行；
 * 设置界面、清扫流程、可见性拦截名单都从这里读，不需要别处再同步。
 */
object FeatureRegistry {

    val all: List<Feature> = listOf(
        // 主页
        EntertainmentTab,
        TopRightAd,
        EntryRow,
        FilterRecommend,
        RollBanner,
        BottomShortcuts,
        // 消息
        RoomChannelCard,
        PinnedEntries,
        // 我的
        Nobility,
        SuperPlayer,
        // 房间
        ToastBanner,
        InputGiftAd,
        PanelShortcuts,
        MicFloatAd,
        // 弹窗
        UpdatePrompt,
        LoginLotteryAd,
    )

    /** 按分组归类，给设置界面用（组内顺序沿用上面的顺序） */
    fun byGroup(): Map<FeatureGroup, List<Feature>> =
        FeatureGroup.entries.associateWith { group -> all.filter { it.group == group } }
            .filterValues { it.isNotEmpty() }
}
