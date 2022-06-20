/*
 * Copyright (c) 2019-2022 StarWishsama.
 *
 * 此源代码的使用受 MIT 许可证约束, 欲阅读此许可证, 可在以下链接查看.
 * Use of this source code is governed by the MIT License which can be found through the following link.
 *
 * https://github.com/StarWishsama/Comet-Bot/blob/dev/LICENSE
 */

package ren.natsuyuk1.comet.cli

import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import moe.sdl.yac.core.CliktCommand
import moe.sdl.yac.core.CommandResult
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jline.reader.EndOfFileException
import org.jline.reader.UserInterruptException
import ren.natsuyuk1.comet.api.Comet
import ren.natsuyuk1.comet.api.command.CommandManager
import ren.natsuyuk1.comet.api.command.ConsoleCommandSender
import ren.natsuyuk1.comet.api.user.CometUser
import ren.natsuyuk1.comet.cli.console.Console
import ren.natsuyuk1.comet.config.branch
import ren.natsuyuk1.comet.config.hash
import ren.natsuyuk1.comet.config.version
import ren.natsuyuk1.comet.utils.coroutine.ModuleScope
import ren.natsuyuk1.comet.utils.jvm.addShutdownHook
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

private val logger = mu.KotlinLogging.logger {}

object CometTerminal {
    private var scope = ModuleScope("CometTerminal")

    val instance = ConcurrentLinkedDeque<Comet>()

    fun init(parentContext: CoroutineContext) {
        scope = ModuleScope("CometTerminal", parentContext)
        CommandManager.init(scope.coroutineContext)
    }
}

class CometTerminalCommand : CliktCommand(name = "comet") {
    override suspend fun run() = scope.launch {
        setupShutdownHook()

        logger.info { "Running Comet Terminal ${version}-${branch}-${hash}" }

        CometTerminal.init(scope.coroutineContext)

        // TODO: Load config

        // TODO: Load database

        setupConsole()
    }.join()

    private suspend fun setupConsole() = scope.launch {
        Console.initReader()
        Console.redirectToJLine()

        while (isActive) {
            try {
                CommandManager.executeCommand(
                    ConsoleCommandSender,
                    CometUser(EntityID(1L, LongIdTable())),
                    Console.readln()
                ).join()
            } catch (e: UserInterruptException) { // Ctrl + C
                println("<Interrupted> use 'quit' command to exit process")
            } catch (e: EndOfFileException) { // Ctrl + D
                exitProcess(0)
            }
        }
    }.join()

    private fun setupShutdownHook() {
        addShutdownHook {
            closeAll()
            println("\nExiting Comet Terminal...")
            Console.redirectToNull()
        }
    }

    companion object {
        private val scope = ModuleScope("CometFrontendScope")

        internal fun closeAll() {
            scope.dispose()
            scope.cancel()
        }
    }
}

suspend fun main(args: Array<String>) {
    when (val result = CometTerminalCommand().main(args)) {
        is CommandResult.Success -> {
            exitProcess(0)
        }

        is CommandResult.Error -> {
            println(result.userMessage)
            exitProcess(1)
        }
    }
}
