package com.sabac.hy.cleaner.core

import android.os.SystemClock
import io.github.libxposed.api.XposedInterface

/** 当前生效的开关。注入进程只读，写入由模块 App 负责。 */
class Settings(private val map: Map<String, Boolean>) {

    val enabled: Boolean get() = map[Keys.ENABLED] ?: true
    val panic: Boolean get() = map[Keys.PANIC] ?: false

    /** 没写过的键按开启处理 */
    fun on(key: String): Boolean = map[key] ?: true

    fun size(): Int = map.size
}

/**
 * 配置读取。
 *
 * LSPosed 的 remote preferences 是单向的：App 写、注入进程读。
 * 这里做节流刷新，改完开关在欢游里切个页面就生效，不用杀进程。
 */
object Live {

    private const val RELOAD_MS = 1500L

    @Volatile
    private var api: XposedInterface? = null

    @Volatile
    var settings: Settings = Settings(emptyMap())
        private set

    private var lastReload = 0L

    fun attach(xposed: XposedInterface): Boolean {
        api = xposed
        reload(force = true)
        return true
    }

    fun reload(force: Boolean = false) {
        val xposed = api ?: return
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastReload < RELOAD_MS) return
        lastReload = now
        runCatching {
            val raw = xposed.getRemotePreferences(Keys.GROUP).all
            settings = Settings(
                raw.mapNotNull { (k, v) -> (v as? Boolean)?.let { k to it } }.toMap(),
            )
        }
    }
}
