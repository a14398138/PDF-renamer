package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CrossRefResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: CrossRefWork? = null
)

@JsonClass(generateAdapter = true)
data class CrossRefWork(
    @Json(name = "title") val title: List<String>? = null,
    @Json(name = "author") val author: List<CrossRefAuthor>? = null,
    @Json(name = "issued") val issued: CrossRefDate? = null,
    @Json(name = "published-print") val publishedPrint: CrossRefDate? = null,
    @Json(name = "published-online") val publishedOnline: CrossRefDate? = null,
    @Json(name = "created") val created: CrossRefDate? = null,
    @Json(name = "container-title") val containerTitle: List<String>? = null,
    @Json(name = "publisher") val publisher: String? = null,
    @Json(name = "DOI") val doi: String? = null,
    @Json(name = "type") val type: String? = null
) {
    fun extractYear(): String? {
        val parts = issued?.dateParts?.firstOrNull()
            ?: publishedPrint?.dateParts?.firstOrNull()
            ?: publishedOnline?.dateParts?.firstOrNull()
            ?: created?.dateParts?.firstOrNull()
        val yearInt = parts?.firstOrNull()
        return yearInt?.toString()
    }

    fun extractPrimaryTitle(): String {
        return title?.firstOrNull()?.trim() ?: "Untitled"
    }

    fun extractAuthorsFormatted(): String {
        if (author.isNullOrEmpty()) return "Unknown"
        val firstAuthor = author.firstOrNull()
        val firstAuthorName = when {
            !firstAuthor?.family.isNullOrBlank() -> firstAuthor?.family
            !firstAuthor?.name.isNullOrBlank() -> firstAuthor?.name
            !firstAuthor?.given.isNullOrBlank() -> firstAuthor?.given
            else -> "Unknown"
        }
        return if (author.size > 1) {
            "$firstAuthorName et al."
        } else {
            firstAuthorName ?: "Unknown"
        }
    }

    fun extractFirstAuthorOnly(): String {
        if (author.isNullOrEmpty()) return "Unknown"
        val firstAuthor = author.firstOrNull()
        return when {
            !firstAuthor?.family.isNullOrBlank() -> firstAuthor?.family ?: "Unknown"
            !firstAuthor?.name.isNullOrBlank() -> firstAuthor?.name ?: "Unknown"
            !firstAuthor?.given.isNullOrBlank() -> firstAuthor?.given ?: "Unknown"
            else -> "Unknown"
        }
    }

    fun extractJournal(): String {
        return containerTitle?.firstOrNull()?.trim() ?: publisher?.trim() ?: ""
    }
}

@JsonClass(generateAdapter = true)
data class CrossRefAuthor(
    @Json(name = "given") val given: String? = null,
    @Json(name = "family") val family: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "sequence") val sequence: String? = null
)

@JsonClass(generateAdapter = true)
data class CrossRefDate(
    @Json(name = "date-parts") val dateParts: List<List<Int>>? = null
)
