package com.example.data.model

data class RenameFormat(
    val id: String,
    val label: String,
    val pattern: String,
    val useEtAl: Boolean = false,
    val spaceReplacement: String = " " // " " or "_"
) {
    companion object {
        val PRESETS = listOf(
            RenameFormat(
                id = "year_author_title_underscore",
                label = "出版年_著者_タイトル",
                pattern = "{year}_{author}_{title}",
                useEtAl = false,
                spaceReplacement = "_"
            ),
            RenameFormat(
                id = "year_author_title_spaces",
                label = "出版年 著者 タイトル",
                pattern = "{year} {author} {title}",
                useEtAl = false,
                spaceReplacement = " "
            ),
            RenameFormat(
                id = "bracket_year_author_dash_title",
                label = "[出版年] 著者 - タイトル",
                pattern = "[{year}] {author} - {title}",
                useEtAl = true,
                spaceReplacement = " "
            ),
            RenameFormat(
                id = "author_year_title",
                label = "著者 (出版年) タイトル",
                pattern = "{author} ({year}) {title}",
                useEtAl = true,
                spaceReplacement = " "
            )
        )

        fun defaultFormat(): RenameFormat = PRESETS[0]
    }

    fun formatFileName(
        year: String?,
        author: String?,
        title: String?,
        originalExtension: String = "pdf"
    ): String {
        val safeYear = if (!year.isNullOrBlank()) year.trim() else "UnknownYear"
        val safeAuthor = if (!author.isNullOrBlank()) author.trim() else "UnknownAuthor"
        val safeTitle = if (!title.isNullOrBlank()) title.trim() else "Untitled"

        var result = pattern
            .replace("{year}", safeYear)
            .replace("{author}", safeAuthor)
            .replace("{title}", safeTitle)

        if (spaceReplacement == "_") {
            // Replace multiple spaces with single underscore if requested
            result = result.replace(Regex("\\s+"), "_")
        }

        // Sanitize illegal filesystem characters: / \ : * ? " < > |
        result = result.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        // Collapse multiple underscores or spaces
        result = result.replace(Regex("_+"), "_").trim(' ', '_', '.')

        // Limit length to keep safe on Android file systems
        if (result.length > 150) {
            result = result.substring(0, 150).trimEnd(' ', '_')
        }

        val ext = originalExtension.trimStart('.')
        return "$result.$ext"
    }
}
