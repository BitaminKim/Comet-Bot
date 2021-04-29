package io.github.starwishsama.comet.objects.gacha.pool

import cn.hutool.core.util.RandomUtil
import io.github.starwishsama.comet.BotVariables
import io.github.starwishsama.comet.logger.HinaLogLevel
import io.github.starwishsama.comet.objects.BotUser
import io.github.starwishsama.comet.objects.gacha.items.ArkNightOperator
import io.github.starwishsama.comet.objects.gacha.items.GachaItem
import io.github.starwishsama.comet.utils.GachaUtil
import java.math.RoundingMode
import java.util.stream.Collectors

/**
 * [ArkNightPool]
 *
 * 明日方舟抽卡实现方法
 *
 * 干员信息来源: https://github.com/Kengxxiao/ArknightsGameData/
 *
 * 干员图片来源: http://prts.wiki/
 *
 * 感谢以上大佬提供的资源!
 *
 * @author StarWishsama
 */

class ArkNightPool(
    override val name: String = "标准寻访",
    override val description: String = "适合多种场合的强力干员",
    val condition: ArkNightOperator.() -> Boolean = {
        GachaUtil.hasOperator(this.name) && obtain?.contains("招募寻访") == true
    }
) : GachaPool() {
    override val tenjouCount: Int = -1
    override val tenjouRare: Int = -1
    override val poolItems: MutableList<ArkNightOperator> = mutableListOf()

    init {
        BotVariables.arkNight.stream().filter { condition(it) }.forEach {
            poolItems.add(it)
        }

        BotVariables.daemonLogger.log(
            HinaLogLevel.Info,
            message = "已加载了 ${poolItems.size} 个干员至卡池 $name",
            prefix = "明日方舟"
        )
    }

    private val r6Range = 0.0..0.02
    private val r5Range = 0.03..0.11
    private val r4Range = 0.12..0.52

    override fun doDraw(time: Int): List<ArkNightOperator> {
        val result = mutableListOf<ArkNightOperator>()
        var r6UpRate = 0.00

        repeat(time) {
            // 在五十连之后的保底
            if (it >= 50 && r6UpRate == 0.0) {
                r6UpRate += 0.02
            }

            // 按默认抽卡规则抽出对应星级干员, 超过五十连后保底机制启用
            val rare: Int = when (RandomUtil.randomDouble(2, RoundingMode.HALF_DOWN)) {
                in r6Range.start..r6Range.endInclusive + r6UpRate -> 6 // 2%
                in r5Range -> 5 // 8%
                in r4Range -> 4 // 40%
                else -> 3 // 50%
            }

            when {
                // 如果在保底的基础上抽到了六星, 则重置加倍概率
                r6UpRate > 0 -> {
                    if (rare == 6) {
                        r6UpRate = 0.0
                    } else {
                        r6UpRate += 0.02
                    }
                }
                // 究极非酋之后必爆一个六星
                rare != 6 && r6UpRate >= 1 -> {
                    result.add(getArkNightOperator(6))
                    r6UpRate = 0.0
                    return@repeat
                }
            }

            result.add(getArkNightOperator(rare))
        }
        return result
    }

    override fun getGachaItem(rare: Int): GachaItem {
        return getArkNightOperator(rare)
    }

    // 使用自定义抽卡方式
    private fun getArkNightOperator(rare: Int): ArkNightOperator {
        // 首先获取所有对应星级干员
        val rareItems = poolItems.parallelStream().filter { it.rare == (rare - 1) }.collect(Collectors.toList())

        require(rareItems.isNotEmpty()) { "获取干员列表失败: 空列表. 等级为 ${rare - 1}, 池内干员数 ${poolItems.size}" }

        val weight: Int

        // 然后检查是否到了出 UP 角色的时候
        if (highProbabilityItems.isNotEmpty()) {
            val highItems =
                highProbabilityItems.keys.parallelStream().collect(Collectors.toList()) as MutableList<ArkNightOperator>
            rareItems.addAll(highItems)

            rareItems.shuffle()

            weight = RandomUtil.randomInt(0, rareItems.size - 1)

            val target = rareItems[weight]

            for ((gi, prop) in highProbabilityItems) {
                val recalculatedProp = (weight * prop).toInt()
                val redrawResult = rareItems[recalculatedProp]
                if (redrawResult.name == gi.name) {
                    return redrawResult
                } else {
                    rareItems.shuffle()
                }
            }

            return target
        } else {
            weight = RandomUtil.randomInt(0, rareItems.size - 1)
            rareItems.shuffle()
            return rareItems[weight]
        }
    }

    /**
     * 明日方舟抽卡结果
     */
    fun getArkDrawResult(user: BotUser, time: Int = 1): List<ArkNightOperator> {
        return if (GachaUtil.checkHasGachaTime(user, time)) {
            user.decreaseTime(time)
            doDraw(time)
        } else {
            emptyList()
        }
    }

    /**
     * 明日方舟抽卡，返回文字
     */
    fun getArkDrawResultAsString(user: BotUser, drawResult: List<ArkNightOperator>): String {
        val currentPool = "目前卡池为: $name\n"
        if (drawResult.isNotEmpty()) {
            when (drawResult.size) {
                1 -> {
                    val (name, _, rare) = drawResult[0]
                    return currentPool + "单次寻访结果\n$name ${GachaUtil.getStar(rare + 1)}"
                }
                10 -> {
                    return StringBuilder(currentPool + "十连寻访结果:\n").apply {
                        for ((name, _, rare) in drawResult) {
                            append(name).append(" ").append(GachaUtil.getStar(rare + 1)).append(" ")
                        }
                    }.trim().toString()
                }
                else -> {
                    val r6Count = drawResult.parallelStream().filter { it.rare + 1 == 6 }.count()
                    val r5Count = drawResult.parallelStream().filter { it.rare + 1 == 4 }.count()
                    val r4Count = drawResult.parallelStream().filter { it.rare + 1 == 3 }.count()
                    val r3Count = drawResult.size - r6Count - r5Count - r4Count

                    return currentPool + "寻访结果:\n" +
                            "寻访次数: ${drawResult.size}\n" +
                            "结果: ${r6Count}[6]|${r5Count}[5]|${r4Count}[4]|${r3Count}[3]\n" +
                            "使用合成玉 ${drawResult.size * 600}"
                }
            }
        } else {
            return GachaUtil.overTimeMessage + "\n剩余次数: ${user.commandTime}"
        }
    }

    /**
     * 明日方舟抽卡，返回文字
     */
    fun getArkDrawResultAsString(user: BotUser, time: Int): String =
        getArkDrawResultAsString(user, getArkDrawResult(user, time))
}