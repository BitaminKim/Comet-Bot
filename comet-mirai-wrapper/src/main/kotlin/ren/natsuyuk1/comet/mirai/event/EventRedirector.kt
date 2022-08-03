package ren.natsuyuk1.comet.mirai.event

import mu.KotlinLogging
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import ren.natsuyuk1.comet.api.event.EventManager
import ren.natsuyuk1.comet.mirai.MiraiComet

private val logger = KotlinLogging.logger {}

suspend fun Event.redirectToComet(comet: MiraiComet) {
    when (this) {
        is GroupMessageEvent -> {
            logger.debug { "Redirecting mirai event $this" }
            EventManager.broadcastEvent(this.toCometEvent(comet))
        }

        is FriendMessageEvent -> {
            EventManager.broadcastEvent(this.toCometEvent(comet))
        }
        is GroupTempMessageEvent -> {
            EventManager.broadcastEvent(this.toCometEvent(comet))
        }
    }
}
