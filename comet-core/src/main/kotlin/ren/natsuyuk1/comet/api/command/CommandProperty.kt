/*
 * Copyright (c) 2019-2022 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 * Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 */

package ren.natsuyuk1.comet.api.command

import ren.natsuyuk1.comet.config.DefaultCometConfig

/**
 * [CommandProperty]
 *
 * 一个命令的相关配置，适用于主命令及子命令
 */
data class CommandProperty(
    val name: String,
    val alias: List<String>,
    val userGroup: List<String>,
    val description: String,
    val helpText: String,
    val permission: String = "comet.command.${name}",
    val executeConsumePoint: Int = DefaultCometConfig.defaultCoolDownTime,
    val executeConsumeType: CommandConsumeType = CommandConsumeType.COOLDOWN
)
