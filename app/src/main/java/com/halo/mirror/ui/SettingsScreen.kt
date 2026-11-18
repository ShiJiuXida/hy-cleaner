package com.halo.mirror.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halo.mirror.AppBridge
import com.halo.mirror.core.FeatureGroup
import com.halo.mirror.core.FeatureRegistry
import com.halo.mirror.core.Keys
import com.halo.mirror.core.Target

/** 设置页的一行：总开关和功能开关共用 */
private class Row(val key: String, val title: String, val desc: String)

private val MASTER = listOf(
    Row(Keys.ENABLED, "启用模块", "关掉后完全不介入欢游"),
    Row(Keys.PANIC, "一键停用", "保留模块，排错用"),
)

private fun defaults(): Map<String, Boolean> = buildMap {
    put(Keys.ENABLED, true)
    put(Keys.PANIC, false)
    FeatureRegistry.all.forEach { put(it.key, true) }
}

@Composable
fun SettingsScreen() {
    val service by AppBridge.service.collectAsState()
    var state by remember { mutableStateOf(defaults()) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(service) {
        val prefs = AppBridge.prefs() ?: return@LaunchedEffect
        val next = defaults().toMutableMap()
        next[Keys.ENABLED] = prefs.getBoolean(Keys.ENABLED, true)
        next[Keys.PANIC] = prefs.getBoolean(Keys.PANIC, false)
        FeatureRegistry.all.forEach { next[it.key] = prefs.getBoolean(it.key, true) }
        state = next
        failed = false
    }

    fun toggle(key: String, value: Boolean) {
        val prefs = AppBridge.prefs()
        if (prefs == null) {
            failed = true
            return
        }
        prefs.edit().putBoolean(key, value).apply()
        state = state + (key to value)
        failed = false
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header(connected = service != null, failed = failed) }
            item { Section("总开关", MASTER, state, ::toggle) }
            FeatureRegistry.byGroup().forEach { (group, features) ->
                item {
                    Section(
                        title = group.label,
                        rows = features.map { Row(it.key, it.title, it.desc) },
                        state = state,
                        onToggle = ::toggle,
                    )
                }
            }
            item { Footer() }
        }
    }
}

@Composable
private fun Header(connected: Boolean, failed: Boolean) {
    Column(Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp)) {
        Text("Mirror", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "界面布局调整 · 欢游 ${Target.VERSION}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        val (text, warn) = when {
            failed -> "写入失败，LSPosed 里确认作用域 ${Target.PACKAGE}" to true
            connected -> "已连接 LSPosed" to false
            else -> "未连接 LSPosed，开关无法保存" to true
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (warn) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(
    title: String,
    rows: List<Row>,
    state: Map<String, Boolean>,
    onToggle: (String, Boolean) -> Unit,
) {
    Column {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column {
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                    SwitchRow(
                        row = row,
                        checked = state[row.key] ?: true,
                        onToggle = onToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(row: Row, checked: Boolean, onToggle: (String, Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(row.key, !checked) }
            .padding(start = 16.dp, end = 12.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                row.desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(10.dp))
        Switch(checked = checked, onCheckedChange = { onToggle(row.key, it) })
    }
}

@Composable
private fun Footer() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("说明", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            listOf(
                "改完开关，在欢游里切一次页面就会生效，不用重启。",
                "release 包无日志；排错用 debug 包，标记 Mirror。",
                "清理只作用于界面控件，不改动欢游的任何数据。",
            ).forEach {
                Text(
                    "· $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}
