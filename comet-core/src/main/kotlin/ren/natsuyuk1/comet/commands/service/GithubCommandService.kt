package ren.natsuyuk1.comet.commands.service

import kotlinx.coroutines.delay
import ren.natsuyuk1.comet.api.Comet
import ren.natsuyuk1.comet.api.command.PlatformCommandSender
import ren.natsuyuk1.comet.api.session.Session
import ren.natsuyuk1.comet.api.session.expire
import ren.natsuyuk1.comet.api.session.registerTimeout
import ren.natsuyuk1.comet.api.user.CometUser
import ren.natsuyuk1.comet.api.user.Group
import ren.natsuyuk1.comet.network.thirdparty.github.GitHubApi
import ren.natsuyuk1.comet.objects.github.data.GithubRepoData
import ren.natsuyuk1.comet.utils.message.Image
import ren.natsuyuk1.comet.utils.message.MessageWrapper
import ren.natsuyuk1.comet.utils.message.buildMessageWrapper
import ren.natsuyuk1.comet.utils.string.StringUtil.toMessageWrapper
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object GithubCommandService {
    private val repoRegex = "(\\w*)/(\\w*)".toRegex()

    class GitHubSubscribeSession(
        contact: PlatformCommandSender,
        val sender: PlatformCommandSender,
        user: CometUser,
        val owner: String,
        val name: String,
        private val groupID: Long,
    ) : Session(contact, user) {
        override fun handle(message: MessageWrapper) {
            val raw = message.parseToString()
            val secret = if (raw == "完成订阅") {
                ""
            } else {
                raw
            }

            GithubRepoData.data.repos.add(
                GithubRepoData.Data.GithubRepo(
                    name,
                    owner,
                    secret,
                    mutableListOf(GithubRepoData.Data.GithubRepo.GithubRepoSubscriber(groupID))
                )
            )

            sender.sendMessage("订阅仓库 $owner/$name 成功, 请至仓库 WebHook 设置添加 Comet 管理提供的链接!".toMessageWrapper())

            expire()
        }
    }

    suspend fun processSubscribe(
        subject: PlatformCommandSender,
        sender: PlatformCommandSender,
        user: CometUser,
        groupID: Long,
        repoName: String
    ) {
        if (repoName.matches(repoRegex)) {
            val slice = repoName.split("/")
            val owner = slice[0]
            val name = slice[1]

            val repos = GithubRepoData.data.repos

            if (repos.any { it.getName() == repoName && it.subscribers.any { sub -> sub.id == groupID } }) {
                subject.sendMessage("你已经订阅过这个仓库了!".toMessageWrapper())
                return
            }

            if (GitHubApi.isRepoExist(owner, name)) {
                val repo = GithubRepoData.data.repos.find { it.getName() == "$owner/$name" }

                if (repo == null) {
                    if (subject is Group) {
                        subject.sendMessage("请在私聊中继续完成你的订阅".toMessageWrapper())
                    }

                    delay(1.seconds)

                    sender.sendMessage(
                        ("你正在订阅仓库 $owner/$name, 是否需要添加仓库机密 (Secret)?\n" +
                            "添加机密后, 能够使接收仓库信息更加安全, 但千万别忘记了!\n" +
                            "如果无需添加, 请回复「完成订阅」, 反之直接发送你欲设置的机密.").toMessageWrapper()
                    )

                    GitHubSubscribeSession(subject, sender, user, owner, name, groupID).registerTimeout(1.minutes)
                } else {
                    repo.subscribers.add(GithubRepoData.Data.GithubRepo.GithubRepoSubscriber(groupID))
                    subject.sendMessage("订阅仓库 $owner/$name 成功, 请至仓库 WebHook 设置添加 Comet 管理提供的链接!".toMessageWrapper())
                }
            } else {
                subject.sendMessage("找不到你想要订阅的 GitHub 仓库".toMessageWrapper())
            }
        } else {
            subject.sendMessage("请输入有效的 GitHub 仓库名称, 例如 StarWishsama/Comet-Bot".toMessageWrapper())
        }
    }

    fun processUnsubscribe(
        subject: PlatformCommandSender,
        groupID: Long,
        repoName: String
    ) {
        if (repoName.matches(repoRegex)) {
            val slice = repoName.split("/")
            val owner = slice[0]
            val name = slice[1]

            val repos = GithubRepoData.data.repos
            val repo = repos.find { it.getName() == "$owner/$name" }

            if (repo != null) {
                repo.subscribers.removeIf { it.id == groupID }
                subject.sendMessage("成功退订 $repoName!".toMessageWrapper())

                if (repo.subscribers.isEmpty()) {
                    repos.remove(repo)
                }
            } else {
                subject.sendMessage("找不到你想要取消订阅的 GitHub 仓库".toMessageWrapper())
            }
        } else {
            subject.sendMessage("请输入有效的 GitHub 仓库名称, 例如 StarWishsama/Comet-Bot".toMessageWrapper())
        }
    }

    // 3, 4
    private val githubLinkRegex by lazy { Regex("""^(https?://)?(www\.)?github\.com/(\w+)/(\w+)$""") }

    suspend fun fetchRepoInfo(comet: Comet, subject: PlatformCommandSender, repoName: String) {
        var owner: String? = null
        var name: String? = null

        if (repoRegex.matches(repoName)) {
            val split = repoName.split("/")

            owner = split[0]
            name = split[1]
        } else if (githubLinkRegex.matches(repoName)) {
            val groupVar = githubLinkRegex.find(repoName)?.groupValues

            if (groupVar.isNullOrEmpty() || groupVar.size < 4) {
                subject.sendMessage("请输入有效的仓库名/链接!".toMessageWrapper())
                return
            }

            owner = groupVar[3]
            name = groupVar[4]
        }


        if (owner == null || name == null) {
            subject.sendMessage("请输入有效的仓库名/链接!".toMessageWrapper())
            return
        }

        val image = GitHubApi.getRepoPreviewImage(comet, owner, name)

        if (image == null) {
            subject.sendMessage("搜索不到这个仓库, 等会再试试吧~".toMessageWrapper())
        } else {
            subject.sendMessage(
                buildMessageWrapper {
                    appendElement(Image(url = image))
                    appendText("🔗 https://github.com/$owner/$name")
                }
            )
        }
    }

    class ModifyRepoSettingSession(
        contact: PlatformCommandSender,
        cometUser: CometUser?,
    ) : Session(contact, cometUser) {
        override fun handle(message: MessageWrapper) {
        }
    }

    suspend fun modifyRepoSetting(
        subject: PlatformCommandSender,
        user: CometUser,
        groupID: Long,
        repoName: String
    ) {
        if (!repoRegex.matches(repoName)) {
            subject.sendMessage("请输入有效的仓库名!".toMessageWrapper())
            return
        }

        if (!GithubRepoData.exists(repoName, groupID)) {
            subject.sendMessage("对应群没有订阅过这个仓库".toMessageWrapper())
            return
        }


    }
}
