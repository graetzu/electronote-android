package de.graetz.electronote.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.graetz.electronote.data.NotebookDocument
import de.graetz.electronote.diagram.DiagramDocument
import java.text.DateFormat
import java.util.Date
import kotlin.math.absoluteValue

private val TAG_PALETTE = listOf(
    IosColors.Blue,
    IosColors.Green,
    IosColors.Orange,
    IosColors.Purple,
    IosColors.Red,
    IosColors.Teal
)

/**
 * Recreates iPadOS's iconic continuous rounded-rectangle badge (BrowserRowView.swift)
 * with solid background color and white centered icon for each document type,
 * plus an optional glowing gold favorite star badge on the top right.
 */
@Composable
fun IosDocumentBadge(
    docType: String,
    isFavorite: Boolean = false,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val (badgeColor, badgeIcon) = when (docType) {
        NotebookDocument.DOC_TYPE_WHITEBOARD, "whiteboard" -> Pair(IosColors.Green, Icons.Outlined.Draw)
        DiagramDocument.TYPE_PAP, "pap" -> Pair(IosColors.Blue, Icons.Outlined.AccountTree)
        DiagramDocument.TYPE_MINDMAP, "mindmap" -> Pair(IosColors.Purple, Icons.Outlined.Hub)
        "pdf" -> Pair(IosColors.Red, Icons.Outlined.PictureAsPdf)
        "folder" -> Pair(IosColors.Blue, Icons.Outlined.Folder)
        else -> Pair(IosColors.Orange, Icons.Outlined.Description)
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(9.dp))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                badgeIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.54f)
            )
        }

        if (isFavorite) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .padding(1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorit",
                    tint = IosColors.Yellow,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * An iOS-style pill tag chip with 15% opacity background and matching solid text color.
 */
@Composable
fun IosTagChip(
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val color = TAG_PALETTE[(tag.hashCode().absoluteValue) % TAG_PALETTE.size]
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .then(clickModifier)
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = tag,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Row of tag chips matching BrowserRowView.swift's tagChipsRow.
 */
@Composable
fun IosTagChipsRow(
    tags: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 4
) {
    if (tags.isEmpty()) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tags.take(maxVisible).forEachIndexed { index, tag ->
            if (index > 0) {
                Box(modifier = Modifier.size(4.dp))
            }
            IosTagChip(tag = tag)
        }
        if (tags.size > maxVisible) {
            Text(
                text = " +${tags.size - maxVisible}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Formats timestamps in iPadOS's clean relative style ("Gerade eben", "vor 5 Min.", "vor 2 Std.", "Gestern", ...).
 */
fun formatRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 0 -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(timestamp))
        diff < 60_000L -> "Gerade eben"
        diff < 3_600_000L -> "vor ${(diff / 60_000L).coerceAtLeast(1)} Min."
        diff < 86_400_000L -> "vor ${(diff / 3_600_000L).coerceAtLeast(1)} Std."
        diff < 172_800_000L -> "Gestern"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }
}
