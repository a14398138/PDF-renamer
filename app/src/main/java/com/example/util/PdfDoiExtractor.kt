package com.example.util

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.nio.charset.StandardCharsets

object PdfDoiExtractor {

    // Regex for matching standard DOIs (10.xxxx/...)
    // Handles variants like:
    // https://doi.org/10.1038/nature14539
    // doi:10.1145/3290605.3300233
    // 10.48550/arXiv.1706.03762
    private val DOI_REGEX = Regex(
        "(?:doi(?:\\.org)?[:\\/\\s]*|https?:\\/\\/(?:dx\\.)?doi\\.org\\/)?\\b(10\\.\\d{4,9}\\/[-._;()\\/:A-Za-z0-9]+)",
        RegexOption.IGNORE_CASE
    )

    // Regex for arXiv identifier (e.g., arXiv:1706.03762 or 1706.03762)
    private val ARXIV_REGEX = Regex(
        "(?:arxiv[:\\/\\s]*)?(\\d{4}\\.\\d{4,5}(?:v\\d+)?)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Attempts to extract a DOI from:
     * 1. The PDF content (first 256KB-512KB where headers, metadata & first page reside)
     * 2. The filename itself
     */
    fun extractDoiFromUri(context: Context, uri: Uri, fileName: String): String? {
        // First check the file name itself for obvious DOI or arXiv ID
        val fromFileName = extractDoiFromText(fileName)
        if (!fromFileName.isNullOrBlank()) {
            return fromFileName
        }

        // Next, inspect PDF bytes
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                extractDoiFromStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractDoiFromStream(stream: InputStream): String? {
        return try {
            // Read up to 512KB from the beginning of the PDF
            val buffer = ByteArray(512 * 1024)
            var totalRead = 0
            while (totalRead < buffer.size) {
                val bytesRead = stream.read(buffer, totalRead, buffer.size - totalRead)
                if (bytesRead == -1) break
                totalRead += bytesRead
            }

            if (totalRead <= 0) return null

            // Search in Latin1 / UTF-8 string conversions
            val textIso = String(buffer, 0, totalRead, StandardCharsets.ISO_8859_1)
            val detected = extractDoiFromText(textIso)
            if (!detected.isNullOrBlank()) {
                return detected
            }

            val textUtf8 = String(buffer, 0, totalRead, StandardCharsets.UTF_8)
            extractDoiFromText(textUtf8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts and sanitizes the first valid DOI or arXiv identifier from text
     */
    fun extractDoiFromText(text: String): String? {
        val match = DOI_REGEX.find(text)
        if (match != null) {
            val rawDoi = match.groupValues[1]
            val cleaned = cleanExtractedDoi(rawDoi)
            if (isValidDoi(cleaned)) {
                return cleaned
            }
        }

        // Fallback: check if text has arXiv identifier
        val arxivMatch = ARXIV_REGEX.find(text)
        if (arxivMatch != null) {
            val arxivId = arxivMatch.groupValues[1]
            return "10.48550/arXiv.$arxivId"
        }

        return null
    }

    fun cleanExtractedDoi(raw: String): String {
        var doi = raw.trim()
        // Trim common trailing characters that might be caught at the end of a sentence/link
        val trailingArtifacts = charArrayOf('.', ',', ';', ')', ']', '}', '>', '<', '"', '\'', '/', '\\')
        while (doi.isNotEmpty() && trailingArtifacts.contains(doi.last())) {
            doi = doi.dropLast(1).trim()
        }
        return doi
    }

    fun isValidDoi(doi: String): Boolean {
        // Valid DOI starts with 10. and has a slash with suffix
        return doi.startsWith("10.") && doi.contains("/") && doi.length >= 7
    }
}
