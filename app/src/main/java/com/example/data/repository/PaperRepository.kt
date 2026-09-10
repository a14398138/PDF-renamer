package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.PaperDao
import com.example.data.local.PaperEntity
import com.example.data.local.PaperStatus
import com.example.data.model.RenameFormat
import com.example.data.remote.CrossRefClient
import com.example.util.PdfDoiExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class PaperRepository(
    private val paperDao: PaperDao
) {
    fun getPapersByFolder(folderUri: String): Flow<List<PaperEntity>> =
        paperDao.getPapersByFolder(folderUri)

    fun getAllPapers(): Flow<List<PaperEntity>> =
        paperDao.getAllPapers()

    suspend fun getPaperById(id: Long): PaperEntity? =
        paperDao.getPaperById(id)

    /**
     * Scans a chosen folder (SAF URI or local directory) for PDF files,
     * registers them into the database, extracts DOIs, and fetches metadata.
     */
    suspend fun scanFolder(
        context: Context,
        folderUri: Uri,
        format: RenameFormat,
        onProgress: (scanned: Int, total: Int, currentName: String) -> Unit = { _, _, _ -> }
    ): List<PaperEntity> = withContext(Dispatchers.IO) {
        val folderUriStr = folderUri.toString()
        val pdfItems = mutableListOf<PdfFileInfo>()

        if (folderUri.scheme == "file") {
            val dir = File(folderUri.path ?: "")
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }?.forEach { f ->
                    pdfItems.add(
                        PdfFileInfo(
                            uri = Uri.fromFile(f),
                            name = f.name,
                            size = f.length(),
                            lastModified = f.lastModified(),
                            isLocal = true,
                            file = f
                        )
                    )
                }
            }
        } else {
            // Storage Access Framework DocumentFile
            val treeDoc = DocumentFile.fromTreeUri(context, folderUri)
            if (treeDoc != null && treeDoc.isDirectory) {
                treeDoc.listFiles().forEach { doc ->
                    if (doc.isFile && (doc.name?.endsWith(".pdf", ignoreCase = true) == true || doc.type == "application/pdf")) {
                        pdfItems.add(
                            PdfFileInfo(
                                uri = doc.uri,
                                name = doc.name ?: "paper.pdf",
                                size = doc.length(),
                                lastModified = doc.lastModified(),
                                isLocal = false,
                                documentFile = doc
                            )
                        )
                    }
                }
            }
        }

        val total = pdfItems.size
        val results = mutableListOf<PaperEntity>()

        pdfItems.forEachIndexed { index, item ->
            onProgress(index + 1, total, item.name)

            // Check if we already have this file in database
            val existing = paperDao.getPaperByUri(item.uri.toString())
            if (existing != null && existing.status == PaperStatus.RENAMED && existing.currentFileName == item.name) {
                results.add(existing)
                return@forEachIndexed
            }

            // Extract DOI
            val detectedDoi = PdfDoiExtractor.extractDoiFromUri(context, item.uri, item.name)

            var paper = existing?.copy(
                currentFileName = item.name,
                fileSizeBytes = item.size,
                lastModified = item.lastModified,
                updatedAt = System.currentTimeMillis()
            ) ?: PaperEntity(
                uriString = item.uri.toString(),
                folderUriString = folderUriStr,
                originalFileName = item.name,
                currentFileName = item.name,
                fileSizeBytes = item.size,
                lastModified = item.lastModified,
                updatedAt = System.currentTimeMillis()
            )

            if (!detectedDoi.isNullOrBlank()) {
                paper = paper.copy(
                    doi = detectedDoi,
                    status = PaperStatus.DOI_FOUND
                )

                // Fetch metadata from CrossRef
                val metaResult = CrossRefClient.fetchWorkByDoi(detectedDoi)
                if (metaResult.isSuccess) {
                    val work = metaResult.getOrNull()!!
                    val year = work.extractYear() ?: ""
                    val firstAuthor = work.extractFirstAuthorOnly()
                    val authors = work.extractAuthorsFormatted()
                    val title = work.extractPrimaryTitle()
                    val journal = work.extractJournal()

                    val authorForRename = if (format.useEtAl) authors else firstAuthor
                    val suggestedName = format.formatFileName(year, authorForRename, title)

                    val isAlreadyRenamed = item.name.equals(suggestedName, ignoreCase = true)

                    paper = paper.copy(
                        publicationYear = year,
                        firstAuthor = firstAuthor,
                        authorsFormatted = authors,
                        title = title,
                        journal = journal,
                        suggestedFileName = suggestedName,
                        status = if (isAlreadyRenamed) PaperStatus.RENAMED else PaperStatus.READY_TO_RENAME,
                        errorMessage = null
                    )
                } else {
                    paper = paper.copy(
                        status = PaperStatus.ERROR,
                        errorMessage = metaResult.exceptionOrNull()?.message ?: "CrossRef検索に失敗しました"
                    )
                }
            } else {
                paper = paper.copy(
                    status = PaperStatus.NO_DOI,
                    errorMessage = "PDFからDOIを検出できませんでした"
                )
            }

            val savedId = paperDao.insertPaper(paper)
            results.add(paper.copy(id = if (paper.id != 0L) paper.id else savedId))
        }

        results
    }

    /**
     * Resolves metadata for a specific paper with a user-supplied or detected DOI
     */
    suspend fun resolveDoiForPaper(
        paper: PaperEntity,
        doi: String,
        format: RenameFormat
    ): Result<PaperEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanDoi = CrossRefClient.cleanDoi(doi)
            if (cleanDoi.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("有効なDOIを入力してください"))
            }

            val metaResult = CrossRefClient.fetchWorkByDoi(cleanDoi)
            if (metaResult.isSuccess) {
                val work = metaResult.getOrNull()!!
                val year = work.extractYear() ?: ""
                val firstAuthor = work.extractFirstAuthorOnly()
                val authors = work.extractAuthorsFormatted()
                val title = work.extractPrimaryTitle()
                val journal = work.extractJournal()

                val authorForRename = if (format.useEtAl) authors else firstAuthor
                val suggestedName = format.formatFileName(year, authorForRename, title)

                val updated = paper.copy(
                    doi = cleanDoi,
                    publicationYear = year,
                    firstAuthor = firstAuthor,
                    authorsFormatted = authors,
                    title = title,
                    journal = journal,
                    suggestedFileName = suggestedName,
                    status = PaperStatus.READY_TO_RENAME,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
                paperDao.updatePaper(updated)
                Result.success(updated)
            } else {
                val err = metaResult.exceptionOrNull()?.message ?: "CrossRef検索に失敗しました"
                val updated = paper.copy(
                    doi = cleanDoi,
                    status = PaperStatus.ERROR,
                    errorMessage = err,
                    updatedAt = System.currentTimeMillis()
                )
                paperDao.updatePaper(updated)
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Renames a paper file using SAF or local file system
     */
    suspend fun renamePaper(
        context: Context,
        paper: PaperEntity,
        targetFileName: String? = null
    ): Result<PaperEntity> = withContext(Dispatchers.IO) {
        try {
            val newName = (targetFileName ?: paper.suggestedFileName).trim()
            if (newName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("リネーム後のファイル名が指定されていません"))
            }

            val uri = Uri.parse(paper.uriString)
            val updatedUriString: String

            if (uri.scheme == "file") {
                val currentFile = File(uri.path ?: "")
                if (!currentFile.exists()) {
                    return@withContext Result.failure(Exception("ファイルが見つかりません: ${currentFile.path}"))
                }
                val parent = currentFile.parentFile ?: return@withContext Result.failure(Exception("親フォルダが見つかりません"))
                val targetFile = File(parent, newName)
                val success = currentFile.renameTo(targetFile)
                if (!success) {
                    return@withContext Result.failure(Exception("ファイルのリネームに失敗しました"))
                }
                updatedUriString = Uri.fromFile(targetFile).toString()
            } else {
                // SAF DocumentFile
                val doc = DocumentFile.fromSingleUri(context, uri)
                    ?: return@withContext Result.failure(Exception("対象ファイルを開けませんでした"))
                val success = doc.renameTo(newName)
                if (!success) {
                    return@withContext Result.failure(Exception("Storage Access Frameworkでのリネームに失敗しました。書き込み権限を確認してください。"))
                }
                updatedUriString = doc.uri.toString()
            }

            val updated = paper.copy(
                currentFileName = newName,
                uriString = updatedUriString,
                status = PaperStatus.RENAMED,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            paperDao.updatePaper(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reverts renamed file back to original file name
     */
    suspend fun revertRename(
        context: Context,
        paper: PaperEntity
    ): Result<PaperEntity> = withContext(Dispatchers.IO) {
        try {
            val originalName = paper.originalFileName
            if (paper.currentFileName == originalName) {
                val updated = paper.copy(status = PaperStatus.READY_TO_RENAME)
                paperDao.updatePaper(updated)
                return@withContext Result.success(updated)
            }

            val uri = Uri.parse(paper.uriString)
            val updatedUriString: String

            if (uri.scheme == "file") {
                val currentFile = File(uri.path ?: "")
                val parent = currentFile.parentFile ?: return@withContext Result.failure(Exception("親フォルダが見つかりません"))
                val targetFile = File(parent, originalName)
                val success = currentFile.renameTo(targetFile)
                if (!success) {
                    return@withContext Result.failure(Exception("リネームの復元に失敗しました"))
                }
                updatedUriString = Uri.fromFile(targetFile).toString()
            } else {
                val doc = DocumentFile.fromSingleUri(context, uri)
                    ?: return@withContext Result.failure(Exception("対象ファイルを開けませんでした"))
                val success = doc.renameTo(originalName)
                if (!success) {
                    return@withContext Result.failure(Exception("SAFでの復元に失敗しました"))
                }
                updatedUriString = doc.uri.toString()
            }

            val updated = paper.copy(
                currentFileName = originalName,
                uriString = updatedUriString,
                status = PaperStatus.READY_TO_RENAME,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            paperDao.updatePaper(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateManualMetadata(
        paper: PaperEntity,
        year: String,
        author: String,
        title: String,
        format: RenameFormat
    ): PaperEntity = withContext(Dispatchers.IO) {
        val suggested = format.formatFileName(year, author, title)
        val updated = paper.copy(
            publicationYear = year,
            firstAuthor = author,
            authorsFormatted = author,
            title = title,
            suggestedFileName = suggested,
            status = PaperStatus.READY_TO_RENAME,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        paperDao.updatePaper(updated)
        updated
    }

    suspend fun deletePaper(paper: PaperEntity) = withContext(Dispatchers.IO) {
        paperDao.deletePaperById(paper.id)
    }

    suspend fun clearFolderPapers(folderUri: String) = withContext(Dispatchers.IO) {
        paperDao.deletePapersByFolder(folderUri)
    }
}

private data class PdfFileInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isLocal: Boolean,
    val file: File? = null,
    val documentFile: DocumentFile? = null
)
