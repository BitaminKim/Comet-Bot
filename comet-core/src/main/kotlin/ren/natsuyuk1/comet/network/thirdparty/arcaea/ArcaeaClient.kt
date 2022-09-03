package ren.natsuyuk1.comet.network.thirdparty.arcaea

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import ren.natsuyuk1.comet.consts.json
import ren.natsuyuk1.comet.network.thirdparty.arcaea.data.ArcaeaCommand
import ren.natsuyuk1.comet.network.thirdparty.arcaea.data.ArcaeaUserInfo
import ren.natsuyuk1.comet.network.thirdparty.arcaea.data.Command
import ren.natsuyuk1.comet.utils.brotli4j.BrotliDecompressor

private val logger = KotlinLogging.logger {}

object ArcaeaClient {
    private const val arcaeaAPIHost = "arc.estertion.win"
    private const val arcaeaAPIPort = 616

    private val songInfo = mutableMapOf<String, String>()

    init {
        runBlocking {
            fetchConstants()
        }
    }

    suspend fun fetchConstants() {
        val cmd = "constants"
        if (!BrotliDecompressor.isUsable()) {
            return
        }

        if (songInfo.isNotEmpty()) return

        val client = HttpClient {
            install(WebSockets)
        }

        client.wss(host = arcaeaAPIHost, port = arcaeaAPIPort) {
            send(cmd)

            while (true) {
                when (val msg = incoming.receive()) {
                    is Frame.Text -> {}
                    is Frame.Binary -> {
                        val incomingJson = String(BrotliDecompressor.decompress(msg.readBytes()))
                        val command: Command = json.decodeFromString(incomingJson)

                        logger.debug { "Received command: $command" }
                        logger.debug { "Received json: $incomingJson" }

                        when (command.command) {
                            ArcaeaCommand.SONG_TITLE -> {
                                if (songInfo.isEmpty()) {
                                    val songInfoData = json.parseToJsonElement(incomingJson)

                                    songInfoData.jsonObject["data"]?.jsonObject?.forEach { id, songName ->
                                        songInfo[id] = songName.jsonObject["en"]?.jsonPrimitive?.content!!
                                    }

                                    logger.info { "已更新歌曲信息 (${songInfo.size} 个)" }
                                }

                                client.close()
                                break
                            }

                            else -> { /* ignore */ }
                        }
                    }

                    else -> {
                        client.close()
                        break
                    }
                }
            }

            if (client.isActive) {
                client.close()
            }
        }
    }

    suspend fun queryUserInfo(userID: String): ArcaeaUserInfo? {
        if (!BrotliDecompressor.isUsable()) {
            return null
        }

        val client = HttpClient {
            install(WebSockets)
        }

        var resp: ArcaeaUserInfo? = null

        client.wss(host = arcaeaAPIHost, port = arcaeaAPIPort) {
            send(userID)

            try {
                while (client.isActive) {
                    when (val msg = incoming.receive()) {
                        is Frame.Text -> {
                            val text = msg.readText()

                            if (text == "bye") {
                                client.close()
                            } else {
                                logger.debug { "Arcaea client received: $text" }
                            }
                        }

                        is Frame.Binary -> {
                            val incomingJson = String(BrotliDecompressor.decompress(msg.readBytes()))
                            val command: Command = json.decodeFromString(incomingJson)

                            logger.debug { "Received command: $command" }
                            logger.debug { "Received json: $incomingJson" }

                            when (command.command) {
                                ArcaeaCommand.USER_INFO -> {
                                    resp = json.decodeFromString(incomingJson)
                                    logger.debug { "Receive user info ${resp?.data?.userID} >> $resp" }
                                    client.close()
                                }

                                else -> { /* ignore */ }
                            }
                        }

                        is Frame.Close -> client.close()
                        else -> { /* ignore */ }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                logger.debug { "Arcaea client closed by accident" }
            }
        }

        return resp
    }

    fun getSongNameByID(id: String): String = songInfo[id] ?: id
}
