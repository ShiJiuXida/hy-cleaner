package com.halo.mirror.feature

import com.halo.mirror.core.Feature
import com.halo.mirror.core.FeatureGroup
import com.halo.mirror.core.HyLog
import com.halo.mirror.core.Settings
import com.halo.mirror.view.Dom

/**
 * 弹窗类功能：和别的 Feature 不一样，目标不是「藏控件」而是「整只窗口关掉」。
 *
 * 所以只声明两条：[matches] 判断这棵树是不是目标弹窗，[close] 负责把它关掉。
 * 拦截和清扫都在 [com.halo.mirror.hook.PopupGuard] 里做 —— 弹窗是窗口，
 * 只 GONE 控件的做法会留下一层挡住点击的透明窗口。
 */
interface PopupFeature : Feature {

    /** 这棵窗口根视图是不是目标弹窗 */
    fun matches(d: Dom): Boolean

    /** 关掉已经显示出来的那个实例；返回 true 表示确实点到了关闭 */
    fun close(d: Dom): Boolean

    /** 弹窗不走「按 id 藏控件」那套，清扫流程里不需要额外动作 */
    override fun apply(d: Dom, s: Settings) = Unit
}

/**
 * 启动时的「版本更新」对话框。
 *
 * 结构上没有什么可靠的容器 id（整块是服务端文案 + 两个按钮），
 * 按文案认：标题出现「版本更新」，并且带着「立刻更新 / 立即更新」这类按钮。
 * 只认标题不认按钮的话，公告里出现「版本更新」四个字也会被误伤。
 */
object UpdatePrompt : PopupFeature {
    override val key = "popup_update"
    override val group = FeatureGroup.POPUP
    override val title = "版本更新提示"
    override val desc = "打开时弹的「版本更新」对话框"

    private const val TITLE = "版本更新"
    private val ACTIONS = listOf("立刻更新", "立即更新", "马上更新", "立即升级")
    private val CLOSES = listOf("稍后更新", "稍后再说", "以后再说", "暂不更新", "取消", "关闭")

    override fun matches(d: Dom): Boolean {
        if (d.textsContaining(TITLE).isEmpty()) return false
        return ACTIONS.any { d.textsContaining(it).isNotEmpty() }
    }

    override fun close(d: Dom): Boolean {
        for (text in CLOSES) {
            val node = d.texts(text).firstOrNull() ?: continue
            val target = d.clickableOf(node) ?: node
            if (target.performClick()) {
                HyLog.i("点击更新弹窗的「$text」")
                return true
            }
        }
        return false
    }
}

/**
 * 登录后弹的全屏抽奖广告（那张宝箱图 + 右上角 X）。
 *
 * 有稳定的 id，按 id 认，比按图/按尺寸猜可靠：
 * `loginLotteryLayout` 是整块容器，`loginLotteryClose` 是右上角关闭。
 * 两个都在才算 —— 主页顶栏那个抽奖入口是另一个 id（loginLotteryEntranceLayout），
 * 不会误伤。
 */
object LoginLotteryAd : PopupFeature {
    override val key = "popup_ad"
    override val group = FeatureGroup.POPUP
    override val title = "登录抽奖广告"
    override val desc = "登录后弹的全屏抽奖广告，右上角带 X"

    private const val LAYOUT = "loginLotteryLayout"
    private const val CLOSE = "loginLotteryClose"

    override fun matches(d: Dom): Boolean =
        d.id(LAYOUT) != null && d.id(CLOSE) != null

    override fun close(d: Dom): Boolean {
        val close = d.id(CLOSE) ?: return false
        val target = d.clickableOf(close) ?: close
        val clicked = target.performClick()
        if (clicked) HyLog.i("点击抽奖广告的关闭按钮")
        return clicked
    }
}
