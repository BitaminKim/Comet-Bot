package ren.natsuyuk1.comet.objects.pjsk.local

import kotlinx.serialization.builtins.ListSerializer
import mu.KotlinLogging
import ren.natsuyuk1.comet.consts.cometClient
import ren.natsuyuk1.comet.consts.json
import ren.natsuyuk1.comet.network.thirdparty.projectsekai.objects.PJSKMusicDifficultyInfo
import ren.natsuyuk1.comet.util.pjsk.pjskFolder
import ren.natsuyuk1.comet.utils.file.isBlank
import ren.natsuyuk1.comet.utils.file.readTextBuffered
import ren.natsuyuk1.comet.utils.file.touch
import ren.natsuyuk1.comet.utils.ktor.DownloadStatus
import ren.natsuyuk1.comet.utils.ktor.downloadFile
import kotlin.time.Duration.Companion.days

private val logger = KotlinLogging.logger {}

object ProjectSekaiMusicDifficulty : ProjectSekaiLocalFile(
    pjskFolder.resolve("musicDifficulties.json"),
    1.days
) {
    internal val musicDiffDatabase = mutableListOf<PJSKMusicDifficultyInfo>()

    override suspend fun load() {
        try {
            musicDiffDatabase.addAll(
                json.decodeFromString(
                    ListSerializer(PJSKMusicDifficultyInfo.serializer()),
                    file.readTextBuffered()
                )
            )
        } catch (e: Exception) {
            logger.warn(e) { "解析 Project Sekai 音乐等级偏差值数据时出现问题" }
        }
    }

    override suspend fun update(): Boolean {
        file.touch()

        if (file.isBlank() || isOutdated()) {

            if (cometClient.client.downloadFile(
                    "https://gitlab.com/pjsekai/database/musics/-/raw/main/musicDifficulties.json",
                    file
                ) == DownloadStatus.OK
            ) {
                updateLastUpdateTime()
                logger.info { "成功更新音乐等级偏差值数据" }

                return true
            }
        }

        return false
    }

    fun getMusicDifficulty(musicId: Int) = musicDiffDatabase.filter { it.musicId == musicId }
}
