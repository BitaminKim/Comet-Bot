package ren.natsuyuk1.comet.api.session

import ren.natsuyuk1.comet.api.task.TaskManager
import ren.natsuyuk1.comet.api.user.CometUser
import ren.natsuyuk1.comet.api.user.Contact
import ren.natsuyuk1.comet.api.user.Group
import ren.natsuyuk1.comet.utils.coroutine.ModuleScope
import ren.natsuyuk1.comet.utils.message.MessageWrapper
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

fun Session.register() = SessionManager.registerSession(this)

fun Session.registerTimeout(timeout: Duration) = SessionManager.registerTimeoutSession(this, timeout)

fun Session.expire() = SessionManager.expireSession(this)

object SessionManager {
    private var scope = ModuleScope("comet-session-manager")

    private val sessions = ConcurrentLinkedQueue<Session>()

    fun init(parentContext: CoroutineContext) {
        scope = ModuleScope(scope.name(), parentContext)
    }

    /**
     * 注册一个会话 [Session] 以备监听
     */
    fun registerSession(session: Session) = sessions.add(session)

    /**
     * 注册一个 [Session] 以备监听
     * 并且这个会话会在指定时间后自动注销
     *
     * @param session 欲注册的会话
     * @param timeout 自动注销的期限
     */
    fun registerTimeoutSession(session: Session, timeout: Duration) {
        registerSession(session)

        TaskManager.registerTask(timeout) {
            expireSession(session)
        }
    }

    /**
     * 注销一个会话 [Session]
     */
    fun expireSession(session: Session) = sessions.remove(session)

    /**
     * 处理会话
     *
     * @param subject 可能触发的联系人 [Contact]
     * @param message 触发时发送的消息
     */
    fun handleSession(subject: Contact, message: MessageWrapper): Boolean {
        val user: CometUser? = if (subject !is Group) {
            CometUser.getUserOrCreate(subject.id, subject.platformName)
        } else null

        val targetSession = sessions.filter {
            it.contact.id == subject.id || it.cometUser?.id == user?.id
        }

        targetSession.forEach { it.handle(message) }

        return targetSession.isNotEmpty()
    }
}