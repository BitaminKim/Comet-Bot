package ren.natsuyuk1.comet.mirai

import ren.natsuyuk1.comet.api.message.MessageSource

/**
 * [MiraiMessageSource]
 *
 * **Mirai 侧独占** 消息元素.
 */
class MiraiMessageSource(
    type: MessageSourceType,
    val botID: Long,
    val ids: IntArray,
    val internalIds: IntArray,
    time: Int,
    from: Long,
    target: Long,
): MessageSource(type, from, target, time.toLong(), (ids.firstOrNull() ?: -1).toLong())


