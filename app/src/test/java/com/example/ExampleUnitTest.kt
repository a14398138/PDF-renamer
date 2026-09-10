package com.example

import com.example.data.model.RenameFormat
import com.example.data.remote.CrossRefClient
import com.example.util.PdfDoiExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDoiExtractionFromText() {
        val sampleText = "Please cite this paper as: Nature volume 521, pages 436–444 (2015). doi:10.1038/nature14539."
        val doi = PdfDoiExtractor.extractDoiFromText(sampleText)
        assertEquals("10.1038/nature14539", doi)
    }

    @Test
    fun testArxivDoiExtraction() {
        val sampleText = "Preprint arXiv:1706.03762v7 [cs.CL] 2 Aug 2023"
        val doi = PdfDoiExtractor.extractDoiFromText(sampleText)
        assertEquals("10.48550/arXiv.1706.03762v7", doi)
    }

    @Test
    fun testCleanDoi() {
        val urlDoi = "https://doi.org/10.1145/3290605.3300233"
        val cleaned = CrossRefClient.cleanDoi(urlDoi)
        assertEquals("10.1145/3290605.3300233", cleaned)
    }

    @Test
    fun testRenameFormatting() {
        val format = RenameFormat.defaultFormat()
        val formatted = format.formatFileName(
            year = "2017",
            author = "Vaswani",
            title = "Attention Is All You Need"
        )
        assertEquals("2017_Vaswani_Attention_Is_All_You_Need.pdf", formatted)
    }

    @Test
    fun testRenameFormattingWithSpecialCharacters() {
        val format = RenameFormat.defaultFormat()
        val formatted = format.formatFileName(
            year = "2023",
            author = "O'Connor",
            title = "Quantum/Neural: Are We There Yet?"
        )
        // Slashes and colons should be sanitized
        assertTrue(!formatted.contains("/"))
        assertTrue(!formatted.contains(":"))
        assertTrue(!formatted.contains("?"))
        assertTrue(formatted.endsWith(".pdf"))
    }
}
