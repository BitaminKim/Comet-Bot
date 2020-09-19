package io.github.starwishsama.comet.objects.pojo.twitter

import cn.hutool.http.HttpException
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.BotVariables.gson
import io.github.starwishsama.comet.api.twitter.TwitterApi
import io.github.starwishsama.comet.objects.pojo.twitter.tweetEntity.Media
import io.github.starwishsama.comet.utils.NumberUtil.getBetterNumber
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.ExperimentalTime

data class Tweet(
    @SerializedName("created_at")
    val postTime: String,
    val id: Long,
    @SerializedName("id_str")
    val idAsString: String,
    @SerializedName("full_text")
    val text: String,
    val truncated: Boolean,
    val entities: JsonObject?,
    val source: String,
    @SerializedName("in_reply_to_status_id")
    val replyTweetId: Long?,
    val user: TwitterUser,
    @SerializedName("retweeted_status")
    val retweetStatus: Tweet?,
    @SerializedName("retweet_count")
    val retweetCount: Long?,
    @SerializedName("favorite_count")
    val likeCount: Long?,
    @SerializedName("possibly_sensitive")
    val sensitive: Boolean?,
    @SerializedName("quoted_status")
    val quotedStatus: Tweet?,
    @SerializedName("is_quote_status")
    val isQuoted: Boolean
) {
    /**
     * 格式化输出推文
     */
    @ExperimentalTime
    fun convertToString(): String {
        val duration =
                Duration.between(getSentTime(), LocalDateTime.now())
        val extraText =
                "\n❤${likeCount?.getBetterNumber()} | \uD83D\uDD01${retweetCount} | 🕘${DateTimeFormatter.ofPattern("HH:mm:ss").format(getSentTime())}"

        if (retweetStatus != null) {
            return "转发了 ${retweetStatus.user.name} 的推文\n${retweetStatus.text}" + extraText
        }

        if (isQuoted && quotedStatus != null) {
            return "对于 ${quotedStatus.user.name} 的推文\n${quotedStatus.text}\n\n${user.name} 进行了评论\n$text" + extraText
        }

        if (replyTweetId != null) {
            val repliedTweet = try {
                TwitterApi.getTweetById(replyTweetId)
            } catch (t: Throwable) {
                return text + extraText
            }
            return "对于 ${repliedTweet.user.name} 的推文\n${repliedTweet.text}\n\n${user.name} 进行了回复\n$text" + extraText
        }

        var result = text + extraText

        val tcoUrl = mutableListOf<String>()

        BotVariables.tcoPattern.matcher(result).run {
            while (find()) {
                tcoUrl.add(group())
            }
        }

        val tweetUrl = tcoUrl.last()

        result = result.replace(tweetUrl, "")
        result = "$result\n\uD83D\uDD17 > $tweetUrl\n这条推文是 $duration 前发送的"

        return result
    }

    /**
     * 判断两个推文是否内容相同
     */
    fun contentEquals(tweet: Tweet?): Boolean {
        if (tweet == null) return false
        return text == tweet.text || getSentTime().isEqual(tweet.getSentTime())
    }

    /**
     * 获取该推文发送的时间
     */
    fun getSentTime(): LocalDateTime {
        val twitterTimeFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH)
        return twitterTimeFormat.parse(postTime).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    /**
     * 获取推文中的第一张图片
     */
    fun getPictureUrl(): String? {
        val jsonEntities = entities

        /**
         * 从此推文中获取图片链接
         */
        if (jsonEntities != null) {
            val media = jsonEntities["media"]
            if (media != null) {
                try {
                    val image =
                            gson.fromJson(media.asJsonArray[0].asJsonObject.toString(), Media::class.java)
                    if (image.isSendableMedia()) {
                        return image.getImageUrl()
                    }
                } catch (e: JsonSyntaxException) {
                    BotVariables.logger.warning("在获取推文下的图片链接时发生了问题", e)
                } catch (e: HttpException) {
                    BotVariables.logger.warning("在获取推文下的图片链接时发生了问题", e)
                }
            }
        }

        /**
         * 如果推文中没有图片, 则尝试获取转发的推文中的图片
         */
        if (retweetStatus != null) {
            return retweetStatus.getPictureUrl()
        }

        /**
         * 如果推文中没有图片, 则尝试获取引用回复推文中的图片
         */
        if (quotedStatus != null) {
            return quotedStatus.getPictureUrl()
        }

        return null
    }
}