/*
 * Copyright (c) 2019-2021 StarWishsama.
 *
 * 此源代码的使用受 GNU General Affero Public License v3.0 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 *  Use of this source code is governed by the GNU AGPLv3 license which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/master/LICENSE
 *
 */

package io.github.starwishsama.comet.service.server

import io.github.starwishsama.comet.CometVariables.netLogger
import io.github.starwishsama.comet.logger.HinaLogLevel
import io.github.starwishsama.comet.service.server.module.defaultModule
import io.github.starwishsama.comet.service.server.module.githubWebHookModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class CometServiceServer(port: Int, customSuffix: String) {
    private val server: ApplicationEngine

    init {
        server = embeddedServer(Netty, environment = applicationEngineEnvironment {
            module {
                install(CallLogging)

                defaultModule()
                githubWebHookModule(customSuffix)
            }

            connector {
                this.port = port
            }
        })

        server.start(false)
        netLogger.log(HinaLogLevel.Info, "服务器启动! 运行在端口 $port", prefix = "WebHook")
    }

    fun stop() {
        server.stop(1000, 1000)
    }
}

