package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusRenamed
import com.example.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperListScreen(
    viewModel: PaperViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val papers by viewModel.papers.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showHelpDialog by remember { mutableStateOf(false) }

    // SAF folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelected(context, uri)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "文献リネーマー",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setFormatDialogVisible(true) },
                        modifier = Modifier.testTag("format_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "リネーム形式設定",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("help_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "使い方ヘルプ",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = stats.readyCount > 0 && !uiState.isScanning && !uiState.isBatchRenaming,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.batchRename() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text("一括リネーム (${stats.readyCount}件)")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("batch_rename_fab")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Folder Selector Header Card
            FolderHeaderCard(
                folderName = uiState.folderDisplayName,
                isScanning = uiState.isScanning,
                onSelectFolder = { folderPickerLauncher.launch(null) },
                onLoadDemo = { viewModel.createAndLoadDemoPapers() },
                onRescan = { viewModel.scanCurrentFolder() },
                formatLabel = uiState.renameFormat.label
            )

            // Summary Stats Row
            StatsRow(
                stats = stats,
                activeFilter = uiState.activeFilter,
                onSelectFilter = { viewModel.setFilterTab(it) }
            )

            // Search Bar & Filter Chips
            SearchAndFilterSection(
                searchQuery = uiState.searchQuery,
                onSearchChanged = { viewModel.setSearchQuery(it) },
                activeFilter = uiState.activeFilter,
                onFilterChanged = { viewModel.setFilterTab(it) }
            )

            // Paper List
            if (papers.isEmpty()) {
                EmptyStateView(
                    isScanning = uiState.isScanning,
                    hasFolder = uiState.folderUri != null,
                    searchQuery = uiState.searchQuery,
                    onSelectFolder = { folderPickerLauncher.launch(null) },
                    onLoadDemo = { viewModel.createAndLoadDemoPapers() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("paper_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = papers,
                        key = { it.id }
                    ) { paper ->
                        PaperCard(
                            paper = paper,
                            onRename = { viewModel.renameSingle(it) },
                            onRevert = { viewModel.revertSingle(it) },
                            onEdit = { viewModel.openEditDialog(it) }
                        )
                    }
                }
            }
        }
    }

    // Edit Paper Dialog
    uiState.editingPaper?.let { paper ->
        EditPaperDialog(
            paper = paper,
            format = uiState.renameFormat,
            onDismiss = { viewModel.dismissEditDialog() },
            onResolveDoi = { doi -> viewModel.resolveDoi(paper, doi) },
            onSaveManual = { year, author, title ->
                viewModel.updateManualMetadata(paper, year, author, title)
            }
        )
    }

    // Format Settings Dialog
    if (uiState.showFormatDialog) {
        FormatSettingsDialog(
            currentFormat = uiState.renameFormat,
            onDismiss = { viewModel.setFormatDialogVisible(false) },
            onSelectFormat = { newFormat ->
                viewModel.updateRenameFormat(newFormat)
            }
        )
    }

    // Scanning progress dialog
    if (uiState.isScanning) {
        BatchProgressDialog(
            title = "論文フォルダを解析中",
            current = uiState.scanProgress.current,
            total = uiState.scanProgress.total,
            currentFileName = uiState.scanProgress.currentFileName
        )
    }

    // Batch Renaming progress dialog
    if (uiState.isBatchRenaming) {
        BatchProgressDialog(
            title = "一括リネームを実行中",
            current = uiState.batchProgress.current,
            total = uiState.batchProgress.total,
            currentFileName = uiState.batchProgress.currentFileName
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun FolderHeaderCard(
    folderName: String,
    isScanning: Boolean,
    onSelectFolder: () -> Unit,
    onLoadDemo: () -> Unit,
    onRescan: () -> Unit,
    formatLabel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("folder_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "対象フォルダ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onRescan,
                    enabled = !isScanning,
                    modifier = Modifier.testTag("rescan_btn")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "再読み込み",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectFolder,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_folder_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("フォルダ選択", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onLoadDemo,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("demo_papers_btn"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("デモ論文作成", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    stats: PaperStats,
    activeFilter: FilterTab,
    onSelectFilter: (FilterTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBadge(
            label = "全PDF",
            count = stats.totalCount,
            isSelected = activeFilter == FilterTab.ALL,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(FilterTab.ALL) }
        )

        StatBadge(
            label = "リネーム待ち",
            count = stats.readyCount,
            isSelected = activeFilter == FilterTab.READY,
            color = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(FilterTab.READY) }
        )

        StatBadge(
            label = "完了",
            count = stats.renamedCount,
            isSelected = activeFilter == FilterTab.RENAMED,
            color = StatusRenamed,
            containerColor = StatusRenamed.copy(alpha = 0.15f),
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(FilterTab.RENAMED) }
        )

        StatBadge(
            label = "DOI未検出",
            count = stats.needsDoiCount,
            isSelected = activeFilter == FilterTab.NEEDS_DOI,
            color = StatusWarning,
            containerColor = StatusWarning.copy(alpha = 0.15f),
            modifier = Modifier.weight(1f),
            onClick = { onSelectFilter(FilterTab.NEEDS_DOI) }
        )
    }
}

@Composable
fun StatBadge(
    label: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) containerColor else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    activeFilter: FilterTab,
    onFilterChanged: (FilterTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_papers_input"),
            placeholder = { Text("タイトル、著者、DOIで検索...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "クリア",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTab.entries.forEach { tab ->
                val isSelected = activeFilter == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChanged(tab) },
                    label = { Text(tab.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_chip_${tab.name}")
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isScanning: Boolean,
    hasFolder: Boolean,
    searchQuery: String,
    onSelectFolder: () -> Unit,
    onLoadDemo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "一致する論文が見つかりません"
                } else if (!hasFolder) {
                    "論文フォルダを選択してください"
                } else {
                    "フォルダ内にPDFが見つかりません"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "検索キーワード「$searchQuery」を変更して再度お試しください。"
                } else {
                    "PDF論文が入ったフォルダを選択すると、自動でDOIを抽出し「出版年_著者_タイトル.pdf」にリネームします。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSelectFolder,
                    modifier = Modifier.testTag("empty_select_folder_btn")
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("フォルダを選択")
                }

                OutlinedButton(
                    onClick = onLoadDemo,
                    modifier = Modifier.testTag("empty_demo_btn")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("デモ論文を試す")
                }
            }
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "文献リネーマーの使い方",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column {
                HelpItem(
                    step = "1",
                    title = "フォルダを選択",
                    desc = "論文PDFが保存されている端末のフォルダへのアクセスを許可します。"
                )
                Spacer(modifier = Modifier.height(10.dp))
                HelpItem(
                    step = "2",
                    title = "DOIからメタデータを自動取得",
                    desc = "PDF内のテキストからDOI (10.xxxx/...) を自動抽出し、CrossRef公式APIから「出版年」「著者」「タイトル」を取得します。"
                )
                Spacer(modifier = Modifier.height(10.dp))
                HelpItem(
                    step = "3",
                    title = "ワンタップでリネーム",
                    desc = "「一括リネーム」ボタンを押すだけで、全論文が整理されたファイル名に変更されます。必要に応じていつでも元の名前に戻せます。"
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

@Composable
fun HelpItem(step: String, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
