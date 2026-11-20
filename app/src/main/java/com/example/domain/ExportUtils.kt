package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ExportUtils {

    fun exportMatchPdfAndShare(
        context: Context,
        match: CricketMatch,
        computed: MatchComputedState
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = "${match.teamAShort}_vs_${match.teamBShort}".replace("[^A-Za-z0-9_]".toRegex(), "_")
        val pdfFile = File(exportDir, "CricketPro_${safeName}.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerBg = Paint().apply { color = Color.rgb(10, 20, 15) }
        val emeraldPaint = Paint().apply {
            color = Color.rgb(0, 200, 100)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 11f
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(4, 120, 87)
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
        }
        val monoPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.MONOSPACE
        }

        // Header banner
        canvas.drawRect(0f, 0f, 595f, 92f, headerBg)
        canvas.drawText("CRICKETPRO OFFICIAL SCORECARD", 28f, 34f, emeraldPaint)
        canvas.drawText("${match.teamAName} vs ${match.teamBName}", 28f, 60f, titlePaint)
        canvas.drawText(
            "${match.groundName} • ${match.oversLimit} Overs • ${computed.resultText}",
            28f,
            78f,
            subPaint
        )

        var y = 118f

        fun drawInningsBlock(inn: InningsComputedState) {
            if (y > 760f) return
            canvas.drawText(
                "${inn.battingTeamName.uppercase(Locale.US)} — ${inn.totalRuns}/${inn.wickets} (${inn.oversDisplay} ov, CRR ${String.format(Locale.US, "%.2f", inn.crr)})",
                28f,
                y,
                sectionPaint
            )
            y += 16f
            canvas.drawText("BATTER                         R    B   4s   6s    SR", 28f, y, monoPaint)
            y += 12f
            for (b in inn.batters.take(11)) {
                if (y > 790f) break
                val line = String.format(
                    Locale.US,
                    "%-28s %3d  %3d  %3d  %3d  %5.1f  (%s)",
                    b.name.take(26),
                    b.runs,
                    b.balls,
                    b.fours,
                    b.sixes,
                    b.strikeRate,
                    b.dismissalText.take(22)
                )
                canvas.drawText(line, 28f, y, monoPaint)
                y += 12f
            }
            y += 4f
            canvas.drawText(
                "Extras: ${inn.totalExtras} (wd ${inn.wides}, nb ${inn.noBalls}, b ${inn.byes}, lb ${inn.legByes})",
                28f,
                y,
                bodyPaint
            )
            y += 14f
            canvas.drawText("BOWLER                         O    M    R    W   ECON", 28f, y, monoPaint)
            y += 12f
            for (bw in inn.bowlers.take(8)) {
                if (y > 805f) break
                val line = String.format(
                    Locale.US,
                    "%-28s %4s  %2d  %3d  %2d   %4.2f",
                    bw.name.take(26),
                    bw.oversDisplay,
                    bw.maidens,
                    bw.runsConceded,
                    bw.wickets,
                    bw.economy
                )
                canvas.drawText(line, 28f, y, monoPaint)
                y += 12f
            }
            y += 18f
        }

        drawInningsBlock(computed.innings1)
        if (computed.innings2.legalBalls > 0 || computed.innings2.batters.isNotEmpty()) {
            drawInningsBlock(computed.innings2)
        }

        if (!match.aiSummary.isNullOrBlank() && y < 780f) {
            canvas.drawText("AI MATCH INSIGHTS", 28f, y, sectionPaint)
            y += 14f
            val words = match.aiSummary.split(" ")
            var line = StringBuilder()
            for (w in words) {
                if (line.length + w.length > 82) {
                    if (y < 820f) canvas.drawText(line.toString(), 28f, y, bodyPaint)
                    y += 12f
                    line = StringBuilder(w)
                } else {
                    if (line.isNotEmpty()) line.append(" ")
                    line.append(w)
                }
            }
            if (line.isNotEmpty() && y < 820f) {
                canvas.drawText(line.toString(), 28f, y, bodyPaint)
            }
        }

        doc.finishPage(page)
        FileOutputStream(pdfFile).use { out -> doc.writeTo(out) }
        doc.close()

        shareFile(context, pdfFile, "application/pdf", "Share CricketPro PDF Scorecard")
    }

    fun exportMatchCsvAndShare(
        context: Context,
        match: CricketMatch,
        balls: List<BallEvent>,
        computed: MatchComputedState
    ): Result<Unit> = runCatching {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = "${match.teamAShort}_vs_${match.teamBShort}".replace("[^A-Za-z0-9_]".toRegex(), "_")
        val csvFile = File(exportDir, "CricketPro_${safeName}_stats.csv")

        val sb = StringBuilder()
        sb.appendLine("Match,${match.teamAName} vs ${match.teamBName}")
        sb.appendLine("Result,${computed.resultText}")
        sb.appendLine("Ground,${match.groundName}")
        sb.appendLine()
        sb.appendLine("Innings,Over,Ball,Striker,NonStriker,Bowler,Runs,ExtraType,ExtraRuns,Wicket,Dismissal,Commentary")
        for (b in balls.sortedBy { it.sequenceIndex }) {
            val comm = b.commentary.replace(",", ";")
            sb.appendLine("${b.innings},${b.overNumber},${b.ballInOver},${b.strikerName},${b.nonStrikerName},${b.bowlerName},${b.runsScored},${b.extraType},${b.extraRuns},${b.wicket},${b.dismissalType},$comm")
        }
        csvFile.writeText(sb.toString())
        shareFile(context, csvFile, "text/csv", "Export CricketPro Match CSV")
    }

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
