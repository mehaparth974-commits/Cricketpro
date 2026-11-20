package com.example.domain

import kotlin.math.roundToInt

data class GeneratedFixture(
    val teamAId: String,
    val teamBId: String,
    val teamAName: String,
    val teamBName: String,
    val roundLabel: String
)

object TournamentEngine {

    /**
     * Calculates Net Run Rate per ICC rules:
     * NRR = (Runs Scored / Overs Faced) - (Runs Conceded / Overs Bowled)
     * Note: 1 over = 6 balls. If balls == 0, rate for that side is 0.0.
     */
    fun calculateNrr(
        runsFor: Int,
        ballsFaced: Int,
        runsAgainst: Int,
        ballsBowled: Int
    ): Double {
        val forRate = if (ballsFaced > 0) (runsFor * 6.0) / ballsFaced else 0.0
        val againstRate = if (ballsBowled > 0) (runsAgainst * 6.0) / ballsBowled else 0.0
        val raw = forRate - againstRate
        return (raw * 1000.0).roundToInt() / 1000.0
    }

    fun generateFixtures(
        format: TournamentFormat,
        teams: List<Pair<String, String>>
    ): List<GeneratedFixture> {
        if (teams.size < 2) return emptyList()
        return when (format) {
            TournamentFormat.ROUND_ROBIN, TournamentFormat.LEAGUE -> {
                val fixtures = mutableListOf<GeneratedFixture>()
                val list = teams.toMutableList()
                val hasBye = list.size % 2 != 0
                if (hasBye) list.add("BYE" to "BYE")
                val n = list.size
                val rounds = n - 1
                val rotating = list.drop(1).toMutableList()
                for (round in 1..rounds) {
                    val currentOrder = listOf(list.first()) + rotating
                    for (i in 0 until n / 2) {
                        val a = currentOrder[i]
                        val b = currentOrder[n - 1 - i]
                        if (a.first != "BYE" && b.first != "BYE") {
                            fixtures.add(
                                GeneratedFixture(
                                    teamAId = a.first,
                                    teamBId = b.first,
                                    teamAName = a.second,
                                    teamBName = b.second,
                                    roundLabel = "Round $round"
                                )
                            )
                        }
                    }
                    val last = rotating.removeAt(rotating.lastIndex)
                    rotating.add(0, last)
                }
                fixtures
            }

            TournamentFormat.KNOCKOUT -> {
                val fixtures = mutableListOf<GeneratedFixture>()
                val shuffled = teams.toList()
                val roundLabel = when {
                    shuffled.size <= 2 -> "Final"
                    shuffled.size <= 4 -> "Semi-Final"
                    shuffled.size <= 8 -> "Quarter-Final"
                    else -> "Knockout Round 1"
                }
                var idx = 0
                while (idx + 1 < shuffled.size) {
                    val a = shuffled[idx]
                    val b = shuffled[idx + 1]
                    fixtures.add(
                        GeneratedFixture(
                            teamAId = a.first,
                            teamBId = b.first,
                            teamAName = a.second,
                            teamBName = b.second,
                            roundLabel = roundLabel
                        )
                    )
                    idx += 2
                }
                fixtures
            }

            TournamentFormat.GROUP_STAGE -> {
                val mid = (teams.size + 1) / 2
                val groupA = teams.subList(0, mid)
                val groupB = teams.subList(mid, teams.size)
                val fixtures = mutableListOf<GeneratedFixture>()
                fixtures.addAll(
                    generateFixtures(TournamentFormat.ROUND_ROBIN, groupA)
                        .map { it.copy(roundLabel = "Group A • ${it.roundLabel}") }
                )
                if (groupB.size >= 2) {
                    fixtures.addAll(
                        generateFixtures(TournamentFormat.ROUND_ROBIN, groupB)
                            .map { it.copy(roundLabel = "Group B • ${it.roundLabel}") }
                    )
                }
                fixtures
            }
        }
    }

    fun evaluateEarnedBadges(profile: PlayerProfile): List<String> {
        val badges = profile.badges.toMutableSet()
        if (profile.matchesPlayed >= 1) badges.add("Debutant")
        if (profile.highestScore >= 50 || profile.fifties >= 1) badges.add("Half-Century Club")
        if (profile.highestScore >= 100 || profile.hundreds >= 1) badges.add("Century Maker")
        if (profile.sixes >= 10) badges.add("Six Machine")
        if (profile.wicketsTaken >= 5) badges.add("Strike Bowler")
        if (profile.bestBowlingWickets >= 5) badges.add("Five-For Hero")
        if (profile.mvpPoints >= 25.0) badges.add("MVP Star")
        return badges.toList()
    }
}
