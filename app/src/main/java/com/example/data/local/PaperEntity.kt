package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaperStatus {
    PENDING,          // Newly detected, awaiting scan
    SCANNING,         // Extracting DOI / fetching metadata
    DOI_FOUND,        // DOI detected, awaiting metadata lookup
    READY_TO_RENAME,  // Metadata fetched, ready for rename
    RENAMED,          // Successfully renamed
    NO_DOI,           // No DOI found in PDF
    ERROR             // Network / parse error
}

@Entity(
    tableName = "papers",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["folderUriString"])
    ]
)
data class PaperEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val folderUriString: String,
    val originalFileName: String,
    val currentFileName: String,
    val suggestedFileName: String = "",
    val doi: String = "",
    val title: String = "",
    val firstAuthor: String = "",
    val authorsFormatted: String = "",
    val publicationYear: String = "",
    val journal: String = "",
    val status: PaperStatus = PaperStatus.PENDING,
    val fileSizeBytes: Long = 0L,
    val lastModified: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
