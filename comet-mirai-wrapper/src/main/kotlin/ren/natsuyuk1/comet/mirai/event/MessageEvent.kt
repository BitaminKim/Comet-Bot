package ren.natsuyuk1.comet.mirai.event

import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import ren.natsuyuk1.comet.api.event.events.message.PrivateMessageEvent
import ren.natsuyuk1.comet.mirai.MiraiComet
import ren.natsuyuk1.comet.mirai.contact.toCometGroup
import ren.natsuyuk1.comet.mirai.contact.toCometUser
import ren.natsuyuk1.comet.mirai.contact.toGroupMember
import ren.natsuyuk1.comet.mirai.util.toMessageWrapper

fun GroupMessageEvent.toCometEvent(comet: MiraiComet): ren.natsuyuk1.comet.api.event.events.message.GroupMessageEvent {
    return ren.natsuyuk1.comet.api.event.events.message.GroupMessageEvent(
        comet = comet,
        subject = this.subject.toCometGroup(comet),
        sender = this.sender.toGroupMember(comet),
        senderName = this.senderName,
        message = this.message.toMessageWrapper(),
        time = this.time.toLong(),
        messageID = source.ids.first().toLong()
    )
}

fun FriendMessageEvent.toCometEvent(comet: MiraiComet): PrivateMessageEvent {
    return PrivateMessageEvent(
        comet,
        this.subject.toCometUser(comet),
        this.sender.toCometUser(comet),
        this.senderName,
        this.message.toMessageWrapper(),
        this.time.toLong(),
        messageID = source.ids.first().toLong()
    )
}

fun GroupTempMessageEvent.toCometEvent(comet: MiraiComet): PrivateMessageEvent {
    return PrivateMessageEvent(
        comet,
        this.subject.toCometUser(comet),
        this.sender.toCometUser(comet),
        this.senderName,
        this.message.toMessageWrapper(),
        this.time.toLong(),
        messageID = source.ids.first().toLong()
    )
}
