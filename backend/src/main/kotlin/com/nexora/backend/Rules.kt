package com.nexora.backend

import kotlin.math.pow
import kotlin.math.roundToLong

object ReputationRules {
    private const val BASE_LIMIT_REAIS = 100.0
    private const val MAX_BUFF_BPS = 10_000
    const val MIN_HELP_LEVEL = 2
    const val MIN_HELP_XP = 100L
    const val ADMIN_FEE_BLOCK_LIMIT_CENTS = 500L

    fun xpRequiredForLevel(level: Int): Long {
        require(level >= 1)
        return (100.0 * 1.105.pow((level - 1).toDouble())).roundToLong()
    }

    fun totalXpRequiredToEnterLevel(level: Int): Long {
        if (level <= 1) return 0
        var total = 0L
        for (current in 1 until level) total += xpRequiredForLevel(current)
        return total
    }

    fun levelForXp(totalXp: Long): Int {
        var level = 1
        var remaining = totalXp
        while (remaining >= xpRequiredForLevel(level) && level < 1000) {
            remaining -= xpRequiredForLevel(level)
            level += 1
        }
        return level
    }

    fun xpIntoLevel(totalXp: Long): Long = totalXp - totalXpRequiredToEnterLevel(levelForXp(totalXp))

    fun supportLimitCents(level: Int): Long {
        if (level < MIN_HELP_LEVEL) return 0
        if (level == MIN_HELP_LEVEL) return 10_000
        var amount = BASE_LIMIT_REAIS
        for (targetLevel in 3..level) {
            amount *= when (targetLevel) {
                3 -> 1.5
                4, 5 -> 1.4
                6, 7 -> 1.3
                8, 9 -> 1.2
                else -> 1.1
            }
        }
        return (amount.roundToLong() * 100).coerceAtLeast(10_000)
    }

    fun adminFeeFor(amountCents: Long): Long = (amountCents / 100).coerceAtLeast(0)

    fun adminFeeLimitCents(level: Int): Long = ADMIN_FEE_BLOCK_LIMIT_CENTS

    fun canRequestHelp(user: UserRecord): Boolean =
        user.level >= MIN_HELP_LEVEL && user.xp >= MIN_HELP_XP

    fun xpForCompletedReturn(amountCents: Long, buffBps: Int): Long {
        val baseXp = (amountCents / 100L).coerceAtLeast(1)
        return (baseXp * (10_000L + buffBps) / 10_000L).coerceAtLeast(1)
    }

    fun recalculateBuffBps(onTimeReturnedCents: Long, earlyReturnedCents: Long, guestsAtLevelFive: Int): Int {
        val onTimeBps = (onTimeReturnedCents / 100_000L * 10L).toInt()
        val earlyBps = (earlyReturnedCents / 100_000L * 30L).toInt()
        val guestBps = guestsAtLevelFive * 10
        return (onTimeBps + earlyBps + guestBps).coerceAtMost(MAX_BUFF_BPS)
    }
}

data class LevelRequirement(val level: Int, val users: Int)
data class RoadmapStep(val step: Int, val capacity: Int, val requirements: List<LevelRequirement>)

object RoadmapRules {
    val steps = listOf(
        RoadmapStep(1, 20, emptyList()),
        RoadmapStep(2, 50, listOf(LevelRequirement(2, 5))),
        RoadmapStep(3, 100, listOf(LevelRequirement(3, 5), LevelRequirement(2, 10))),
        RoadmapStep(4, 200, listOf(LevelRequirement(4, 5), LevelRequirement(3, 10), LevelRequirement(2, 20))),
        RoadmapStep(5, 350, listOf(LevelRequirement(5, 5), LevelRequirement(4, 10), LevelRequirement(3, 20))),
        RoadmapStep(6, 500, listOf(LevelRequirement(6, 5), LevelRequirement(5, 10), LevelRequirement(4, 20))),
        RoadmapStep(7, 750, listOf(LevelRequirement(7, 5), LevelRequirement(6, 10), LevelRequirement(5, 20))),
        RoadmapStep(8, 1_000, listOf(LevelRequirement(8, 5), LevelRequirement(7, 10), LevelRequirement(6, 20))),
        RoadmapStep(9, 2_000, listOf(LevelRequirement(9, 5), LevelRequirement(8, 10), LevelRequirement(7, 20))),
        RoadmapStep(10, 5_000, listOf(LevelRequirement(10, 5), LevelRequirement(9, 10), LevelRequirement(8, 20))),
    )

    fun currentStep(levelCounts: Map<Int, Int>): RoadmapStep =
        steps.last { step -> step.requirements.all { countAtOrAbove(levelCounts, it.level) >= it.users } }

    private fun countAtOrAbove(levelCounts: Map<Int, Int>, level: Int): Int =
        levelCounts.filterKeys { it >= level }.values.sum()
}
