package de.graetz.electronote.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.graetz.electronote.data.NotebookStore
import de.graetz.electronote.diagram.DiagramStore
import de.graetz.electronote.ui.theme.IosColors
import de.graetz.electronote.ui.theme.IosDocumentBadge
import de.graetz.electronote.ui.theme.formatRelativeDate

private sealed class TrashItem(val id: String, val name: String, val deletedAt: Long?, val docTypeKey: String) {
    class Notebook(val id0: String, name: String, deletedAt: Long?, docType: String) :
        TrashItem(id0, name, deletedAt, docType)

    class Diagram(val id0: String, name: String, deletedAt: Long?, type: String) :
        TrashItem(id0, name, deletedAt, type)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var trashed by remember { mutableStateOf(listOf<TrashItem>()) }
    var refreshKey by remember { mutableStateOf(0) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey) {
        val notebooks = NotebookStore.listTrash(context).map { TrashItem.Notebook(it.id, it.name, it.deletedAt, it.docType) }
        val diagrams = DiagramStore.listTrash(context).map { TrashItem.Diagram(it.id, it.name, it.deletedAt, it.type) }
        trashed = (notebooks + diagrams).sortedByDescending { it.deletedAt }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Zurück")
                }
                Text(
                    "Papierkorb",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                if (trashed.isNotEmpty()) {
                    TextButton(onClick = { showEmptyConfirm = true }) {
                        Text("Leeren", color = IosColors.Red, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        if (trashed.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Papierkorb ist leer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashed, key = { it.id }) { doc ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IosDocumentBadge(
                                docType = doc.docTypeKey,
                                isFavorite = false,
                                size = 38.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    doc.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Gelöscht: " + (doc.deletedAt?.let { formatRelativeDate(it) } ?: "Unbekannt"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                when (doc) {
                                    is TrashItem.Notebook -> NotebookStore.restoreFromTrash(context, doc.id)
                                    is TrashItem.Diagram -> DiagramStore.restoreFromTrash(context, doc.id)
                                }
                                refreshKey++
                            }) {
                                Icon(
                                    Icons.Outlined.Restore,
                                    contentDescription = "Wiederherstellen",
                                    tint = IosColors.Blue
                                )
                            }
                            IconButton(onClick = {
                                when (doc) {
                                    is TrashItem.Notebook -> NotebookStore.deleteDocument(context, doc.id)
                                    is TrashItem.Diagram -> DiagramStore.deleteDiagram(context, doc.id)
                                }
                                refreshKey++
                            }) {
                                Icon(
                                    Icons.Outlined.DeleteForever,
                                    contentDescription = "Endgültig löschen",
                                    tint = IosColors.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("Papierkorb leeren?") },
            text = { Text("Alle Elemente im Papierkorb werden unwiderruflich gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    NotebookStore.emptyTrash(context)
                    DiagramStore.emptyTrash(context)
                    showEmptyConfirm = false
                    refreshKey++
                }) { Text("Endgültig leeren", color = IosColors.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
}
