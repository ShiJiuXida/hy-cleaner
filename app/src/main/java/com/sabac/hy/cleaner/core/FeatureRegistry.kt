package com.sabac.hy.cleaner.core

import com.sabac.hy.cleaner.feature.BottomShortcuts
import com.sabac.hy.cleaner.feature.EntertainmentTab
import com.sabac.hy.cleaner.feature.EntryRow
import com.sabac.hy.cleaner.feature.FilterRecommend
import com.sabac.hy.cleaner.feature.InputGiftAd
import com.sabac.hy.cleaner.feature.MicFloatAd
import com.sabac.hy.cleaner.feature.Nobility
import com.sabac.hy.cleaner.feature.PanelShortcuts
import com.sabac.hy.cleaner.feature.PinnedEntries
import com.sabac.hy.cleaner.feature.RollBanner
import com.sabac.hy.cleaner.feature.RoomChannelCard
import com.sabac.hy.cleaner.feature.SuperPlayer
import com.sabac.hy.cleaner.feature.ToastBanner
import com.sabac.hy.cleaner.feature.TopRightAd

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
    )

    /** 按分组归类，给设置界面用（组内顺序沿用上面的顺序） */
    fun byGroup(): Map<FeatureGroup, List<Feature>> =
        FeatureGroup.entries.associateWith { group -> all.filter { it.group == group } }
            .filterValues { it.isNotEmpty() }
}
