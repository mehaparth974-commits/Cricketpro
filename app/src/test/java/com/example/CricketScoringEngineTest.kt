package com.example

import com.example.domain.BallEvent
import com.example.domain.CricketMatch
import com.example.domain.CricketScoringEngine
import com.example.domain.DismissalType
import com.example.domain.ExtraType
import com.example.domain.MatchStatus
import com.example.domain.TournamentEngine
import com.example.domain.TournamentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CricketScoringEngineTest {

    private fun baseMatch(oversLimit: Int = 5, powerplayOvers: Int = 2): CricketMatch {
        return CricketMatch(
            id = "m1",
            teamAId = "tA",
            teamBId = "tB",
            teamAName = "Emerald Strikers",
            teamBName = "Midnight Royals",
            teamAShort = "EMS",
            teamBShort = "MNR",
            teamAPlayers = listOf("Aarav", "Vikram", "Rohan", "Kabir", "Dev", "Ishaan", "Siddharth", "Karan", "Nikhil", "Arjun", "Yash"),
            teamBPlayers = listOf("Zaid", "Pranav", "Varun", "Manish", "Rahul", "Deepak", "Suresh", "Harish", "Amit", "Tarun", "Kunal"),
            oversLimit = oversLimit,
            powerplayOvers = powerplayOvers,
            battingFirstTeam = "A",
            status = MatchStatus.LIVE.name,
            currentInnings = 1
        )
    }

    @Test
    fun `test extras strike rotation over completion and powerplay`() {
        val match = baseMatch(oversLimit = 5, powerplayOvers = 1)
        val balls = listOf(
            // Ball 1: 1 run -> Aarav & Vikram swap
            BallEvent(sequenceIndex = 0, innings = 1, overNumber = 0, ballInOver = 1, strikerName = "Aarav", nonStrikerName = "Vikram", bowlerName = "Zaid", runsScored = 1),
            // Wide -> legal balls stays 1, total +1, strike unchanged (Vikram on strike)
            BallEvent(sequenceIndex = 1, innings = 1, overNumber = 0, ballInOver = 2, strikerName = "Vikram", nonStrikerName = "Aarav", bowlerName = "Zaid", extraType = ExtraType.WIDE.name, extraRuns = 0),
            // No-ball + 4 -> legal balls stays 1, total +5 (1nb + 4bat), free hit next
            BallEvent(sequenceIndex = 2, innings = 1, overNumber = 0, ballInOver = 2, strikerName = "Vikram", nonStrikerName = "Aarav", bowlerName = "Zaid", runsScored = 4, extraType = ExtraType.NO_BALL.name),
            // Ball 2: 6 runs
            BallEvent(sequenceIndex = 3, innings = 1, overNumber = 0, ballInOver = 2, strikerName = "Vikram", nonStrikerName = "Aarav", bowlerName = "Zaid", runsScored = 6),
            // Ball 3: 2 leg byes
            BallEvent(sequenceIndex = 4, innings = 1, overNumber = 0, ballInOver = 3, strikerName = "Vikram", nonStrikerName = "Aarav", bowlerName = "Zaid", extraType = ExtraType.LEG_BYE.name, extraRuns = 2),
            // Ball 4: 0 runs
            BallEvent(sequenceIndex = 5, innings = 1, overNumber = 0, ballInOver = 4, strikerName = "Vikram", nonStrikerName = "Aarav", bowlerName = "Zaid", runsScored = 0),
            // Ball 5: Bowled wicket!
            BallEvent(sequenceIndex = 6, innings = 1, overNumber = 0, ballInOver = 5, strikerName = "Vikram", nonStrikerName = "Aarav", bowlerName = "Zaid", wicket = true, dismissalType = DismissalType.BOWLED.name, dismissedPlayerName = "Vikram"),
            // Ball 6: 0 runs by Rohan -> Over completes, strike swaps to Aarav
            BallEvent(sequenceIndex = 7, innings = 1, overNumber = 0, ballInOver = 6, strikerName = "Rohan", nonStrikerName = "Aarav", bowlerName = "Zaid", runsScored = 0)
        )

        val computed = CricketScoringEngine.computeMatch(match, balls)
        val inn1 = computed.innings1

        assertEquals(15, inn1.totalRuns) // 1 + 1wd + 5nb + 6 + 2lb + 0 + 0 + 0 = 15
        assertEquals(1, inn1.wickets)
        assertEquals(6, inn1.legalBalls)
        assertEquals("1.0", inn1.oversDisplay)
        assertEquals(4, inn1.totalExtras) // 1wd + 1nb + 2lb
        assertEquals("Aarav", inn1.currentStriker)
        assertEquals("Rohan", inn1.currentNonStriker)
        assertFalse(inn1.powerplayActive) // over 1 >= powerplayOvers(1)
    }

    @Test
    fun `test second innings chase target required run rate and result`() {
        val match = baseMatch(oversLimit = 2, powerplayOvers = 1).copy(currentInnings = 2)
        // 1st innings: 20 runs in 2 overs
        val inn1Balls = (0 until 12).map { i ->
            BallEvent(
                sequenceIndex = i,
                innings = 1,
                overNumber = i / 6,
                ballInOver = (i % 6) + 1,
                strikerName = "Aarav",
                nonStrikerName = "Vikram",
                bowlerName = "Zaid",
                runsScored = if (i < 5) 4 else 0
            )
        }
        // 2nd innings: 22 runs in 4 balls -> target 21 surpassed
        val inn2Balls = listOf(
            BallEvent(sequenceIndex = 12, innings = 2, overNumber = 0, ballInOver = 1, strikerName = "Zaid", nonStrikerName = "Pranav", bowlerName = "Karan", runsScored = 6),
            BallEvent(sequenceIndex = 13, innings = 2, overNumber = 0, ballInOver = 2, strikerName = "Zaid", nonStrikerName = "Pranav", bowlerName = "Karan", runsScored = 6),
            BallEvent(sequenceIndex = 14, innings = 2, overNumber = 0, ballInOver = 3, strikerName = "Zaid", nonStrikerName = "Pranav", bowlerName = "Karan", runsScored = 6),
            BallEvent(sequenceIndex = 15, innings = 2, overNumber = 0, ballInOver = 4, strikerName = "Zaid", nonStrikerName = "Pranav", bowlerName = "Karan", runsScored = 4)
        )

        val computed = CricketScoringEngine.computeMatch(match, inn1Balls + inn2Balls)
        assertEquals(21, computed.innings2.target)
        assertEquals(22, computed.innings2.totalRuns)
        assertTrue(computed.innings2.inningsCompleted)
        assertEquals(MatchStatus.COMPLETED, computed.matchStatus)
        assertTrue(computed.resultText.contains("Midnight Royals won by 10 wickets"))
    }

    @Test
    fun `test retired hurt does not count as wicket while retired out does`() {
        val match = baseMatch()
        val balls = listOf(
            BallEvent(sequenceIndex = 0, innings = 1, overNumber = 0, ballInOver = 1, strikerName = "Aarav", nonStrikerName = "Vikram", bowlerName = "Zaid", wicket = false, dismissalType = DismissalType.RETIRED_HURT.name, dismissedPlayerName = "Aarav"),
            BallEvent(sequenceIndex = 1, innings = 1, overNumber = 0, ballInOver = 2, strikerName = "Rohan", nonStrikerName = "Vikram", bowlerName = "Zaid", wicket = true, dismissalType = DismissalType.RETIRED_OUT.name, dismissedPlayerName = "Rohan")
        )
        val computed = CricketScoringEngine.computeMatch(match, balls)
        assertEquals(1, computed.innings1.wickets)
        val aarav = computed.innings1.batters.first { it.name == "Aarav" }
        val rohan = computed.innings1.batters.first { it.name == "Rohan" }
        assertTrue(aarav.isRetiredHurt)
        assertFalse(aarav.isOut)
        assertTrue(rohan.isOut)
    }

    @Test
    fun `test tournament nrr and fixture generation`() {
        val nrr = TournamentEngine.calculateNrr(
            runsFor = 180,
            ballsFaced = 120,
            runsAgainst = 150,
            ballsBowled = 120
        )
        assertEquals(1.5, nrr, 0.001)

        val teams = listOf("1" to "A", "2" to "B", "3" to "C", "4" to "D")
        val rrFixtures = TournamentEngine.generateFixtures(TournamentFormat.ROUND_ROBIN, teams)
        assertEquals(6, rrFixtures.size)

        val koFixtures = TournamentEngine.generateFixtures(TournamentFormat.KNOCKOUT, teams)
        assertEquals(2, koFixtures.size)

        val grpFixtures = TournamentEngine.generateFixtures(TournamentFormat.GROUP_STAGE, teams)
        assertEquals(2, grpFixtures.size)
    }
}
