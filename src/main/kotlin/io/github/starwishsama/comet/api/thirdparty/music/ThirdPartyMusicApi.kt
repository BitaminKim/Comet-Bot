package io.github.starwishsama.comet.api.thirdparty.music

import com.fasterxml.jackson.module.kotlin.readValue
import io.github.starwishsama.comet.BotVariables.mapper
import io.github.starwishsama.comet.api.thirdparty.music.data.NetEaseSearchResult
import io.github.starwishsama.comet.api.thirdparty.music.data.QQMusicSearchResult
import io.github.starwishsama.comet.api.thirdparty.music.entity.MusicSearchResult
import io.github.starwishsama.comet.utils.network.NetUtil
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * 腾讯音乐, 网易云音乐搜索 API
 */
object ThirdPartyMusicApi {
    /** 腾讯, 调用限额1分钟100次，10分钟500次，1小时2000次 */
    private const val jsososo = "https://api.qq.jsososo.com/song/urls?id="

    fun searchNetEaseMusic(name: String, length: Int = 1): List<MusicSearchResult> {
        val resp = NetUtil.executeHttpRequest("https://music.163.com/api/search/pc?offset=0&total=true&limit=${
            1.coerceAtMost(
                length
            )
        }&type=1&s=${name}", call = {
            header("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0")
            post(object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/x-www-form-urlencoded".toMediaTypeOrNull()
                }

                override fun writeTo(sink: BufferedSink) {
                    return
                }

            })
        })

        val page = resp.body?.string()

        val searchResult: NetEaseSearchResult = mapper.readValue(page ?: return emptyList())

        if (searchResult.code != 200) {
            return emptyList()
        }

        val songResults = mutableListOf<MusicSearchResult>()

        searchResult.result.songs.forEach { song: NetEaseSearchResult.Song ->

            songResults.add(
                MusicSearchResult(
                    song.name,
                    song.buildArtistsName(),
                    "https://music.163.com/#/song?id=${song.id}",
                    song.album.picUrl,
                    "http://music.163.com/song/media/outer/url?id=${song.id}&userid=1"
                )
            )
        }

        return songResults
    }

    fun searchQQMusic(name: String, length: Int = 1): List<MusicSearchResult> {
        val searchResult = getQQMusicSearchResult(name) ?: return emptyList()

        val result = mutableListOf<MusicSearchResult>()

        val songs = searchResult.data.songs.songList

        songs.subList(0, songs.size.coerceAtMost(length)).forEach { song ->
            val artists = mutableListOf<String>().also { l ->
                song.singer.forEach {
                    l.add(it.name)
                }
            }

            val playResult = NetUtil.getPageContent("$jsososo${song.songMid}")

            println(playResult)

            val playURL: String

            if (playResult?.isNotBlank() == true) {
                val musicUrlObject = mapper.readTree(playResult)

                playURL = if (musicUrlObject.isNull) {
                    ""
                } else {
                    val urlObject = musicUrlObject["data"]
                    if (!urlObject.isNull) {
                        urlObject[song.songMid].asText()
                    } else {
                        ""
                    }
                }
            } else {
                return@forEach
            }

            val jumpUrl = "https://y.qq.com/n/yqq/song/${song.songMid}.html?no_redirect=1"

            result.add(
                MusicSearchResult(
                    song.songName,
                    artists,
                    jumpUrl,
                    getQQMusicCover(jumpUrl),
                    playURL
                )
            )
        }

        println(result)

        return result
    }

    fun getQQMusicSearchResult(name: String): QQMusicSearchResult? {
        val songResult =
            NetUtil.getPageContent(
                "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=${
                    URLEncoder.encode(
                        name,
                        "UTF-8"
                    )
                }&format=json&inCharset=utf8&outCharset=utf-8"
            )
        return mapper.readValue(songResult ?: return null)
    }

    private fun getQQMusicCover(songURL: String): String {
        val req = Jsoup.connect(songURL)
        val resp = req.execute()

        return "http:" + resp.parse().getElementsByClass("data__cover").select("img")[0].attributes()["src"]
    }
}