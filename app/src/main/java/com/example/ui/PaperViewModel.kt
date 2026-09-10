package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PaperEntity
import com.example.data.local.PaperStatus
import com.example.data.model.RenameFormat
import com.example.data.repository.PaperRepository
import com.example.util.DemoPaperGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class FilterTab(val label: String) {
    ALL("すべて"),
    READY("リネーム待ち"),
    RENAMED("リネーム済"),
    NEEDS_DOI("DOI未検出")
}

data class ScanProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentFileName: String = ""
)

data class PaperUiState(
    val folderUri: Uri? = null,
    val folderDisplayName: String = "未選択",
    val searchQuery: String = "",
    val activeFilter: FilterTab = FilterTab.ALL,
    val renameFormat: RenameFormat = RenameFormat.defaultFormat(),
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress = ScanProgress(),
    val isBatchRenaming: Boolean = false,
    val batchProgress: ScanProgress = ScanProgress(),
    val editingPaper: PaperEntity? = null,
    val showFormatDialog: Boolean = false,
    val userMessage: String? = null
)

data class PaperStats(
    val totalCount: Int = 0,
    val readyCount: Int = 0,
    val renamedCount: Int = 0,
    val needsDoiCount: Int = 0
)

class PaperViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PaperRepository by lazy {
        val db = AppDatabase.getInstance(application)
        PaperRepository(db.paperDao())
    }

    private val _uiState = MutableStateFlow(PaperUiState())
    val uiState: StateFlow<PaperUiState> = _uiState.asStateFlow()

    // Base paper stream from DB
    private val rawPapers = repository.getAllPapers()

    // Combined filtered papers
    val papers: StateFlow<List<PaperEntity>> = combine(
        rawPapers,
        _uiState
    ) { list, state ->
        val folderFiltered = if (state.folderUri != null) {
            val folderStr = state.folderUri.toString()
            list.filter { it.folderUriString == folderStr }
        } else {
            list
        }

        val query = state.searchQuery.trim().lowercase()
        val searchFiltered = if (query.isNotEmpty()) {
            folderFiltered.filter { paper ->
                paper.title.lowercase().contains(query) ||
                        paper.firstAuthor.lowercase().contains(query) ||
                        paper.authorsFormatted.lowercase().contains(query) ||
                        paper.currentFileName.lowercase().contains(query) ||
                        paper.doi.lowercase().contains(query) ||
                        paper.publicationYear.contains(query)
            }
        } else {
            folderFiltered
        }

        when (state.activeFilter) {
            FilterTab.ALL -> searchFiltered
            FilterTab.READY -> searchFiltered.filter { it.status == PaperStatus.READY_TO_RENAME }
            FilterTab.RENAMED -> searchFiltered.filter { it.status == PaperStatus.RENAMED }
            FilterTab.NEEDS_DOI -> searchFiltered.filter { it.status == PaperStatus.NO_DOI || it.status == PaperStatus.ERROR }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary statistics
    val stats: StateFlow<PaperStats> = combine(
        rawPapers,
        _uiState
    ) { list, state ->
        val folderFiltered = if (state.folderUri != null) {
            val folderStr = state.folderUri.toString()
            list.filter { it.folderUriString == folderStr }
        } else {
            list
        }

        PaperStats(
            totalCount = folderFiltered.size,
            readyCount = folderFiltered.count { it.status == PaperStatus.READY_TO_RENAME },
            renamedCount = folderFiltered.count { it.status == PaperStatus.RENAMED },
            needsDoiCount = folderFiltered.count { it.status == PaperStatus.NO_DOI || it.status == PaperStatus.ERROR }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaperStats()
    )

    init {
        // Automatically check if demo folder has papers on initial launch
        val demoDir = File(application.filesDir, "sample_papers")
        if (demoDir.exists() && demoDir.listFiles()?.any { it.extension.equals("pdf", ignoreCase = true) } == true) {
            setFolder(Uri.fromFile(demoDir), "サンプル論文フォルダ (${demoDir.name})")
            scanCurrentFolder()
        }
    }

    fun setFolder(uri: Uri, displayName: String? = null) {
        val name = displayName ?: uri.lastPathSegment?.split(":")?.lastOrNull() ?: uri.lastPathSegment ?: "フォルダ"
        _uiState.value = _uiState.value.copy(
            folderUri = uri,
            folderDisplayName = name
        )
    }

    fun onFolderSelected(context: Context, treeUri: Uri) {
        try {
            // Persist SAF permission so we can read and write files inside the folder across launches
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Ignore if already persisted or not supported by provider
        }

        val doc = DocumentFile.fromTreeUri(context, treeUri)
        val folderName = doc?.name ?: treeUri.lastPathSegment ?: "選択フォルダ"
        setFolder(treeUri, folderName)
        scanCurrentFolder()
    }

    fun scanCurrentFolder() {
        val folderUri = _uiState.value.folderUri ?: return
        val context = getApplication<Application>()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanProgress = ScanProgress(0, 0, "フォルダ走査中...")
            )

            try {
                val scanned = repository.scanFolder(
                    context = context,
                    folderUri = folderUri,
                    format = _uiState.value.renameFormat,
                    onProgress = { current, total, name ->
                        _uiState.value = _uiState.value.copy(
                            scanProgress = ScanProgress(current, total, name)
                        )
                    }
                )
                val readyCount = scanned.count { it.status == PaperStatus.READY_TO_RENAME }
                val msg = "${scanned.size} 件のPDFを検出しました (リネーム候補: $readyCount 件)"
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    userMessage = msg
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    userMessage = "スキャン中にエラーが発生しました: ${e.message}"
                )
            }
        }
    }

    /**
     * Generates sample research papers with real DOIs in the internal storage
     * so that the user can immediately test the DOI extraction & renaming without setup
     */
    fun createAndLoadDemoPapers() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                scanProgress = ScanProgress(0, 3, "デモ論文PDFを生成中...")
            )

            val demoDir = File(context.filesDir, "sample_papers")
            DemoPaperGenerator.generateSamplePapersInDir(demoDir)

            setFolder(Uri.fromFile(demoDir), "デモ論文フォルダ (3論文)")
            scanCurrentFolder()
            _uiState.value = _uiState.value.copy(
                userMessage = "デモ用論文PDFを3件作成しました！「一括リネーム」を試せます。"
            )
        }
    }

    fun batchRename() {
        val context = getApplication<Application>()
        val currentPapers = papers.value
        val targets = currentPapers.filter { it.status == PaperStatus.READY_TO_RENAME }

        if (targets.isEmpty()) {
            _uiState.value = _uiState.value.copy(userMessage = "リネーム対象の論文がありません")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBatchRenaming = true,
                batchProgress = ScanProgress(0, targets.size, "")
            )

            var successCount = 0
            var failCount = 0

            targets.forEachIndexed { index, paper ->
                _uiState.value = _uiState.value.copy(
                    batchProgress = ScanProgress(index + 1, targets.size, paper.currentFileName)
                )

                val result = repository.renamePaper(context, paper)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failCount++
                }
            }

            _uiState.value = _uiState.value.copy(
                isBatchRenaming = false,
                userMessage = "一括リネーム完了: $successCount 件成功" + if (failCount > 0) ", $failCount 件失敗" else ""
            )
        }
    }

    fun renameSingle(paper: PaperEntity) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val res = repository.renamePaper(context, paper)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "「${res.getOrNull()?.currentFileName}」にリネームしました"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    userMessage = "リネーム失敗: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun revertSingle(paper: PaperEntity) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val res = repository.revertRename(context, paper)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "元のファイル名「${paper.originalFileName}」に復元しました"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    userMessage = "復元失敗: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun resolveDoi(paper: PaperEntity, doi: String) {
        viewModelScope.launch {
            val res = repository.resolveDoiForPaper(paper, doi, _uiState.value.renameFormat)
            if (res.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    userMessage = "メタデータを取得しました: ${res.getOrNull()?.title}",
                    editingPaper = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    userMessage = "DOI検索失敗: ${res.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun updateManualMetadata(paper: PaperEntity, year: String, author: String, title: String) {
        viewModelScope.launch {
            val updated = repository.updateManualMetadata(paper, year, author, title, _uiState.value.renameFormat)
            _uiState.value = _uiState.value.copy(
                userMessage = "メタデータを保存しました",
                editingPaper = null
            )
        }
    }

    fun openEditDialog(paper: PaperEntity) {
        _uiState.value = _uiState.value.copy(editingPaper = paper)
    }

    fun dismissEditDialog() {
        _uiState.value = _uiState.value.copy(editingPaper = null)
    }

    fun setFormatDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showFormatDialog = visible)
    }

    fun updateRenameFormat(newFormat: RenameFormat) {
        _uiState.value = _uiState.value.copy(
            renameFormat = newFormat,
            showFormatDialog = false
        )
        // Recalculate suggested names for ready papers
        scanCurrentFolder()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setFilterTab(tab: FilterTab) {
        _uiState.value = _uiState.value.copy(activeFilter = tab)
    }

    fun clearUserMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }
}
