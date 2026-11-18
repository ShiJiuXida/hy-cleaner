package com.sabac.hy.cleaner.core

import com.sabac.hy.cleaner.view.Dom

/** 设置页上的分组 */
enum class FeatureGroup(val label: String) {
    HOME("主页"),
    MESSAGE("消息"),
    MINE("我的"),
    ROOM("房间"),
}

/**
 * 一个开关 = 一个功能。
 *
 * 标题、说明、规则、拦截名单全在这里，设置界面直接读这份定义，
 * 不再有「键名一处、文案一处、规则一处」的重复。
 */
interface Feature {
    /** 配置键，同时是 remote preferences 里的字段名 */
    val key: String

    val group: FeatureGroup
    val title: String
    val desc: String

    /**
     * 只要这个开关开着，这些 id 的控件就不允许变可见。
     *
     * 只在「一个 id 只服务一种用途」时填 —— 像 item_layout_root、tab_filter_item_root
     * 这类被大量复用的 id 不能放进来，否则会把正常内容一起收掉。
     */
    val blockIds: List<String> get() = emptyList()

    fun apply(d: Dom, s: Settings)

    /** 让注册表统一判断，避免每个功能都写一遍 if */
    fun applyIfOn(d: Dom, s: Settings) {
        if (!s.on(key)) return
        runCatching { apply(d, s) }
            .onFailure { HyLog.w("规则异常 key=$key ${it.message}") }
    }
}
