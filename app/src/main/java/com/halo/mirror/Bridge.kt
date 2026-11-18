package com.halo.mirror

import android.app.Application
import android.content.SharedPreferences
import com.halo.mirror.core.Keys
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 模块 App 与框架的连接。
 *
 * 框架通过 ContentProvider 把 binder 推过来，App 侧拿到之后才有「写配置」的能力。
 * 写不进去就说明模块没被 LSPosed 激活，界面会直接提示，不偷偷写本地。
 */
object AppBridge : XposedServiceHelper.OnServiceListener {

    private val _service = MutableStateFlow<XposedService?>(null)
    val service: StateFlow<XposedService?> = _service.asStateFlow()

    fun init() = runCatching { XposedServiceHelper.registerListener(this) }

    override fun onServiceBind(service: XposedService) {
        _service.value = service
    }

    override fun onServiceDied(service: XposedService) {
        _service.value = null
    }

    fun prefs(): SharedPreferences? =
        runCatching { _service.value?.getRemotePreferences(Keys.GROUP) }.getOrNull()
}

class HyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppBridge.init()
    }
}
