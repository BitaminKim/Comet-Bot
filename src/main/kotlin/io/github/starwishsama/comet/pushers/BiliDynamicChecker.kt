package io.github.starwishsama.comet.pushers

import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.BotVariables.perGroup
import io.github.starwishsama.comet.api.thirdparty.bilibili.MainApi
import io.github.starwishsama.comet.objects.wrapper.MessageWrapper
import io.github.starwishsama.comet.utils.StringUtil.convertToChain
import io.github.starwishsama.comet.utils.verboseS
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import java.util.concurrent.ScheduledFuture

object BiliDynamicChecker : CometPusher {
    private val pushedList = mutableSetOf<PushDynamicHistory>()
    override val delayTime: Long = 3
    override val internal: Long = 3
    override var future: ScheduledFuture<*>? = null
    override var bot: Bot? = null

    override fun retrieve() {
        var count = 0

        val collectedUsers = mutableSetOf<Long>()

        perGroup.parallelStream().forEach {
            if (it.biliPushEnabled) {
                collectedUsers.plusAssign(it.biliSubscribers)
            }
        }

        collectedUsers.parallelStream().forEach { uid ->
            val data = runBlocking { MainApi.getDynamic(uid) }

            if (data.success) {
                if (pushedList.isEmpty()) {
                    pushedList.plusAssign(PushDynamicHistory(uid, data))
                    count++
                } else {
                    val isExist = pushedList.parallelStream().filter { it.uid == uid }.findFirst().isPresent

                    if (!isExist) {
                        pushedList.plusAssign(PushDynamicHistory(uid, data))
                    } else {
                        val oldData =
                                pushedList.parallelStream().filter { it.uid == uid && data.text == it.pushContent.text }
                                        .findFirst()

                        if (!oldData.isPresent) {
                            pushedList.parallelStream().forEach {
                                if (it.uid == uid) {
                                    it.pushContent = data
                                    it.isPushed = false
                                }
                            }
                        }
                    }
                }
            }
        }

        push()
    }

    override fun push() {
        pushedList.parallelStream().forEach { pdh ->
            perGroup.parallelStream().forEach { cfg ->
                if (cfg.biliPushEnabled) {
                    cfg.biliSubscribers.parallelStream().forEach { uid ->
                        if (pdh.uid == uid) {
                            pdh.target.plusAssign(cfg.id)
                        }
                    }
                }
            }
        }

        val count = pushToGroups()
        if (count > 0) BotVariables.daemonLogger.verboseS("Push bili dynamic success, have pushed $count group(s)!")
    }

    private fun pushToGroups(): Int {
        var count = 0

        /** 遍历推送列表推送开播消息 */
        pushedList.parallelStream().forEach { pdh ->
            if (!pdh.isPushed) {
                pdh.target.forEach { gid ->
                    runBlocking {
                        val group = bot?.getGroup(gid)
                        group?.sendMessage(
                            "${MainApi.getUserNameByMid(pdh.uid)} ".convertToChain() + pdh.pushContent.toMessageChain(
                                group
                            )
                        )
                        count++
                        delay(1_500)
                    }
                }

                pdh.isPushed = true
            }
        }

        return count
    }

    private data class PushDynamicHistory(
        val uid: Long,
        var pushContent: MessageWrapper,
        val target: MutableSet<Long> = mutableSetOf(),
        var isPushed: Boolean = false
    ) {
        fun compare(other: MessageWrapper): Boolean {
            return pushContent.text == other.text
        }
    }
}