package com.example.data.repository

import android.util.Log
import com.example.domain.CricketMatch
import com.example.domain.MatchComputedState
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiMatchInsights(
    val headline: String,
    val summaryParagraph: String,
    val turningPoint: String,
    val tacticalRecommendation: String,
    val highlightMoments: List<String>
)

class AiInsightsRepository {
    companion object {
        private const val TAG = "AiInsightsRepository"
    }

    private val insightsSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "Punchy broadcast headline for the match"),
            "summaryParagraph" to Schema.string(description = "Concise 2-4 sentence match summary"),
            "turningPoint" to Schema.string(description = "The decisive turning point or partnership"),
            "tacticalRecommendation" to Schema.string(description = "Tactical insight for batting or bowling execution"),
            "highlightMoments" to Schema.array(
                items = Schema.string(),
                description = "3-5 short key highlight bullet moments"
            )
        )
    )

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightsSchema
            }
        )
    }

    suspend fun generateMatchInsights(
        match: CricketMatch,
        computed: MatchComputedState
    ): Result<AiMatchInsights> {
        val inn1 = computed.innings1
        val inn2 = computed.innings2
        val topBat1 = inn1.batters.sortedByDescending { it.runs }.take(3)
            .joinToString { "${it.name} ${it.runs}(${it.balls})" }
        val topBowl1 = inn1.bowlers.sortedByDescending { it.wickets }.take(2)
            .joinToString { "${it.name} ${it.wickets}/${it.runsConceded}" }
        val topBat2 = inn2.batters.sortedByDescending { it.runs }.take(3)
            .joinToString { "${it.name} ${it.runs}(${it.balls})" }
        val topBowl2 = inn2.bowlers.sortedByDescending { it.wickets }.take(2)
            .joinToString { "${it.name} ${it.wickets}/${it.runsConceded}" }
        val mvp = computed.mvpLeaderboard.firstOrNull()?.let { "${it.playerName} (${String.format("%.1f", it.points)} pts)" } ?: "TBD"

        val prompt = """
            Analyze this real cricket match and generate structured broadcast insights.
            Match: ${match.teamAName} vs ${match.teamBName} (${match.oversLimit} overs) at ${match.groundName}.
            Status/Result: ${computed.resultText}
            1st Innings: ${inn1.battingTeamName} ${inn1.totalRuns}/${inn1.wickets} in ${inn1.oversDisplay} ov (CRR ${String.format("%.2f", inn1.crr)}, Dot% ${inn1.dotBallPct}%, Boundary% ${inn1.boundaryPct}%).
            Top Batters 1st Inn: ${topBat1.ifBlank { "None yet" }}. Top Bowlers: ${topBowl1.ifBlank { "None yet" }}.
            2nd Innings: ${inn2.battingTeamName} ${inn2.totalRuns}/${inn2.wickets} in ${inn2.oversDisplay} ov (CRR ${String.format("%.2f", inn2.crr)}, Target ${inn2.target}).
            Top Batters 2nd Inn: ${topBat2.ifBlank { "None yet" }}. Top Bowlers: ${topBowl2.ifBlank { "None yet" }}.
            MVP Leader: $mvp.
            Keep every field grounded strictly in these real figures.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("Gemini returned an empty response."))
            val json = JSONObject(jsonText)
            val momentsArray = json.optJSONArray("highlightMoments")
            val moments = mutableListOf<String>()
            if (momentsArray != null) {
                for (i in 0 until momentsArray.length()) {
                    moments.add(momentsArray.getString(i))
                }
            }
            Result.success(
                AiMatchInsights(
                    headline = json.optString("headline", "${match.teamAShort} vs ${match.teamBShort} Match Brief"),
                    summaryParagraph = json.optString("summaryParagraph", computed.resultText),
                    turningPoint = json.optString("turningPoint", "Key middle-overs spell."),
                    tacticalRecommendation = json.optString("tacticalRecommendation", "Maintain dot-ball pressure."),
                    highlightMoments = moments
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI insight generation timed out", e)
            Result.failure(IllegalStateException("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI insight generation failed", e)
            Result.failure(e)
        }
    }
}
