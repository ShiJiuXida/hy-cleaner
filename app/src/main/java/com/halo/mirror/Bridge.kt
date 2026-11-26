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

    /** 拿过一次就留着：RemotePreferences 本身是 binder 的薄封装，重复创建没意义 */
    @Volatile
    private var cached: SharedPreferences? = null

    /** 最近一次失败的原因，给界面显示用，不是 null 就说明上一次动作为什么没成 */
    @Volatile
    var lastError: String? = null
        private set

    fun init() = runCatching { XposedServiceHelper.registerListener(this) }

    override fun onServiceBind(service: XposedService) {
        cached = null
        _service.value = service
    }

    override fun onServiceDied(service: XposedService) {
        cached = null
        _service.value = null
    }

    fun prefs(): SharedPreferences? {
        cached?.let { return it }
        val svc = _service.value ?: run {
            lastError = "未连接 LSPosed"
            return null
        }
        return runCatching { svc.getRemotePreferences(Keys.GROUP) }
            .onFailure { lastError = "读取配置失败：${it.javaClass.simpleName} ${it.message.orEmpty()}" }
            .getOrNull()
            ?.also { cached = it }
    }

    /**
     * 写一个开关。
     *
     * 用 [SharedPreferences.Editor.commit] 而不是 apply：这里的 SharedPreferences 是框架
     * 转发的 binder 实现，apply 的「异步落盘」语义对它不一定成立，而且拿不到返回值 ——
     * 之前就是 apply 完之后无条件当成功，把真正的原因盖掉了。
     */
    fun save(key: String, value: Boolean): Boolean {
        val p = prefs() ?: return false
        return runCatching {
            val ok = p.edit().putBoolean(key, value).commit()
            if (!ok) lastError = "框架拒绝写入（commit 返回 false）"
            ok
        }.onFailure {
            lastError = "写入失败：${it.javaClass.simpleName} ${it.message.orEmpty()}"
        }.getOrDefault(false)
    }

    /** 成功时顺便清掉上一次的错误 */
    fun clearError() {
        lastError = null
    }
}

class HyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppBridge.init()
    }
}
