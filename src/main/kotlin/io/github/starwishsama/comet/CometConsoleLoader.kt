package io.github.starwishsama.comet

import com.google.auto.service.AutoService
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import kotlin.time.ExperimentalTime

/**
 * Comet 的 Console 插件部分
 *
 * 注意: 使用 Console 启动是实验性功能!
 * 由于 Comet 的设计缺陷, 在 Console 有多个机器人的情况下可能无法正常工作.
 */
@AutoService(JvmPlugin::class)
object CometConsoleLoader : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.starwishsama.comet",
        version = "0.6-M2",
    ) {
        name("Comet")
    }
) {

    @ExperimentalTime
    override fun onEnable() {

        logger.info("Comet 已启动, 正在初始化 ${Bot.instances} 个 Bot...")

        if (Bot.instances.isEmpty()) {
            logger.warning("找不到已登录的 Bot, 请登录后重启 Console 重试!")
            return
        }

        Bot.instances.forEach { bot ->
            Comet.invokePostTask(bot, logger)
        }
    }

    override fun onDisable() {
        invokeWhenClose()
    }
}