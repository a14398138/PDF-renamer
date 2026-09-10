package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object DemoPaperGenerator {

    data class SamplePaperInfo(
        val initialFileName: String,
        val title: String,
        val authors: String,
        val doi: String,
        val year: String,
        val journal: String
    )

    val SAMPLES = listOf(
        SamplePaperInfo(
            initialFileName = "downloaded_paper_1706.03762.pdf",
            title = "Attention Is All You Need",
            authors = "Ashish Vaswani, Noam Shazeer, Niki Parmar, et al.",
            doi = "10.48550/arXiv.1706.03762",
            year = "2017",
            journal = "NeurIPS / arXiv"
        ),
        SamplePaperInfo(
            initialFileName = "nature14539_full_text.pdf",
            title = "Deep learning",
            authors = "Yann LeCun, Yoshua Bengio, Geoffrey Hinton",
            doi = "10.1038/nature14539",
            year = "2015",
            journal = "Nature"
        ),
        SamplePaperInfo(
            initialFileName = "science_doudna_crispr.pdf",
            title = "A Programmable Dual-RNA-Guided DNA Endonuclease in Adaptive Bacterial Immunity",
            authors = "Martin Jinek, Krzysztof Chylinski, Ines Fonfara, Emmanuelle Charpentier, Jennifer A. Doudna",
            doi = "10.1126/science.1225829",
            year = "2012",
            journal = "Science"
        )
    )

    /**
     * Generates standard sample PDF files in the target directory with real DOIs
     */
    fun generateSamplePapersInDir(targetDir: File): List<File> {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val generatedFiles = mutableListOf<File>()
        for (sample in SAMPLES) {
            val file = File(targetDir, sample.initialFileName)
            createPdfFile(file, sample)
            generatedFiles.add(file)
        }
        return generatedFiles
    }

    fun createPdfFile(outputFile: File, sample: SamplePaperInfo) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Header bar
        paint.color = Color.rgb(30, 58, 138) // Deep Navy
        canvas.drawRect(0f, 0f, 595f, 60f, paint)

        // Header text
        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("RESEARCH PAPER REPOSITORY PREVIEW", 40f, 36f, paint)

        // Title
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        paint.isFakeBoldText = true
        val titleLines = splitTextIntoLines(sample.title, 40)
        var y = 110f
        for (line in titleLines) {
            canvas.drawText(line, 40f, y, paint)
            y += 24f
        }

        // Authors
        y += 10f
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 13f
        paint.isFakeBoldText = false
        canvas.drawText(sample.authors, 40f, y, paint)

        // Journal & Year
        y += 20f
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 12f
        canvas.drawText("${sample.journal} (${sample.year})", 40f, y, paint)

        // Divider
        y += 20f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(40f, y, 555f, y, paint)

        // DOI Box (Distinct badge to ensure DOI is clearly visible & embedded)
        y += 30f
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(40f, y, 555f, y + 45f, 8f, 8f, paint)

        paint.color = Color.rgb(30, 58, 138)
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("DOI:", 56f, y + 27f, paint)

        paint.color = Color.rgb(14, 116, 144)
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val doiText = "https://doi.org/${sample.doi}"
        canvas.drawText(doiText, 95f, y + 27f, paint)

        // Abstract paragraph
        y += 75f
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Abstract", 40f, y, paint)

        y += 20f
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("This is an academic research document used for demonstrating automated DOI detection,", 40f, y, paint)
        y += 18f
        canvas.drawText("bibliographic metadata fetching via CrossRef, and formatted literature renaming.", 40f, y, paint)
        y += 18f
        canvas.drawText("Target DOI: ${sample.doi}", 40f, y, paint)

        document.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun splitTextIntoLines(text: String, maxCharsPerLine: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine = word
            } else if ((currentLine + " " + word).length <= maxCharsPerLine) {
                currentLine += " $word"
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
