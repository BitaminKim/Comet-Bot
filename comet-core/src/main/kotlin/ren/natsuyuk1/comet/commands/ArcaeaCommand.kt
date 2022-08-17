package ren.natsuyuk1.comet.commands

import moe.sdl.yac.core.subcommands
import moe.sdl.yac.parameters.options.option
import ren.natsuyuk1.comet.api.Comet
import ren.natsuyuk1.comet.api.command.*
import ren.natsuyuk1.comet.api.user.CometUser
import ren.natsuyuk1.comet.commands.service.ArcaeaService
import ren.natsuyuk1.comet.util.toMessageWrapper
import ren.natsuyuk1.comet.utils.message.MessageWrapper
import ren.natsuyuk1.comet.utils.string.StringUtil.toArgs

val ARCAEA by lazy {
    CommandProperty(
        "arcaea",
        listOf("arc", "阿卡伊"),
        "查询 Arcaea 相关信息",
        """
        /arc bind -i [账号 ID] - 绑定账号
        /arc info (账号 ID) 查询绑定账号用户信息
        """.trimIndent(),
        executeConsumePoint = 30
    )
}

class ArcaeaCommand(
    comet: Comet,
    override val sender: PlatformCommandSender,
    override val subject: PlatformCommandSender,
    val message: MessageWrapper,
    val user: CometUser
) : CometCommand(comet, sender, subject, message, user, ARCAEA) {

    init {
        subcommands(
            Bind(subject, sender, user),
            Info(comet, subject, sender, user),
        )
    }

    override suspend fun run() {
        if (message.parseToString().toArgs().size == 1) {
            subject.sendMessage(ARCAEA.helpText.toMessageWrapper())
        }
    }

    class Bind(
        override val subject: PlatformCommandSender,
        override val sender: PlatformCommandSender,
        override val user: CometUser
    ) : CometSubCommand(subject, sender, user, BIND) {

        companion object {
            val BIND = SubCommandProperty(
                "bind",
                listOf("绑定"),
                ARCAEA
            )
        }

        private val userID by option(
            "-i", "--id",
            help = "要绑定的 Arcaea 账号 ID"
        )

        override suspend fun run() {
            if (userID == null || userID!!.length > 9) {
                subject.sendMessage("请正确填写你的 Arcaea 账号 ID! 例如 /arc bind -i 123456789".toMessageWrapper())
                return
            }

            subject.sendMessage(ArcaeaService.bindAccount(user, userID!!))
        }
    }

    class Info(
        val comet: Comet,
        override val subject: PlatformCommandSender,
        override val sender: PlatformCommandSender,
        override val user: CometUser
    ) : CometSubCommand(subject, sender, user, INFO) {

        private val userID by option(
            "-i", "--id",
            help = "要查询的 Arcaea 账号 ID"
        )

        companion object {
            val INFO = SubCommandProperty(
                "info",
                listOf("查询"),
                ARCAEA
            )
        }

        override suspend fun run() {
            subject.sendMessage("🔍 正在获取 Arcaea 信息, 请坐和放宽...".toMessageWrapper())
            if (userID == null) {
                ArcaeaService.queryUserInfo(comet, subject, user)
            } else {
                if (userID!!.length > 9) {
                    subject.sendMessage("请正确填写 Arcaea 账号 ID! 例如 /arc info -i 123456789".toMessageWrapper())
                    return
                }
                ArcaeaService.querySpecificUserInfo(comet, subject, userID!!)
            }
        }
    }
}

