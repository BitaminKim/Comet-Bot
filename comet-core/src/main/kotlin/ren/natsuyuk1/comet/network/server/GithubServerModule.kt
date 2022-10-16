package ren.natsuyuk1.comet.network.server

import cn.hutool.core.net.URLDecoder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ren.natsuyuk1.comet.api.event.broadcast
import ren.natsuyuk1.comet.event.pusher.github.GithubEvent
import ren.natsuyuk1.comet.objects.github.data.GithubRepoData
import ren.natsuyuk1.comet.objects.github.data.SecretStatus
import ren.natsuyuk1.comet.service.GitHubService
import ren.natsuyuk1.comet.utils.error.ErrorHelper
import ren.natsuyuk1.comet.utils.ktor.asReadable
import java.io.IOException

private val logger = mu.KotlinLogging.logger {}

fun Application.githubWebHookModule() {
    routing {
        handleWebHook()
    }
}

internal fun Routing.handleWebHook() {
    post(path = "/github") {
        GithubWebHookHandler.handle(call)
    }
}

/**
 * [GithubWebHookHandler]
 *
 * 处理 Github Webhook 请求.
 */
object GithubWebHookHandler {
    private const val signature256 = "X-Hub-Signature-256"
    private const val eventTypeHeader = "X-GitHub-Event"

    suspend fun handle(call: ApplicationCall) {
        logger.debug { "有新连接 ${call.request.httpMethod} - ${call.request.uri}" }
        logger.debug {
            "Request Headers: ${call.request.headers.asReadable()}"
        }

        try {
            // Get information from header to identity whether the request is from GitHub.
            if (!isGitHubRequest(call.request)) {
                logger.debug { "Github Webhook 传入无效请求" }
                call.respond(HttpStatusCode.Forbidden, "Unsupported Request")
                return
            }

            val signature = call.request.header(signature256)
            val eventType = call.request.header(eventTypeHeader) ?: ""
            val request = call.receiveText()
            val secretStatus = GitHubService.checkSecret(signature, request, eventType)

            logger.debug { "GitHub WebHook 收到新事件, secretStatus = $secretStatus" }

            if (!checkSecretStatus(call, secretStatus, signature)) {
                call.respond(HttpStatusCode.Forbidden, "Validate Failed")
                logger.debug("Secret 校验失败")
                return
            }

            val payload = URLDecoder.decode(request.replace("payload=", ""), Charsets.UTF_8)

            logger.debug("接收到传入请求: $payload")

            var hasError = false

            try {
                val event = GitHubService.processEvent(payload, eventType)

                if (event != null) {
                    if (event.isSendableEvent()) {
                        GithubRepoData.find(event.repoName())?.let {
                            GithubEvent(it, event).broadcast()
                        }
                    }
                } else {
                    logger.debug("推送 WebHook 消息失败, 不支持的事件类型")

                    call.respondText(
                        "Comet 已收到事件, 但所请求的事件类型不支持 ($eventType)",
                        status = HttpStatusCode.InternalServerError
                    )

                    return
                }
            } catch (e: IOException) {
                ErrorHelper.createErrorReportFile("推送 WebHook 消息失败", "GitHub WebHook", e, payload)
                hasError = true
            }

            when {
                hasError -> {
                    call.respondText("Comet 发生内部错误", status = HttpStatusCode.InternalServerError)
                }

                secretStatus == SecretStatus.NO_SECRET -> {
                    call.respondText("Comet 已收到事件, 推荐使用密钥加密以保证安全")
                }

                else -> {
                    call.respondText("Comet 已收到事件")
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "推送 WebHook 消息失败" }
            call.respondText("Comet 发生内部错误", status = HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun checkSecretStatus(
        call: ApplicationCall,
        secretStatus: SecretStatus,
        signature: String?
    ): Boolean {
        if (secretStatus == SecretStatus.HAS_SECRET && signature != null) {
            return true
        }

        if (signature == null && secretStatus == SecretStatus.NO_SECRET) {
            call.respondText(status = HttpStatusCode.NotFound, text = "Not Found")
            return true
        }

        if (secretStatus == SecretStatus.FAILED) {
            logger.debug("获取 Secret 失败")
            call.respondText(status = HttpStatusCode.InternalServerError, text = "Internal Server Error")
            return false
        }

        if (secretStatus == SecretStatus.UNAUTHORIZED) {
            logger.debug { "收到新事件, 未通过安全验证. 请求的签名为: ${signature?.firstOrNull() ?: "无"}" }
            call.respondText(status = HttpStatusCode.Forbidden, text = "The request isn't verified")
            return false
        }

        return false
    }

    /**
     * [isGitHubRequest]
     *
     * 通过多种方式检测来源是否来自于 GitHub
     *
     * @return 是否为 GitHub 的请求
     */
    private fun isGitHubRequest(req: ApplicationRequest): Boolean {
        return req.httpMethod == HttpMethod.Post && req.header(eventTypeHeader) != null
    }
}
