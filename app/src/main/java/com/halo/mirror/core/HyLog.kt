package com.halo.mirror.core

import io.github.libxposed.api.XposedInterface

/**
 * 日志。
 *
 * debug 包正常输出；release 包里所有调用点都被 R8 折叠成空操作
 * （[ENABLED] 是编译期常量 false），连「往 logcat 写过什么」这件事都不存在。
 *
 * 这不是洁癖：目标应用持有 READ_LOGS 权限，如果日志里有
 * 「已挂载 4/4」「收起 home_discover_rel」这种明文，等于自己把痕迹写进公共缓冲区。
 */
object HyLog {

    private const val TAG = "Mirror"

    @Volatile
    private var sink: XposedInterface? = null

    fun bind(api: XposedInterface) {
        if (!ENABLED) return
        sink = api
    }

    fun i(message: String) {
        if (!ENABLED) return
        runCatching { sink?.log(android.util.Log.INFO, TAG, message) }
    }

    fun w(message: String) {
        if (!ENABLED) return
        runCatching { sink?.log(android.util.Log.WARN, TAG, message) }
    }

    /**
     * 由构建类型决定：debug=true、release=false。
     * R8 会把 release 里的分支直接折叠掉。
     */
    val ENABLED: Boolean = com.halo.mirror.BuildConfig.DEBUG
}
