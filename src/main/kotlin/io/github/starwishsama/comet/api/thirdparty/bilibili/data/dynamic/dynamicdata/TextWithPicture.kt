package io.github.starwishsama.comet.api.thirdparty.bilibili.data.dynamic.dynamicdata

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.api.thirdparty.bilibili.data.dynamic.DynamicData
import io.github.starwishsama.comet.api.thirdparty.bilibili.data.user.UserProfile
import io.github.starwishsama.comet.objects.wrapper.MessageWrapper
import io.github.starwishsama.comet.utils.NumberUtil.toLocalDateTime
import java.time.LocalDateTime

data class TextWithPicture(
        val item: Item,
        val user: User,
) : DynamicData {
    data class User(
            val uid: Long,
            @JsonProperty("head_url")
            val headUrl: String,
            @JsonProperty("name")
            val name: String,
            @JsonProperty("vip")
            val vipInfo: UserProfile.VipInfo
    )

    data class Item(
            @JsonProperty("id")
            val id: Long,
            @JsonProperty("title")
            val title: String?,
            @JsonProperty("description")
            val text: String?,
            @JsonProperty("category")
            val category: String,
            @JsonProperty("role")
            val role: JsonNode,
            @JsonProperty("sources")
            val sources: JsonNode,
            @JsonProperty("pictures")
            val pictures: List<Picture>,
            @JsonProperty("pictures_count")
            val pictureCount: Int,
            @JsonProperty("upload_time")
            val uploadTime: Long,
            @JsonProperty("at_control")
            val atControl: String,
            @JsonProperty("reply")
            val replyCount: Long,
            @JsonProperty("settings")
            val settings: JsonNode,
            @JsonProperty("is_fav")
            val isFavorite: Int,
    ) {
        data class Picture(
                @JsonProperty("img_src")
                var imgUrl: String
        )
    }

    override fun getContact(): MessageWrapper {
        val wrapped = MessageWrapper().addText("发布了动态:\n ${item.text ?: "获取失败"}\n" + "🕘 ${BotVariables.hmsPattern.format(item.uploadTime.toLocalDateTime())}\n")

        if (!item.pictures.isNullOrEmpty()) {
            item.pictures.forEach {
                wrapped.addPictureByURL(it.imgUrl)
            }
        }

        return wrapped
    }

    override fun getSentTime(): LocalDateTime = item.uploadTime.toLocalDateTime()
}