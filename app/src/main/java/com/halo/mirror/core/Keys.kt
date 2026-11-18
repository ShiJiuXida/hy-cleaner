package com.halo.mirror.core

/**
 * 全局常量。功能开关的键名不在这里 —— 每个功能自己声明（见 [Feature.key]）。
 */
object Keys {
    /** LSPosed remote preferences 分组名 */
    const val GROUP = "prefs"

    /** 总开关 */
    const val ENABLED = "enabled"

    /** 一键停用（保留模块，排查崩溃用） */
    const val PANIC = "panic"
}

/** 欢游的包名与版本基线 */
object Target {
    const val PACKAGE = "com.sabac.hy"
    const val VERSION = "2.88.5"

    /** 规则是按这个版本写的；版本变了只告警不阻断 */
    const val VERSION_CODE = 19623L
}
