package com.sabac.hy.cleaner.core

import android.util.Log
import io.github.libxposed.api.XposedInterface

/**
 * 日志只走框架：LSPosed 会收集到管理器日志页，同时进 logcat（tag LSPosedFramework）。
 * 模块 App 自身看不到这些日志。
 */
object HyLog {

    const val TAG = "HYClean"

    @Volatile
    private var sink: XposedInterface? = null

    fun bind(api: XposedInterface) {
        sink = api
    }

    fun i(message: String) {
        runCatching { sink?.log(Log.INFO, TAG, message) }
    }

    fun w(message: String) {
        runCatching { sink?.log(Log.WARN, TAG, message) }
    }
}
