package de.graetz.electronote.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.graetz.electronote.data.NotebookDocument
import de.graetz.electronote.data.NotebookDocumentSummary
import de.graetz.electronote.data.NotebookStore
import de.graetz.electronote.diagram.DiagramDocument
import de.graetz.electronote.diagram.DiagramStore
import de.graetz.electronote.diagram.DiagramSummary
import de.graetz.electronote.nextcloud.NextcloudDownloadDialog
import de.graetz.electronote.nextcloud.NextcloudLoginDialog
import de.graetz.electronote.pdf.PdfImporter
import de.graetz.electronote.ui.theme.IosColors
import de.graetz.electronote.ui.theme.IosDocumentBadge
import de.graetz.electronote.ui.theme.IosTagChipsRow
import de.graetz.electronote.ui.theme.formatRelativeDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class BrowserItem(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val tags: List<String>,
    val isFavorite: Boolean,
    val docTypeKey: String
) {
    class Notebook(val summary: NotebookDocumentSummary) :
        BrowserItem(summary.id, summary.name, summary.updatedAt, summary.tags, summary.isFavorite, summary.docType)

    class Diagram(val summary: DiagramSummary) :
        BrowserItem(summary.id, summary.name, summary.updatedAt, summary.tags, summary.isFavorite, summary.type)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    onOpenDocument: (String) -> Unit,
    onOpenDiagram: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var documents by remember { mutableStateOf(listOf<NotebookDocumentSummary>()) }
    var diagrams by remember { mutableStateOf(listOf<DiagramSummary>()) }
    var refreshKey by remember { mutableStateOf(0) }

    var showNextcloudLogin by remember { mutableStateOf(false) }
    var showNextcloudDownload by remember { mutableStateOf(false) }
    var showDriveLogin by remember { mutableStateOf(false) }
    var showDriveDownload by remember { mutableStateOf(false) }

    var showNewMenu by remember { mutableStateOf(false) }
    var showCloudMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var editingTagsFor by remember { mutableStateOf<BrowserItem?>(null) }
    var itemToRename by remember { mutableStateOf<BrowserItem?>(null) }
    var isImportingPdf by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        documents = NotebookStore.listDocuments(context)
        diagrams = DiagramStore.listDiagrams(context)
    }

    fun queryPdfDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)?.removeSuffix(".pdf") else null
        }
    }

    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImportingPdf = true
        scope.launch {
            val name = withContext(Dispatchers.IO) { queryPdfDisplayName(uri) } ?: "PDF-Notizbuch"
            val doc = withContext(Dispatchers.IO) { NotebookStore.createDocument(context, name) }
            val newBackgrounds = withContext(Dispatchers.IO) { PdfImporter.importPdf(context, uri, doc) }
            if (newBackgrounds.isNotEmpty()) {
                doc.backgrounds.addAll(newBackgrounds)
                val bottom = newBackgrounds.maxOf { it.yOffsetPx + it.heightPx } + 200
                doc.canvasHeightPx = maxOf(doc.canvasHeightPx, bottom)
                withContext(Dispatchers.IO) { NotebookStore.saveDocument(context, doc) }
            }
            isImportingPdf = false
            onOpenDocument(doc.id)
        }
    }

    val allTags = (documents.flatMap { it.tags } + diagrams.flatMap { it.tags }).distinct().sorted()

    val allBrowserItems: List<BrowserItem> = remember(documents, diagrams, selectedTag, searchQuery) {
        val nb = documents.map { BrowserItem.Notebook(it) }
        val dg = diagrams.map { BrowserItem.Diagram(it) }
        (nb + dg)
            .filter { selectedTag == null || selectedTag in it.tags }
            .filter {
                if (searchQuery.isBlank()) true
                else it.name.contains(searchQuery, ignoreCase = true) || it.tags.any { t -> t.contains(searchQuery, ignoreCase = true) }
            }
            .sortedByDescending { it.updatedAt }
    }

    val favoriteItems = remember(allBrowserItems) { allBrowserItems.filter { it.isFavorite } }
    val regularItems = remember(allBrowserItems) { allBrowserItems.filter { !it.isFavorite } }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Main top navigation row aligned with iPadOS toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ElectroNote",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Grouped Action 1: New Document (+)
                    Box {
                        IconButton(onClick = { showNewMenu = true }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Neu", tint = IosColors.Blue)
                        }
                        DropdownMenu(
                            expanded = showNewMenu,
                            onDismissRequest = { showNewMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Neues Dokument…") },
                                leadingIcon = { Icon(Icons.Outlined.NoteAdd, contentDescription = null, tint = IosColors.Orange) },
                                onClick = {
                                    showNewMenu = false
                                    showTypePicker = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("PDF öffnen & markieren") },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = IosColors.Red) },
                                onClick = {
                                    showNewMenu = false
                                    openPdfLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }
                    }

                    // Grouped Action 2: Cloud Sync Menu
                    Box {
                        IconButton(onClick = { showCloudMenu = true }) {
                            Icon(Icons.Outlined.CloudSync, contentDescription = "Cloud-Synchronisation")
                        }
                        DropdownMenu(
                            expanded = showCloudMenu,
                            onDismissRequest = { showCloudMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Von Nextcloud laden…") },
                                leadingIcon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = IosColors.Blue) },
                                onClick = {
                                    showCloudMenu = false
                                    showNextcloudDownload = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Nextcloud verbinden…") },
                                leadingIcon = { Icon(Icons.Outlined.CloudQueue, contentDescription = null) },
                                onClick = {
                                    showCloudMenu = false
                                    showNextcloudLogin = true
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Von Google Drive laden…") },
                                leadingIcon = { Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = IosColors.Green) },
                                onClick = {
                                    showCloudMenu = false
                                    showDriveDownload = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Google Drive verbinden…") },
                                leadingIcon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
                                onClick = {
                                    showCloudMenu = false
                                    showDriveLogin = true
                                }
                            )
                        }
                    }

                    // Grouped Action 3: Overflow Options (...)
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Optionen")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("In allen Inhalten & Handschriften suchen…") },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSearch()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Papierkorb") },
                                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenTrash()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (AppPreferences.isDarkMode) "Heller Modus" else "Dunkelmodus") },
                                leadingIcon = {
                                    Icon(
                                        if (AppPreferences.isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    AppPreferences.toggleDarkMode(context)
                                }
                            )
                        }
                    }
                }

                // Clean iOS-style search bar right beneath the header
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Notizen, Handschrift & Dokumente…",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Clear,
                                    contentDescription = "Löschen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tag filter capsules
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAllActive = selectedTag == null
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isAllActive) IosColors.Blue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { selectedTag = null }
                    ) {
                        Text(
                            text = "Alle",
                            fontSize = 12.sp,
                            fontWeight = if (isAllActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    for (tag in allTags) {
                        val active = selectedTag == tag
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) IosColors.Blue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { selectedTag = if (active) null else tag }
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (allBrowserItems.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (documents.isEmpty() && diagrams.isEmpty()) "Noch keine Dokumente"
                            else "Keine Treffer für diese Auswahl",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tippe oben rechts auf + um ein Dokument anzulegen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Section 1: Favoriten (like BrowserSidebarView.swift)
                    if (favoriteItems.isNotEmpty() && searchQuery.isBlank() && selectedTag == null) {
                        item {
                            Text(
                                text = "FAVORITEN",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(favoriteItems, key = { "fav_" + it.id }) { item ->
                            DocumentItemRow(
                                item = item,
                                onOpen = {
                                    focusManager.clearFocus()
                                    when (item) {
                                        is BrowserItem.Notebook -> onOpenDocument(item.id)
                                        is BrowserItem.Diagram -> onOpenDiagram(item.id)
                                    }
                                },
                                onToggleFavorite = {
                                    when (item) {
                                        is BrowserItem.Notebook -> NotebookStore.setFavorite(context, item.id, !item.isFavorite)
                                        is BrowserItem.Diagram -> DiagramStore.setFavorite(context, item.id, !item.isFavorite)
                                    }
                                    refreshKey++
                                },
                                onRename = { itemToRename = item },
                                onEditTags = { editingTagsFor = item },
                                onDelete = {
                                    when (item) {
                                        is BrowserItem.Notebook -> NotebookStore.moveToTrash(context, item.id)
                                        is BrowserItem.Diagram -> DiagramStore.moveToTrash(context, item.id)
                                    }
                                    refreshKey++
                                }
                            )
                        }
                        item {
                            Text(
                                text = "ALLE DOKUMENTE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // Section 2: Regular / Filtered items
                    val itemsToDisplay = if (favoriteItems.isNotEmpty() && searchQuery.isBlank() && selectedTag == null) regularItems else allBrowserItems
                    items(itemsToDisplay, key = { it.id }) { item ->
                        DocumentItemRow(
                            item = item,
                            onOpen = {
                                focusManager.clearFocus()
                                when (item) {
                                    is BrowserItem.Notebook -> onOpenDocument(item.id)
                                    is BrowserItem.Diagram -> onOpenDiagram(item.id)
                                }
                            },
                            onToggleFavorite = {
                                when (item) {
                                    is BrowserItem.Notebook -> NotebookStore.setFavorite(context, item.id, !item.isFavorite)
                                    is BrowserItem.Diagram -> DiagramStore.setFavorite(context, item.id, !item.isFavorite)
                                }
                                refreshKey++
                            },
                            onRename = { itemToRename = item },
                            onEditTags = { editingTagsFor = item },
                            onDelete = {
                                when (item) {
                                    is BrowserItem.Notebook -> NotebookStore.moveToTrash(context, item.id)
                                    is BrowserItem.Diagram -> DiagramStore.moveToTrash(context, item.id)
                                }
                                refreshKey++
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal DocumentTypePicker matching iPadOS DocumentTypePickerView.swift
    if (showTypePicker) {
        DocumentTypePickerDialog(
            onDismiss = { showTypePicker = false },
            onPickNotebook = {
                showTypePicker = false
                val count = documents.size + 1
                val doc = NotebookStore.createDocument(context, "Notizbuch $count")
                refreshKey++
                onOpenDocument(doc.id)
            },
            onPickWhiteboard = {
                showTypePicker = false
                val count = documents.count { it.docType == NotebookDocument.DOC_TYPE_WHITEBOARD } + 1
                val doc = NotebookStore.createDocument(context, "Whiteboard $count", NotebookDocument.DOC_TYPE_WHITEBOARD)
                refreshKey++
                onOpenDocument(doc.id)
            },
            onPickPap = {
                showTypePicker = false
                val count = diagrams.count { it.type == DiagramDocument.TYPE_PAP } + 1
                val doc = DiagramStore.createDiagram(context, "Ablaufplan $count", DiagramDocument.TYPE_PAP)
                refreshKey++
                onOpenDiagram(doc.id)
            },
            onPickMindMap = {
                showTypePicker = false
                val count = diagrams.count { it.type == DiagramDocument.TYPE_MINDMAP } + 1
                val doc = DiagramStore.createDiagram(context, "MindMap $count", DiagramDocument.TYPE_MINDMAP)
                refreshKey++
                onOpenDiagram(doc.id)
            }
        )
    }

    // Rename Dialog
    itemToRename?.let { item ->
        var newName by remember(item.id) { mutableStateOf(item.name) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Dokument umbenennen") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newName.trim()
                    if (trimmed.isNotEmpty()) {
                        when (item) {
                            is BrowserItem.Notebook -> NotebookStore.renameDocument(context, item.id, trimmed)
                            is BrowserItem.Diagram -> DiagramStore.renameDiagram(context, item.id, trimmed)
                        }
                        refreshKey++
                    }
                    itemToRename = null
                }) { Text("Umbenennen") }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) { Text("Abbrechen") }
            }
        )
    }

    // Edit Tags Dialog
    editingTagsFor?.let { item ->
        var input by remember(item.id) { mutableStateOf(item.tags.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { editingTagsFor = null },
            title = { Text("Tags bearbeiten") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("z.B. Klasse10, Wechselstrom") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val tags = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    when (item) {
                        is BrowserItem.Notebook -> NotebookStore.setTags(context, item.id, tags)
                        is BrowserItem.Diagram -> DiagramStore.setTags(context, item.id, tags)
                    }
                    editingTagsFor = null
                    refreshKey++
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { editingTagsFor = null }) { Text("Abbrechen") }
            }
        )
    }

    if (showNextcloudLogin) {
        NextcloudLoginDialog(onDismiss = { showNextcloudLogin = false })
    }
    if (showNextcloudDownload) {
        NextcloudDownloadDialog(
            onDismiss = { showNextcloudDownload = false },
            onDownloaded = { refreshKey++ },
            onDownloadedDiagram = { refreshKey++ }
        )
    }
    if (showDriveLogin) {
        de.graetz.electronote.drive.GoogleDriveLoginDialog(onDismiss = { showDriveLogin = false })
    }
    if (showDriveDownload) {
        de.graetz.electronote.drive.GoogleDriveDownloadDialog(
            onDismiss = { showDriveDownload = false },
            onDownloaded = { refreshKey++ }
        )
    }
}

/**
 * Modern document card matching BrowserRowView.swift with continuous 38dp rounded badge,
 * clean typography, relative timestamp, tag capsules, favorite star, and context menu.
 */
@Composable
private fun DocumentItemRow(
    item: BrowserItem,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // iOS-style solid rounded badge with centered icon & top-right favorite star
            IosDocumentBadge(
                docType = item.docTypeKey,
                isFavorite = item.isFavorite,
                size = 38.dp
            )

            // Document info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = formatRelativeDate(item.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.tags.isNotEmpty()) {
                    IosTagChipsRow(
                        tags = item.tags,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Quick favorite star toggle
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (item.isFavorite) "Favorit entfernen" else "Zu Favoriten hinzufügen",
                    tint = if (item.isFavorite) IosColors.Yellow else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // More options overflow menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Optionen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Umbenennen") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen") },
                        leadingIcon = {
                            Icon(
                                if (item.isFavorite) Icons.Outlined.StarBorder else Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (item.isFavorite) MaterialTheme.colorScheme.onSurfaceVariant else IosColors.Yellow
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Tags bearbeiten…") },
                        leadingIcon = { Icon(Icons.Outlined.Label, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEditTags()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("In den Papierkorb", color = IosColors.Red) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = IosColors.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Modal DocumentTypePicker matching iPadOS DocumentTypePickerView.swift with 2x2 grid of cards.
 */
@Composable
private fun DocumentTypePickerDialog(
    onDismiss: () -> Unit,
    onPickNotebook: () -> Unit,
    onPickPap: () -> Unit,
    onPickWhiteboard: () -> Unit,
    onPickMindMap: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Dokumenttyp wählen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TypeCard(
                        icon = Icons.Outlined.Description,
                        color = IosColors.Orange,
                        title = "Notizbuch",
                        description = "Endlos langer Zettel zum Schreiben und Zeichnen",
                        modifier = Modifier.weight(1f),
                        onClick = onPickNotebook
                    )
                    TypeCard(
                        icon = Icons.Outlined.AccountTree,
                        color = IosColors.Blue,
                        title = "Ablaufplan (PAP)",
                        description = "Programmablaufplan mit Knoten und Verbindungen",
                        modifier = Modifier.weight(1f),
                        onClick = onPickPap
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TypeCard(
                        icon = Icons.Outlined.Draw,
                        color = IosColors.Green,
                        title = "Whiteboard",
                        description = "Freie Zeichenfläche, beliebig zoombar",
                        modifier = Modifier.weight(1f),
                        onClick = onPickWhiteboard
                    )
                    TypeCard(
                        icon = Icons.Outlined.Hub,
                        color = IosColors.Purple,
                        title = "MindMap",
                        description = "Gedankenkarte mit Ästen und Notizen",
                        modifier = Modifier.weight(1f),
                        onClick = onPickMindMap
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun TypeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
