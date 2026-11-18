package com.halo.mirror.core

import android.app.Application
import android.os.Build

/** 当前进程名。用于把 hook 限制在界面进程，push / 保活子进程没有 View 树。 */
object Process {

    @Volatile
    private var cached: String? = null

    fun name(): String {
        cached?.let { return it }
        val value = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Application.getProcessName()
            } else {
                Class.forName("android.app.ActivityThread")
                    .getMethod("currentProcessName")
                    .invoke(null) as? String
            }
        }.getOrNull().orEmpty()
        cached = value
        return value
    }

    /** 只有主进程需要处理界面 */
    fun isMain(): Boolean = name() == Target.PACKAGE || name().isEmpty()
}
