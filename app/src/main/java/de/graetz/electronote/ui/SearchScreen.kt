package de.graetz.electronote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.graetz.electronote.data.NotebookDocumentSummary
import de.graetz.electronote.data.NotebookStore
import de.graetz.electronote.diagram.DiagramStore
import de.graetz.electronote.diagram.DiagramSummary
import de.graetz.electronote.ui.theme.IosDocumentBadge
import de.graetz.electronote.ui.theme.IosTagChipsRow
import de.graetz.electronote.ui.theme.formatRelativeDate

private sealed class SearchHit(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val tags: List<String>,
    val isFavorite: Boolean,
    val docTypeKey: String,
    val snippet: String,
    val isDiagram: Boolean
) {
    class NotebookHit(summary: NotebookDocumentSummary, snippet: String) :
        SearchHit(summary.id, summary.name, summary.updatedAt, summary.tags, summary.isFavorite, summary.docType, snippet, false)

    class DiagramHit(summary: DiagramSummary, snippet: String) :
        SearchHit(summary.id, summary.name, summary.updatedAt, summary.tags, summary.isFavorite, summary.type, snippet, true)
}

/**
 * Cross-document full-text search matching iPadOS search style.
 * Searches across document titles, tags, and OCR-indexed handwriting/text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onOpenDiagram: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var allDocs by remember { mutableStateOf(listOf<NotebookDocumentSummary>()) }
    var allDiagrams by remember { mutableStateOf(listOf<DiagramSummary>()) }

    LaunchedEffect(Unit) {
        allDocs = NotebookStore.listDocuments(context)
        allDiagrams = DiagramStore.listDiagrams(context)
    }

    val results: List<SearchHit> = remember(query, allDocs, allDiagrams) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val docHits = allDocs.filter { doc ->
                doc.name.contains(query, ignoreCase = true) ||
                    doc.tags.any { it.contains(query, ignoreCase = true) } ||
                    doc.searchText.contains(query, ignoreCase = true)
            }.map { SearchHit.NotebookHit(it, snippetFor(it.searchText, query)) }

            val diagHits = allDiagrams.filter { diag ->
                diag.name.contains(query, ignoreCase = true) ||
                    diag.tags.any { it.contains(query, ignoreCase = true) }
            }.map { SearchHit.DiagramHit(it, "") }

            (docHits + diagHits).sortedByDescending { it.updatedAt }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Zurück")
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Notizen, Handschrift & Dokumente…",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { query = "" },
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
        }
    ) { padding ->
        when {
            query.isBlank() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Notizen & Handschrift durchsuchen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Suche nach Titeln, Tags oder handgeschriebenem Text in allen Notizbüchern.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            results.isEmpty() -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp)
                ) {
                    Text(
                        "Keine Treffer für „$query“",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { (if (it.isDiagram) "d_" else "n_") + it.id }) { hit ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (hit.isDiagram) onOpenDiagram(hit.id) else onOpenDocument(hit.id)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            IosDocumentBadge(
                                docType = hit.docTypeKey,
                                isFavorite = hit.isFavorite,
                                size = 38.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    hit.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    formatRelativeDate(hit.updatedAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hit.tags.isNotEmpty()) {
                                    IosTagChipsRow(
                                        tags = hit.tags,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                if (hit.snippet.isNotBlank()) {
                                    Text(
                                        hit.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun snippetFor(text: String, query: String): String {
    val idx = text.indexOf(query, ignoreCase = true)
    if (idx < 0) return ""
    val start = maxOf(0, idx - 40)
    val end = minOf(text.length, idx + query.length + 40)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < text.length) "…" else ""
    return prefix + text.substring(start, end) + suffix
}
