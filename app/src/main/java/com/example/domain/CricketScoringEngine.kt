package com.example.domain

import kotlin.math.max
import kotlin.math.roundToInt

data class BatterLine(
    val name: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val isRetiredHurt: Boolean = false,
    val dismissalText: String = "not out"
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs * 100.0) / balls else 0.0
}

data class BowlerLine(
    val name: String,
    val legalBalls: Int = 0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0
) {
    val oversDisplay: String
        get() = "${legalBalls / 6}.${legalBalls % 6}"

    val economy: Double
        get() = if (legalBalls > 0) (runsConceded * 6.0) / legalBalls else 0.0
}

data class FallOfWicket(
    val wicketNumber: Int,
    val scoreAtWicket: Int,
    val batterName: String,
    val overDisplay: String
)

data class PartnershipStat(
    val wicketLabel: String,
    val batter1: String,
    val batter2: String,
    val runs: Int,
    val balls: Int
)

data class OverSummary(
    val overNumber: Int,
    val runs: Int,
    val wickets: Int,
    val cumulativeRuns: Int,
    val runRateAtOver: Double,
    val balls: List<BallEvent>
)

data class MvpCandidate(
    val playerName: String,
    val teamName: String,
    val points: Double,
    val runs: Int,
    val wickets: Int,
    val fieldingDismissals: Int
)

data class InningsComputedState(
    val inningsNumber: Int,
    val battingTeamName: String,
    val bowlingTeamName: String,
    val totalRuns: Int,
    val wickets: Int,
    val legalBalls: Int,
    val oversDisplay: String,
    val crr: Double,
    val target: Int,
    val runsNeeded: Int,
    val ballsRemaining: Int,
    val rrr: Double,
    val wides: Int,
    val noBalls: Int,
    val byes: Int,
    val legByes: Int,
    val totalExtras: Int,
    val currentStriker: String,
    val currentNonStriker: String,
    val currentBowler: String,
    val batters: List<BatterLine>,
    val bowlers: List<BowlerLine>,
    val fallOfWickets: List<FallOfWicket>,
    val partnerships: List<PartnershipStat>,
    val overSummaries: List<OverSummary>,
    val currentOverBalls: List<BallEvent>,
    val powerplayActive: Boolean,
    val freeHitActive: Boolean,
    val dotBallPct: Double,
    val boundaryPct: Double,
    val inningsCompleted: Boolean
)

data class MatchComputedState(
    val innings1: InningsComputedState,
    val innings2: InningsComputedState,
    val activeInnings: InningsComputedState,
    val matchStatus: MatchStatus,
    val resultText: String,
    val mvpLeaderboard: List<MvpCandidate>
)

object CricketScoringEngine {

    fun computeMatch(match: CricketMatch, balls: List<BallEvent>): MatchComputedState {
        val sorted = balls.sortedBy { it.sequenceIndex }
        val team1BatFirst = match.battingFirstTeam == "A"
        val inn1BatName = if (team1BatFirst) match.teamAName else match.teamBName
        val inn1BowlName = if (team1BatFirst) match.teamBName else match.teamAName
        val inn2BatName = inn1BowlName
        val inn2BowlName = inn1BatName

        val inn1Players = if (team1BatFirst) match.teamAPlayers else match.teamBPlayers
        val inn2Players = if (team1BatFirst) match.teamBPlayers else match.teamAPlayers

        val inn1Balls = sorted.filter { it.innings == 1 }
        val inn2Balls = sorted.filter { it.innings == 2 }

        val inn1State = computeInnings(
            inningsNumber = 1,
            battingTeamName = inn1BatName,
            bowlingTeamName = inn1BowlName,
            battingRoster = inn1Players,
            bowlingRoster = inn2Players,
            balls = inn1Balls,
            oversLimit = match.oversLimit,
            powerplayOvers = match.powerplayOvers,
            target = 0
        )

        val targetForInn2 = if (inn1State.inningsCompleted || match.currentInnings == 2 || inn2Balls.isNotEmpty()) {
            inn1State.totalRuns + 1
        } else {
            0
        }

        val inn2State = computeInnings(
            inningsNumber = 2,
            battingTeamName = inn2BatName,
            bowlingTeamName = inn2BowlName,
            battingRoster = inn2Players,
            bowlingRoster = inn1Players,
            balls = inn2Balls,
            oversLimit = match.oversLimit,
            powerplayOvers = match.powerplayOvers,
            target = targetForInn2
        )

        val computedStatus: MatchStatus
        val computedResult: String
        when {
            match.matchStatus == MatchStatus.COMPLETED -> {
                computedStatus = MatchStatus.COMPLETED
                computedResult = match.resultText.ifBlank {
                    resolveResult(inn1BatName, inn2BatName, inn1State, inn2State)
                }
            }
            inn2State.inningsCompleted -> {
                computedStatus = MatchStatus.COMPLETED
                computedResult = resolveResult(inn1BatName, inn2BatName, inn1State, inn2State)
            }
            match.currentInnings == 2 || inn2Balls.isNotEmpty() -> {
                computedStatus = MatchStatus.LIVE
                computedResult = if (inn2State.target > 0) {
                    "$inn2BatName need ${inn2State.runsNeeded} runs in ${inn2State.ballsRemaining} balls"
                } else {
                    "2nd Innings in progress"
                }
            }
            inn1State.inningsCompleted -> {
                computedStatus = MatchStatus.INNINGS_BREAK
                computedResult = "Innings Break — Target ${inn1State.totalRuns + 1} for $inn2BatName"
            }
            inn1Balls.isNotEmpty() || match.matchStatus == MatchStatus.LIVE -> {
                computedStatus = MatchStatus.LIVE
                computedResult = "$inn1BatName batting first"
            }
            else -> {
                computedStatus = MatchStatus.UPCOMING
                computedResult = "Toss: ${match.tossWinner} elected to ${match.tossDecision.lowercase()}"
            }
        }

        val activeInnings = if (match.currentInnings == 2 || inn2Balls.isNotEmpty()) inn2State else inn1State
        val mvpList = computeMvp(inn1State, inn2State, sorted)

        return MatchComputedState(
            innings1 = inn1State,
            innings2 = inn2State,
            activeInnings = activeInnings,
            matchStatus = computedStatus,
            resultText = computedResult,
            mvpLeaderboard = mvpList
        )
    }

    private fun resolveResult(
        inn1Team: String,
        inn2Team: String,
        inn1: InningsComputedState,
        inn2: InningsComputedState
    ): String {
        return when {
            inn2.totalRuns > inn1.totalRuns -> {
                val wicketsLeft = (10 - inn2.wickets).coerceAtLeast(1)
                "$inn2Team won by $wicketsLeft wicket${if (wicketsLeft == 1) "" else "s"}"
            }
            inn1.totalRuns > inn2.totalRuns -> {
                val margin = inn1.totalRuns - inn2.totalRuns
                "$inn1Team won by $margin run${if (margin == 1) "" else "s"}"
            }
            else -> "Match Tied"
        }
    }

    private fun computeInnings(
        inningsNumber: Int,
        battingTeamName: String,
        bowlingTeamName: String,
        battingRoster: List<String>,
        bowlingRoster: List<String>,
        balls: List<BallEvent>,
        oversLimit: Int,
        powerplayOvers: Int,
        target: Int
    ): InningsComputedState {
        val maxLegalBalls = oversLimit * 6
        var totalRuns = 0
        var wickets = 0
        var legalBalls = 0
        var wides = 0
        var noBalls = 0
        var byes = 0
        var legByes = 0

        val batterOrder = mutableListOf<String>()
        val batterMap = linkedMapOf<String, BatterLine>()
        val bowlerMap = linkedMapOf<String, BowlerLine>()
        val fows = mutableListOf<FallOfWicket>()
        val partnerships = mutableListOf<PartnershipStat>()

        var striker = battingRoster.getOrNull(0) ?: "Batter 1"
        var nonStriker = battingRoster.getOrNull(1) ?: "Batter 2"
        var bowler = bowlingRoster.firstOrNull() ?: "Bowler 1"

        fun ensureBatter(name: String) {
            if (name.isNotBlank() && !batterMap.containsKey(name)) {
                batterOrder.add(name)
                batterMap[name] = BatterLine(name = name)
            }
        }

        fun ensureBowler(name: String) {
            if (name.isNotBlank() && !bowlerMap.containsKey(name)) {
                bowlerMap[name] = BowlerLine(name = name)
            }
        }

        var currentPartnerRuns = 0
        var currentPartnerBalls = 0
        var currentPartnerWicketIndex = 1
        var pBatter1 = striker
        var pBatter2 = nonStriker

        var freeHit = false
        var dotBalls = 0
        var boundaryBalls = 0

        // Track runs per over per bowler for maiden detection
        val overRunsByOver = mutableMapOf<Int, Int>()
        val overLegalByOver = mutableMapOf<Int, Int>()
        val overBowler = mutableMapOf<Int, String>()

        for (ball in balls) {
            val sName = ball.strikerName.ifBlank { striker }
            val nsName = ball.nonStrikerName.ifBlank { nonStriker }
            val bName = ball.bowlerName.ifBlank { bowler }
            ensureBatter(sName)
            ensureBatter(nsName)
            ensureBowler(bName)

            striker = sName
            nonStriker = nsName
            bowler = bName
            if (pBatter1.isBlank()) pBatter1 = sName
            if (pBatter2.isBlank()) pBatter2 = nsName

            val ballRuns = ball.totalBallRuns
            totalRuns += ballRuns
            currentPartnerRuns += ballRuns

            val extra = ball.extraTypeEnum
            when (extra) {
                ExtraType.WIDE -> wides += 1 + ball.extraRuns
                ExtraType.NO_BALL -> noBalls += 1
                ExtraType.BYE -> byes += ball.extraRuns
                ExtraType.LEG_BYE -> legByes += ball.extraRuns
                ExtraType.NONE -> Unit
            }

            if (extra == ExtraType.NO_BALL && ball.extraRuns > 0) {
                byes += ball.extraRuns
            }

            val legal = ball.isLegalDelivery
            val currentOverIdx = legalBalls / 6
            if (legal) {
                legalBalls += 1
                currentPartnerBalls += 1
                overLegalByOver[currentOverIdx] = (overLegalByOver[currentOverIdx] ?: 0) + 1
                overBowler[currentOverIdx] = bName
            }

            val bowlerChargedRuns = when (extra) {
                ExtraType.NONE -> ball.runsScored
                ExtraType.WIDE -> 1 + ball.extraRuns
                ExtraType.NO_BALL -> 1 + ball.runsScored
                ExtraType.BYE, ExtraType.LEG_BYE -> 0
            }
            overRunsByOver[currentOverIdx] = (overRunsByOver[currentOverIdx] ?: 0) + bowlerChargedRuns

            if (ballRuns == 0 && !ball.wicket) dotBalls++
            if (ball.runsScored == 4 || ball.runsScored == 6) boundaryBalls++

            // Update batter line
            val curBatter = batterMap[sName] ?: BatterLine(sName)
            val batterBallIncrement = if (extra != ExtraType.WIDE) 1 else 0
            val batterRunsIncrement = if (extra == ExtraType.NONE || extra == ExtraType.NO_BALL) ball.runsScored else 0
            batterMap[sName] = curBatter.copy(
                runs = curBatter.runs + batterRunsIncrement,
                balls = curBatter.balls + batterBallIncrement,
                fours = curBatter.fours + if (batterRunsIncrement == 4) 1 else 0,
                sixes = curBatter.sixes + if (batterRunsIncrement == 6) 1 else 0
            )

            // Update bowler line
            val curBowler = bowlerMap[bName] ?: BowlerLine(bName)
            val dismissal = ball.dismissalTypeEnum
            val bowlerWicketInc = if (ball.wicket && dismissal.creditsBowler) 1 else 0
            bowlerMap[bName] = curBowler.copy(
                legalBalls = curBowler.legalBalls + if (legal) 1 else 0,
                runsConceded = curBowler.runsConceded + bowlerChargedRuns,
                wickets = curBowler.wickets + bowlerWicketInc,
                wides = curBowler.wides + if (extra == ExtraType.WIDE) 1 else 0,
                noBalls = curBowler.noBalls + if (extra == ExtraType.NO_BALL) 1 else 0
            )

            // Handle dismissal
            if (ball.wicket || dismissal == DismissalType.RETIRED_HURT) {
                val outName = ball.dismissedPlayerName?.takeIf { it.isNotBlank() } ?: sName
                ensureBatter(outName)
                val outBatter = batterMap[outName] ?: BatterLine(outName)
                val dismissalText = formatDismissal(dismissal, bName, ball.fielderName)
                if (dismissal.countsAsWicket) {
                    wickets += 1
                    batterMap[outName] = outBatter.copy(
                        isOut = true,
                        dismissalText = dismissalText
                    )
                    fows.add(
                        FallOfWicket(
                            wicketNumber = wickets,
                            scoreAtWicket = totalRuns,
                            batterName = outName,
                            overDisplay = "${legalBalls / 6}.${legalBalls % 6}"
                        )
                    )
                    partnerships.add(
                        PartnershipStat(
                            wicketLabel = "${currentPartnerWicketIndex}${ordinalSuffix(currentPartnerWicketIndex)} Wkt",
                            batter1 = pBatter1,
                            batter2 = pBatter2,
                            runs = currentPartnerRuns,
                            balls = currentPartnerBalls
                        )
                    )
                    currentPartnerWicketIndex += 1
                    currentPartnerRuns = 0
                    currentPartnerBalls = 0
                    pBatter1 = ""
                    pBatter2 = ""
                } else if (dismissal == DismissalType.RETIRED_HURT) {
                    batterMap[outName] = outBatter.copy(
                        isRetiredHurt = true,
                        dismissalText = "retired hurt"
                    )
                }
            }

            // Strike rotation
            val runningRuns = when (extra) {
                ExtraType.NONE -> ball.runsScored
                ExtraType.WIDE -> ball.extraRuns
                ExtraType.NO_BALL -> ball.runsScored + ball.extraRuns
                ExtraType.BYE, ExtraType.LEG_BYE -> ball.extraRuns
            }
            if (runningRuns % 2 == 1) {
                val tmp = striker
                striker = nonStriker
                nonStriker = tmp
            }

            // Over complete swap
            if (legal && legalBalls % 6 == 0) {
                val tmp = striker
                striker = nonStriker
                nonStriker = tmp
            }

            freeHit = extra == ExtraType.NO_BALL
        }

        // Compute maidens
        for ((overIdx, legalCount) in overLegalByOver) {
            if (legalCount >= 6 && (overRunsByOver[overIdx] ?: 0) == 0) {
                val bName = overBowler[overIdx]
                if (bName != null) {
                    bowlerMap[bName]?.let { existing ->
                        bowlerMap[bName] = existing.copy(maidens = existing.maidens + 1)
                    }
                }
            }
        }

        // Active unbroken partnership
        if (currentPartnerRuns > 0 || currentPartnerBalls > 0 || balls.isNotEmpty()) {
            partnerships.add(
                PartnershipStat(
                    wicketLabel = "${currentPartnerWicketIndex}${ordinalSuffix(currentPartnerWicketIndex)} Wkt*",
                    batter1 = pBatter1.ifBlank { striker },
                    batter2 = pBatter2.ifBlank { nonStriker },
                    runs = currentPartnerRuns,
                    balls = currentPartnerBalls
                )
            )
        }

        // Group over summaries
        val overSummaries = mutableListOf<OverSummary>()
        var runningCumulative = 0
        val ballsByOver = balls.groupBy { it.overNumber }
        val maxOverKey = ballsByOver.keys.maxOrNull() ?: -1
        for (overIdx in 0..maxOverKey) {
            val overBalls = ballsByOver[overIdx] ?: emptyList()
            if (overBalls.isNotEmpty()) {
                val oRuns = overBalls.sumOf { it.totalBallRuns }
                val oWickets = overBalls.count { it.wicket }
                runningCumulative += oRuns
                val overNumDisplay = overIdx + 1
                overSummaries.add(
                    OverSummary(
                        overNumber = overNumDisplay,
                        runs = oRuns,
                        wickets = oWickets,
                        cumulativeRuns = runningCumulative,
                        runRateAtOver = runningCumulative.toDouble() / overNumDisplay,
                        balls = overBalls
                    )
                )
            }
        }

        val currentOverIdx = legalBalls / 6
        val currentOverBalls = balls.filter { it.overNumber == currentOverIdx }

        val crr = if (legalBalls > 0) (totalRuns * 6.0) / legalBalls else 0.0
        val runsNeeded = if (target > 0) max(0, target - totalRuns) else 0
        val ballsRemaining = max(0, maxLegalBalls - legalBalls)
        val rrr = if (target > 0 && ballsRemaining > 0 && runsNeeded > 0) {
            (runsNeeded * 6.0) / ballsRemaining
        } else {
            0.0
        }

        val maxWickets = (battingRoster.size - 1).coerceIn(1, 10)
        val inningsCompleted = wickets >= maxWickets ||
            legalBalls >= maxLegalBalls ||
            (target > 0 && totalRuns >= target)

        val totalDeliveries = balls.size.coerceAtLeast(1)
        val dotPct = (dotBalls * 100.0) / totalDeliveries
        val boundaryPct = (boundaryBalls * 100.0) / totalDeliveries

        val oversDisplay = "${legalBalls / 6}.${legalBalls % 6}"
        val extrasTotal = wides + noBalls + byes + legByes

        return InningsComputedState(
            inningsNumber = inningsNumber,
            battingTeamName = battingTeamName,
            bowlingTeamName = bowlingTeamName,
            totalRuns = totalRuns,
            wickets = wickets,
            legalBalls = legalBalls,
            oversDisplay = oversDisplay,
            crr = crr,
            target = target,
            runsNeeded = runsNeeded,
            ballsRemaining = ballsRemaining,
            rrr = rrr,
            wides = wides,
            noBalls = noBalls,
            byes = byes,
            legByes = legByes,
            totalExtras = extrasTotal,
            currentStriker = striker,
            currentNonStriker = nonStriker,
            currentBowler = bowler,
            batters = batterOrder.mapNotNull { batterMap[it] },
            bowlers = bowlerMap.values.toList(),
            fallOfWickets = fows,
            partnerships = partnerships,
            overSummaries = overSummaries,
            currentOverBalls = currentOverBalls,
            powerplayActive = (legalBalls / 6) < powerplayOvers,
            freeHitActive = freeHit,
            dotBallPct = (dotPct * 10).roundToInt() / 10.0,
            boundaryPct = (boundaryPct * 10).roundToInt() / 10.0,
            inningsCompleted = inningsCompleted
        )
    }

    private fun computeMvp(
        inn1: InningsComputedState,
        inn2: InningsComputedState,
        allBalls: List<BallEvent>
    ): List<MvpCandidate> {
        val pointsMap = linkedMapOf<String, MvpCandidate>()

        fun addBat(teamName: String, batters: List<BatterLine>) {
            for (b in batters) {
                val cur = pointsMap[b.name] ?: MvpCandidate(b.name, teamName, 0.0, 0, 0, 0)
                val pts = b.runs * 0.1 + b.fours * 0.5 + b.sixes * 1.0 + (if (b.runs >= 50) 3.0 else 0.0)
                pointsMap[b.name] = cur.copy(
                    points = cur.points + pts,
                    runs = cur.runs + b.runs
                )
            }
        }

        fun addBowl(teamName: String, bowlers: List<BowlerLine>) {
            for (bw in bowlers) {
                val cur = pointsMap[bw.name] ?: MvpCandidate(bw.name, teamName, 0.0, 0, 0, 0)
                val econBonus = if (bw.legalBalls >= 12 && bw.economy < 6.5) 2.5 else 0.0
                val pts = bw.wickets * 3.5 + bw.maidens * 1.5 + econBonus
                pointsMap[bw.name] = cur.copy(
                    points = cur.points + pts,
                    wickets = cur.wickets + bw.wickets
                )
            }
        }

        addBat(inn1.battingTeamName, inn1.batters)
        addBat(inn2.battingTeamName, inn2.batters)
        addBowl(inn1.bowlingTeamName, inn1.bowlers)
        addBowl(inn2.bowlingTeamName, inn2.bowlers)

        for (ball in allBalls) {
            val fielder = ball.fielderName?.takeIf { it.isNotBlank() }
            if (ball.wicket && fielder != null) {
                val cur = pointsMap[fielder] ?: MvpCandidate(fielder, "", 0.0, 0, 0, 0)
                pointsMap[fielder] = cur.copy(
                    points = cur.points + 1.5,
                    fieldingDismissals = cur.fieldingDismissals + 1
                )
            }
        }

        return pointsMap.values.sortedByDescending { it.points }
    }

    fun formatDismissal(type: DismissalType, bowler: String, fielder: String?): String {
        val f = fielder?.takeIf { it.isNotBlank() }
        return when (type) {
            DismissalType.NONE -> "not out"
            DismissalType.BOWLED -> "b $bowler"
            DismissalType.CAUGHT -> if (f != null) "c $f b $bowler" else "c & b $bowler"
            DismissalType.LBW -> "lbw b $bowler"
            DismissalType.RUN_OUT -> if (f != null) "run out ($f)" else "run out"
            DismissalType.STUMPED -> if (f != null) "st $f b $bowler" else "st b $bowler"
            DismissalType.HIT_WICKET -> "hit wicket b $bowler"
            DismissalType.RETIRED_HURT -> "retired hurt"
            DismissalType.RETIRED_OUT -> "retired out"
        }
    }

    fun generateAutoCommentary(
        striker: String,
        bowler: String,
        runsScored: Int,
        extraType: ExtraType,
        extraRuns: Int,
        wicket: Boolean,
        dismissalType: DismissalType,
        fielder: String?
    ): String {
        if (wicket) {
            return when (dismissalType) {
                DismissalType.BOWLED -> "OUT! $bowler castles $striker! Timber!"
                DismissalType.CAUGHT -> "WICKET! $striker is caught${fielder?.let { " by $it" } ?: ""} off $bowler!"
                DismissalType.LBW -> "OUT! Plumb LBW! $bowler traps $striker in front."
                DismissalType.RUN_OUT -> "RUN OUT! Direct hit${fielder?.let { " by $it" } ?: ""}, $striker has to walk back!"
                DismissalType.STUMPED -> "STUMPED! Quick glovework${fielder?.let { " by $it" } ?: ""} off $bowler!"
                DismissalType.HIT_WICKET -> "HIT WICKET! Disaster for $striker off $bowler!"
                DismissalType.RETIRED_OUT -> "$striker retires out."
                DismissalType.RETIRED_HURT -> "$striker retires hurt."
                DismissalType.NONE -> "Dot ball."
            }
        }
        return when (extraType) {
            ExtraType.WIDE -> "Wide ball by $bowler (${1 + extraRuns} extra${if (extraRuns > 0) "s" else ""})."
            ExtraType.NO_BALL -> "NO BALL by $bowler! Free hit coming up! ${if (runsScored > 0) "$striker scores $runsScored." else ""}".trim()
            ExtraType.BYE -> "$extraRuns bye${if (extraRuns == 1) "" else "s"} taken past the keeper."
            ExtraType.LEG_BYE -> "$extraRuns leg-bye${if (extraRuns == 1) "" else "s"} off the pads."
            ExtraType.NONE -> when (runsScored) {
                0 -> "Good length from $bowler, defended solidly by $striker."
                1 -> "$striker nudges it into the gap for a single."
                2 -> "Well-placed by $striker, they come back for two."
                3 -> "Crisp timing! $striker and partner sprint back for three."
                4 -> "FOUR! $striker pierces the infield and it races to the fence!"
                6 -> "SIX! Massive hit from $striker, launched cleanly into the stands!"
                else -> "$runsScored runs scored by $striker off $bowler."
            }
        }
    }

    private fun ordinalSuffix(n: Int): String = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
}
