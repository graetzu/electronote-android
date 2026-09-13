package de.graetz.electronote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.graetz.electronote.diagram.DEFAULT_BYPASS_DISTANCE_PX
import de.graetz.electronote.diagram.DiagramConnection
import de.graetz.electronote.diagram.DiagramDocument
import de.graetz.electronote.diagram.DiagramNode
import de.graetz.electronote.diagram.DiagramPort
import de.graetz.electronote.diagram.DiagramShapeKind
import de.graetz.electronote.diagram.DiagramStore
import de.graetz.electronote.diagram.MINDMAP_SHAPES
import de.graetz.electronote.diagram.PAP_SHAPES
import de.graetz.electronote.diagram.PapGrid
import de.graetz.electronote.diagram.PapTemplate
import de.graetz.electronote.diagram.PapTemplates
import de.graetz.electronote.diagram.arrowHeadPath
import de.graetz.electronote.diagram.buildPathWithCrossingJumps
import de.graetz.electronote.diagram.composeShapeFor
import de.graetz.electronote.diagram.hasSubroutineStripes
import de.graetz.electronote.diagram.mindMapCurvePath
import de.graetz.electronote.diagram.mindMapEndpoints
import de.graetz.electronote.diagram.papFillColor
import de.graetz.electronote.diagram.papStrokeColor
import de.graetz.electronote.diagram.routePapConnectionFull
import de.graetz.electronote.ui.theme.IosColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

private const val CANVAS_SIZE_DP = 2600

// Exact iOS MindMap palette (bubbleColors), in order.
private val MINDMAP_COLORS = listOf(
    IosColors.Blue, IosColors.Purple, IosColors.Teal, IosColors.Green,
    IosColors.Orange, IosColors.Pink, IosColors.Red
)

@Composable
fun DiagramScreen(diagramId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var diagramType by remember { mutableStateOf(DiagramDocument.TYPE_PAP) }
    var diagramName by remember { mutableStateOf("Diagramm") }
    var nodes by remember { mutableStateOf<List<NodeUiState>>(emptyList()) }
    var connections by remember { mutableStateOf<List<DiagramConnection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var pendingShape by remember { mutableStateOf<DiagramShapeKind?>(null) }
    var selectedColor by remember { mutableStateOf(MINDMAP_COLORS[0]) }
    var connectMode by remember { mutableStateOf(false) }
    var connectFromId by remember { mutableStateOf<String?>(null) }
    var editingNode by remember { mutableStateOf<NodeUiState?>(null) }
    var bypassDistancePx by remember { mutableStateOf(DEFAULT_BYPASS_DISTANCE_PX) }
    var showRoutingSettings by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    val hScroll = rememberScrollState()
    val vScroll = rememberScrollState()
    val isPap = diagramType == DiagramDocument.TYPE_PAP

    fun persist() {
        val doc = DiagramDocument(
            id = diagramId,
            name = diagramName,
            type = diagramType,
            nodes = nodes.map { it.toNode() }.toMutableList(),
            connections = connections.toMutableList(),
            bypassDistancePx = bypassDistancePx
        )
        scope.launch(Dispatchers.IO) { DiagramStore.saveDiagram(context, doc) }
    }

    LaunchedEffect(diagramId) {
        val loaded = withContext(Dispatchers.IO) { DiagramStore.loadDiagram(context, diagramId) }
        if (loaded != null) {
            diagramName = loaded.name
            diagramType = loaded.type
            nodes = loaded.nodes.map { NodeUiState(it) }
            connections = loaded.connections
            bypassDistancePx = loaded.bypassDistancePx
        } else if (isPap) {
            // First time opening PAP: load Tutorial 1 as default!
            val tpl = PapTemplates.load(PapTemplate.TUTORIAL_1)
            diagramName = tpl.name
            nodes = tpl.nodes.map { NodeUiState(it) }
            connections = tpl.connections
            persist()
        }
        isLoading = false
    }

    fun addNode(shape: DiagramShapeKind, tapX: Float, tapY: Float) {
        val node = if (isPap) {
            var (col, row) = PapGrid.nearestGrid(tapX, tapY)
            if (nodes.any { it.col == col && it.row == row }) {
                row = (nodes.filter { it.col == col }.mapNotNull { it.row }.maxOrNull() ?: (row - 1)) + 1
            }
            DiagramNode(
                x = PapGrid.centerX(col) - shape.widthPx / 2f,
                y = PapGrid.centerY(row) - shape.heightPx / 2f,
                shape = shape,
                col = col,
                row = row,
                tag = if (shape == DiagramShapeKind.IO) "E" else "",
                text = when (shape) {
                    DiagramShapeKind.START -> "Start"
                    DiagramShapeKind.END -> "Ende"
                    DiagramShapeKind.PROCESS -> "Vorgang"
                    DiagramShapeKind.IO -> "Eingabe"
                    DiagramShapeKind.DECISION -> "Bedingung?"
                    DiagramShapeKind.SUBROUTINE -> "Unterprogramm"
                    DiagramShapeKind.COMMENT -> "Kommentar"
                    else -> ""
                }
            )
        } else {
            DiagramNode(
                x = tapX - shape.widthPx / 2f,
                y = tapY - shape.heightPx / 2f,
                shape = shape,
                colorArgb = android.graphics.Color.argb(
                    (selectedColor.alpha * 255).roundToInt(),
                    (selectedColor.red * 255).roundToInt(),
                    (selectedColor.green * 255).roundToInt(),
                    (selectedColor.blue * 255).roundToInt()
                )
            )
        }
        val ui = NodeUiState(node)
        nodes = nodes + ui
        selectedNodeId = ui.id
        persist()
    }

    fun insertBelow(fromNode: NodeUiState, shape: DiagramShapeKind) {
        val targetCol = fromNode.col ?: 1
        val targetRow = (fromNode.row ?: 0) + 1

        // Shift down existing nodes in the same column at row >= targetRow
        nodes.forEach { n ->
            if (n.col == targetCol && (n.row ?: 0) >= targetRow) {
                val newR = (n.row ?: 0) + 1
                n.row = newR
                n.y = PapGrid.centerY(newR) - n.shape.heightPx / 2f
            }
        }

        val defaultText = when (shape) {
            DiagramShapeKind.START -> "Start"
            DiagramShapeKind.END -> "Ende"
            DiagramShapeKind.PROCESS -> "Vorgang"
            DiagramShapeKind.IO -> if (shape == DiagramShapeKind.IO) "Eingabe" else ""
            DiagramShapeKind.DECISION -> "Bedingung?"
            DiagramShapeKind.SUBROUTINE -> "Unterprogramm"
            DiagramShapeKind.COMMENT -> "Kommentar"
            else -> ""
        }

        val newNode = DiagramNode(
            x = PapGrid.centerX(targetCol) - shape.widthPx / 2f,
            y = PapGrid.centerY(targetRow) - shape.heightPx / 2f,
            shape = shape,
            text = defaultText,
            col = targetCol,
            row = targetRow,
            tag = if (shape == DiagramShapeKind.IO) "E" else ""
        )
        val ui = NodeUiState(newNode)
        nodes = nodes + ui

        val edgeLabel = if (fromNode.shape == DiagramShapeKind.DECISION) "ja" else ""
        connections = connections + DiagramConnection(
            fromNodeId = fromNode.id,
            toNodeId = newNode.id,
            label = edgeLabel,
            fromPort = DiagramPort.BOTTOM
        )
        selectedNodeId = newNode.id
        persist()
    }

    fun branchRight(fromNode: NodeUiState, shape: DiagramShapeKind = DiagramShapeKind.PROCESS) {
        val targetCol = (fromNode.col ?: 1) + 1
        val targetRow = fromNode.row ?: 0
        val occupied = nodes.any { it.col == targetCol && it.row == targetRow }
        val finalRow = if (occupied) {
            ((nodes.filter { it.col == targetCol }.mapNotNull { it.row }.maxOrNull() ?: targetRow) + 1)
        } else targetRow

        val newNode = DiagramNode(
            x = PapGrid.centerX(targetCol) - shape.widthPx / 2f,
            y = PapGrid.centerY(finalRow) - shape.heightPx / 2f,
            shape = shape,
            col = targetCol,
            row = finalRow,
            tag = if (shape == DiagramShapeKind.IO) "A" else "",
            text = if (shape == DiagramShapeKind.DECISION) "Bedingung?" else "Anweisung"
        )
        val ui = NodeUiState(newNode)
        nodes = nodes + ui

        connections = connections + DiagramConnection(
            fromNodeId = fromNode.id,
            toNodeId = newNode.id,
            label = "nein",
            fromPort = DiagramPort.RIGHT
        )
        selectedNodeId = newNode.id
        persist()
    }

    fun deleteNode(id: String) {
        nodes = nodes.filterNot { it.id == id }
        connections = connections.filterNot { it.fromNodeId == id || it.toNodeId == id }
        if (selectedNodeId == id) selectedNodeId = null
        persist()
    }

    fun inferPort(from: NodeUiState, to: NodeUiState): DiagramPort {
        val fromRow = from.row
        val toRow = to.row
        return if (isPap && fromRow != null && toRow != null && fromRow == toRow) {
            if (to.x >= from.x) DiagramPort.RIGHT else DiagramPort.LEFT
        } else {
            DiagramPort.BOTTOM
        }
    }

    fun defaultLabel(from: NodeUiState): String {
        if (!isPap || from.shape != DiagramShapeKind.DECISION) return ""
        val outgoingCount = connections.count { it.fromNodeId == from.id }
        return if (outgoingCount == 0) "ja" else "nein"
    }

    val palette = if (isPap) PAP_SHAPES else MINDMAP_SHAPES

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { persist(); onBack() }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Zurück", modifier = Modifier.size(20.dp))
                }
                Text(
                    diagramName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 6.dp)
                )
                if (isPap) {
                    IconButton(onClick = { showTemplatePicker = true }) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "Vorlagen", modifier = Modifier.size(20.dp))
                    }
                }
                IconButton(onClick = {
                    connectMode = !connectMode
                    connectFromId = null
                    if (connectMode) pendingShape = null
                }) {
                    Icon(
                        Icons.Outlined.Timeline,
                        contentDescription = "Verbinden",
                        tint = if (connectMode) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isPap) {
                    IconButton(onClick = { showRoutingSettings = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Verbindungsabstand", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        if (!isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Tool Palette
                    Column(
                        modifier = Modifier
                            .width(96.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (shape in palette) {
                            val isCommentPreview = isPap && shape == DiagramShapeKind.COMMENT
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (pendingShape == shape) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (isPap && selectedNodeId != null) {
                                            val sel = nodes.find { it.id == selectedNodeId }
                                            if (sel != null) {
                                                insertBelow(sel, shape)
                                                pendingShape = null
                                                return@clickable
                                            }
                                        }
                                        pendingShape = if (pendingShape == shape) null else shape
                                        connectMode = false
                                        connectFromId = null
                                    }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 28.dp)
                                        .clip(composeShapeFor(shape))
                                        .background(if (isPap) shape.papFillColor() else selectedColor)
                                        .then(
                                            if (isCommentPreview) {
                                                Modifier.drawBehind {
                                                    val hook = 6.dp.toPx()
                                                    val p = Path().apply {
                                                        moveTo(hook, 0f)
                                                        lineTo(0f, 0f)
                                                        lineTo(0f, size.height)
                                                        lineTo(hook, size.height)
                                                    }
                                                    drawPath(p, Color(0xFF00008B), style = Stroke(width = 2.dp.toPx()))
                                                }
                                            } else {
                                                Modifier.border(
                                                    1.2.dp,
                                                    if (isPap) shape.papStrokeColor() else MaterialTheme.colorScheme.outline,
                                                    composeShapeFor(shape)
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hasSubroutineStripes(shape)) {
                                        Box(Modifier.fillMaxHeight().width(1.5.dp).offset(x = 6.dp).background(Color(0xFF348E3E)))
                                        Box(Modifier.fillMaxHeight().width(1.5.dp).offset(x = (54 - 7.5).dp).background(Color(0xFF348E3E)))
                                    }
                                    if (shape == DiagramShapeKind.IO) {
                                        Text(
                                            "E",
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = Color(0xFFD94838),
                                            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 1.dp)
                                        )
                                    }
                                }
                                Text(shape.label, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        if (!isPap) {
                            Text("Farbe", fontSize = 9.sp, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                            for (color in MINDMAP_COLORS) {
                                Box(
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            if (selectedColor == color) 2.dp else 1.dp,
                                            if (selectedColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }

                    // Main Scrollable Canvas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(hScroll)
                            .verticalScroll(vScroll)
                    ) {
                        val density = LocalDensity.current
                        val densityFactor = density.density

                        Box(
                            modifier = Modifier
                                .size(width = CANVAS_SIZE_DP.dp, height = CANVAS_SIZE_DP.dp)
                                .pointerInputTap(pendingShape) { offset ->
                                    val shape = pendingShape
                                    if (shape != null) {
                                        addNode(shape, offset.x / densityFactor, offset.y / densityFactor)
                                        pendingShape = null
                                    } else {
                                        selectedNodeId = null
                                    }
                                }
                        ) {
                            if (isPap) {
                                // Column guides, matching PapDesigner
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    withTransform({
                                        scale(scaleX = densityFactor, scaleY = densityFactor, pivot = Offset.Zero)
                                    }) {
                                        for (col in 0..5) {
                                            val x = PapGrid.centerX(col)
                                            drawLine(Color(0x12000000), Offset(x, 0f), Offset(x, CANVAS_SIZE_DP.toFloat()), strokeWidth = 1f)
                                        }
                                    }
                                }

                                // Centered Diagram Title above Start Block (PapDesigner Style)
                                Text(
                                    text = diagramName.ifBlank { "Ablaufplan" },
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00008B),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                ((PapGrid.centerX(1) - 175f) * densityFactor).roundToInt(),
                                                (28f * densityFactor).roundToInt()
                                            )
                                        }
                                        .width(350.dp)
                                )
                            }

                            // Orthogonal Connection Routes
                            val papRoutes = if (isPap) {
                                connections.mapNotNull { conn ->
                                    val from = nodes.find { it.id == conn.fromNodeId } ?: return@mapNotNull null
                                    val to = nodes.find { it.id == conn.toNodeId } ?: return@mapNotNull null
                                    conn to routePapConnectionFull(from.toNode(), to.toNode(), conn.fromPort, bypassDistancePx)
                                }
                            } else emptyList()

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                withTransform({
                                    scale(scaleX = densityFactor, scaleY = densityFactor, pivot = Offset.Zero)
                                }) {
                                    if (isPap) {
                                        val priorPolylines = mutableListOf<List<Offset>>()
                                        for ((_, route) in papRoutes) {
                                            val path = buildPathWithCrossingJumps(route.points, priorPolylines)
                                            drawPath(path, Color(0xFF333333), style = Stroke(width = 2.2f))
                                            arrowHeadPath(route.points)?.let { drawPath(it, Color(0xFF333333)) }
                                            priorPolylines.add(route.points)
                                        }
                                    } else {
                                        for (conn in connections) {
                                            val from = nodes.find { it.id == conn.fromNodeId } ?: continue
                                            val to = nodes.find { it.id == conn.toNodeId } ?: continue
                                            val (start, end) = mindMapEndpoints(from.toNode(), to.toNode())
                                            val path = mindMapCurvePath(start, end)
                                            drawPath(path, Color(from.colorArgb).copy(alpha = 0.85f), style = Stroke(width = 3.0f))
                                        }
                                    }
                                }
                            }

                            // PAP Connection Labels (ja / nein / ...)
                            if (isPap) {
                                for ((conn, route) in papRoutes) {
                                    if (conn.label.isBlank()) continue
                                    Box(
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    (route.labelPos.x * densityFactor).roundToInt(),
                                                    (route.labelPos.y * densityFactor).roundToInt()
                                                )
                                            }
                                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            conn.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF333333)
                                        )
                                    }
                                }
                            }

                            // Nodes
                            for (node in nodes) {
                                DiagramNodeView(
                                    node = node,
                                    selected = selectedNodeId == node.id,
                                    isConnectSource = connectMode && connectFromId == node.id,
                                    connectMode = connectMode,
                                    snapToGrid = isPap,
                                    isPap = isPap,
                                    onTap = {
                                        if (connectMode) {
                                            val from = connectFromId
                                            if (from == null) {
                                                connectFromId = node.id
                                            } else if (from != node.id) {
                                                val fromNode = nodes.find { it.id == from }
                                                if (fromNode != null) {
                                                    connections = connections + DiagramConnection(
                                                        fromNodeId = from,
                                                        toNodeId = node.id,
                                                        label = defaultLabel(fromNode),
                                                        fromPort = inferPort(fromNode, node)
                                                    )
                                                }
                                                connectFromId = null
                                                persist()
                                            }
                                        } else {
                                            if (selectedNodeId == node.id) {
                                                editingNode = node
                                            } else {
                                                selectedNodeId = node.id
                                            }
                                        }
                                    },
                                    onDoubleTap = {
                                        editingNode = node
                                    },
                                    onMoved = { persist() }
                                )
                            }

                            // DIN Title Block (Schriftfeld) in Bottom-Right
                            if (isPap) {
                                val maxNodeY = nodes.maxOfOrNull { it.y + it.shape.heightPx } ?: 600f
                                val maxNodeX = nodes.maxOfOrNull { it.x + it.shape.widthPx } ?: 600f
                                val titleBlockY = max(maxNodeY + 50f, 740f)
                                val titleBlockX = max(maxNodeX - 280f, 380f)
                                DinTitleBlock(
                                    projectName = "Tutorial - Ablaufplan",
                                    author = "f.folkmann",
                                    diagramName = diagramName.ifBlank { "Ablaufplan" },
                                    createdDate = "14.01.07",
                                    modifiedDate = "18.04.20",
                                    modifier = Modifier.offset {
                                        IntOffset(
                                            (titleBlockX * densityFactor).roundToInt(),
                                            (titleBlockY * densityFactor).roundToInt()
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Action Bar on Selected Node
                selectedNodeId?.let { selId ->
                    val selNode = nodes.find { it.id == selId }
                    if (selNode != null && !connectMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    selNode.text.ifBlank { selNode.shape.label },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Button(
                                    onClick = { editingNode = selNode },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Text", fontSize = 11.sp)
                                }
                                if (isPap && selNode.shape == DiagramShapeKind.DECISION) {
                                    Button(
                                        onClick = { branchRight(selNode) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("+ Nein (Rechts)", fontSize = 11.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        deleteNode(selNode.id)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Löschen", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                                IconButton(onClick = { selectedNodeId = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Abwählen", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Node Edit Dialog
    editingNode?.let { node ->
        var input by remember(node.id) { mutableStateOf(node.text) }
        var tagInput by remember(node.id) { mutableStateOf(node.tag) }
        AlertDialog(
            onDismissRequest = { editingNode = null },
            title = { Text("Text & Eigenschaften bearbeiten") },
            text = {
                Column {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6
                    )
                    if (node.shape == DiagramShapeKind.IO) {
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { tagInput = if (tagInput == "E") "" else "E" }) {
                                Text(if (tagInput == "E") "✓ Eingabe (E)" else "Eingabe (E)")
                            }
                            TextButton(onClick = { tagInput = if (tagInput == "A") "" else "A" }) {
                                Text(if (tagInput == "A") "✓ Ausgabe (A)" else "Ausgabe (A)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    node.text = input
                    node.tag = tagInput
                    editingNode = null
                    persist()
                }) { Text("Speichern") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        deleteNode(node.id)
                        editingNode = null
                    }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { editingNode = null }) { Text("Abbrechen") }
                }
            }
        )
    }

    // Template Picker Dialog (PapDesigner Tutorial 1)
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("PapDesigner Vorlagen (Tutorial 1)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (tpl in PapTemplate.values()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val res = PapTemplates.load(tpl)
                                    diagramName = res.name
                                    nodes = res.nodes.map { NodeUiState(it) }
                                    connections = res.connections
                                    selectedNodeId = null
                                    persist()
                                    showTemplatePicker = false
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tpl.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(tpl.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Schließen") }
            }
        )
    }

    // Routing Settings Dialog
    if (showRoutingSettings) {
        AlertDialog(
            onDismissRequest = { showRoutingSettings = false },
            title = { Text("Verbindungsabstand") },
            text = {
                Column {
                    Text(
                        "Wie weit Verbindungen von Bausteinen ausweichen, bevor sie abbiegen. " +
                            "Größerer Abstand vermeidet Überschneidungen bei dicht stehenden Bausteinen.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "${bypassDistancePx.roundToInt()} px",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Slider(
                        value = bypassDistancePx,
                        onValueChange = { bypassDistancePx = it },
                        onValueChangeFinished = { persist() },
                        valueRange = 16f..80f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoutingSettings = false }) { Text("Fertig") }
            }
        )
    }
}

@Composable
private fun DinTitleBlock(
    projectName: String,
    author: String,
    diagramName: String,
    createdDate: String,
    modifiedDate: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(280.dp)
            .background(Color.White)
            .border(1.2.dp, Color(0xFF333333))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Projekt:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555), modifier = Modifier.width(70.dp))
                Text(projectName, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Ersteller:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555), modifier = Modifier.width(70.dp))
                Text(author, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Diagramm:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555), modifier = Modifier.width(70.dp))
                Text(diagramName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00008B))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Erstellt:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555), modifier = Modifier.width(70.dp))
                Text(createdDate, fontSize = 10.sp, color = Color(0xFF333333))
                Spacer(Modifier.width(16.dp))
                Text("Geändert:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                Spacer(Modifier.width(4.dp))
                Text(modifiedDate, fontSize = 10.sp, color = Color(0xFF333333))
            }
        }
    }
}

@Composable
private fun DiagramNodeView(
    node: NodeUiState,
    selected: Boolean,
    isConnectSource: Boolean,
    connectMode: Boolean,
    snapToGrid: Boolean,
    isPap: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onMoved: () -> Unit
) {
    val density = LocalDensity.current
    val densityFactor = density.density
    val widthDp = node.shape.widthPx.dp
    val heightDp = node.shape.heightPx.dp

    val fillColor = if (isPap) node.shape.papFillColor() else Color(node.colorArgb)
    val strokeColor = if (isPap) node.shape.papStrokeColor() else MaterialTheme.colorScheme.outline
    val isComment = isPap && node.shape == DiagramShapeKind.COMMENT

    Box(
        modifier = Modifier
            .offset {
                IntOffset((node.x * densityFactor).roundToInt(), (node.y * densityFactor).roundToInt())
            }
            .size(width = widthDp, height = heightDp)
            .then(
                if (!isComment) {
                    Modifier
                        .clip(composeShapeFor(node.shape))
                        .background(if (isConnectSource) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else fillColor)
                        .border(
                            width = if (selected || isConnectSource) 2.5.dp else 1.2.dp,
                            color = if (selected) Color(0xFF1D5BB5) else if (isConnectSource) MaterialTheme.colorScheme.primary else strokeColor,
                            shape = composeShapeFor(node.shape)
                        )
                } else {
                    Modifier.drawBehind {
                        val hook = 10.dp.toPx()
                        val strokeW = if (selected) 2.5.dp.toPx() else 1.8.dp.toPx()
                        val color = if (selected) Color(0xFF1D5BB5) else Color(0xFF00008B)
                        val p = Path().apply {
                            moveTo(hook, 0f)
                            lineTo(0f, 0f)
                            lineTo(0f, size.height)
                            lineTo(hook, size.height)
                        }
                        drawPath(p, color, style = Stroke(width = strokeW))
                    }
                }
            )
            .pointerInput(node.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .pointerInput(node.id, connectMode) {
                if (connectMode) return@pointerInput
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        node.x += dragAmount.x / densityFactor
                        node.y += dragAmount.y / densityFactor
                    },
                    onDragEnd = {
                        if (snapToGrid) {
                            val cx = node.x + node.shape.widthPx / 2f
                            val cy = node.y + node.shape.heightPx / 2f
                            val (col, row) = PapGrid.nearestGrid(cx, cy)
                            node.col = col
                            node.row = row
                            node.x = PapGrid.centerX(col) - node.shape.widthPx / 2f
                            node.y = PapGrid.centerY(row) - node.shape.heightPx / 2f
                        }
                        onMoved()
                    }
                )
            },
        contentAlignment = if (isComment) Alignment.CenterStart else Alignment.Center
    ) {
        if (hasSubroutineStripes(node.shape)) {
            val stripeColor = if (isPap) Color(0xFF348E3E) else Color.Black.copy(alpha = 0.35f)
            Box(Modifier.fillMaxHeight().width(2.dp).offset(x = 10.dp).background(stripeColor))
            Box(Modifier.fillMaxHeight().width(2.dp).offset(x = widthDp - 12.dp).background(stripeColor))
        }

        Text(
            text = node.text,
            fontSize = if (isComment) 11.sp else 12.sp,
            textAlign = if (isComment) TextAlign.Start else TextAlign.Center,
            color = if (isComment) Color(0xFF00008B) else if (isConnectSource) Color.White else Color(0xFF222222),
            modifier = Modifier.padding(
                start = if (isComment) 14.dp else if (node.shape == DiagramShapeKind.IO) 22.dp else 6.dp,
                end = if (node.shape == DiagramShapeKind.IO) 22.dp else 6.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
            maxLines = if (isComment) 6 else 3,
            lineHeight = if (isComment) 13.sp else 15.sp
        )

        // PapDesigner Italic Badge in Lower-Left Corner
        if (node.shape == DiagramShapeKind.IO && node.tag.isNotBlank()) {
            Text(
                text = node.tag,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = Color(0xFFD94838),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 4.dp)
            )
        }
    }
}

private fun Modifier.pointerInputTap(key: Any?, onTap: (Offset) -> Unit): Modifier =
    this.then(Modifier.pointerInput(key) { detectTapGestures(onTap = onTap) })

private class NodeUiState(node: DiagramNode) {
    val id = node.id
    var x by mutableStateOf(node.x)
    var y by mutableStateOf(node.y)
    var shape by mutableStateOf(node.shape)
    var text by mutableStateOf(node.text)
    var colorArgb by mutableStateOf(node.colorArgb)
    var col by mutableStateOf(node.col)
    var row by mutableStateOf(node.row)
    var tag by mutableStateOf(node.tag)
    fun toNode() = DiagramNode(id = id, x = x, y = y, shape = shape, text = text, colorArgb = colorArgb, col = col, row = row, tag = tag)
}
