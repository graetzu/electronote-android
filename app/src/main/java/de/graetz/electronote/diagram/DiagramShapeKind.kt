package de.graetz.electronote.diagram

import androidx.compose.ui.graphics.Color

// Matches Friedrich Folkmann's PapDesigner (DIN 66001) visual style & iOS vocabulary
enum class DiagramShapeKind(val label: String, val widthPx: Float, val heightPx: Float) {
    // PAP (DIN 66001)
    START("Start", 150f, 52f),
    END("Ende", 150f, 52f),
    PROCESS("Prozess", 160f, 54f),
    IO("Ein-/Ausgabe", 160f, 54f),
    DECISION("Verzweigung", 150f, 70f),
    SUBROUTINE("Unterprogramm", 160f, 54f),
    COMMENT("Kommentar", 210f, 65f),
    CONNECTOR("Verbindung", 32f, 32f),

    // MindMap
    CIRCLE("Kreis", 130f, 130f),
    OVAL("Oval", 160f, 95f),
    RECTANGLE("Rechteck", 150f, 90f),
    DIAMOND("Raute", 140f, 110f)
}

fun DiagramShapeKind.papFillColor(): Color = when (this) {
    DiagramShapeKind.START, DiagramShapeKind.END -> Color(0xFFD0E6FA)
    DiagramShapeKind.PROCESS, DiagramShapeKind.SUBROUTINE -> Color(0xFFC4F4C7)
    DiagramShapeKind.IO -> Color(0xFFFFD0C8)
    DiagramShapeKind.DECISION -> Color(0xFFFFF0A5)
    DiagramShapeKind.COMMENT -> Color.Transparent
    DiagramShapeKind.CONNECTOR -> Color.White
    else -> Color(0xFF4FC3F7)
}

fun DiagramShapeKind.papStrokeColor(): Color = when (this) {
    DiagramShapeKind.START, DiagramShapeKind.END -> Color(0xFF4A88C5)
    DiagramShapeKind.PROCESS, DiagramShapeKind.SUBROUTINE -> Color(0xFF348E3E)
    DiagramShapeKind.IO -> Color(0xFFD94838)
    DiagramShapeKind.DECISION -> Color(0xFFC28B10)
    DiagramShapeKind.COMMENT -> Color(0xFF00008B)
    DiagramShapeKind.CONNECTOR -> Color(0xFF555555)
    else -> Color(0xFF1976D2)
}

val PAP_SHAPES = listOf(
    DiagramShapeKind.START,
    DiagramShapeKind.END,
    DiagramShapeKind.PROCESS,
    DiagramShapeKind.IO,
    DiagramShapeKind.DECISION,
    DiagramShapeKind.SUBROUTINE,
    DiagramShapeKind.COMMENT,
    DiagramShapeKind.CONNECTOR
)

val MINDMAP_SHAPES = listOf(
    DiagramShapeKind.CIRCLE,
    DiagramShapeKind.OVAL,
    DiagramShapeKind.RECTANGLE,
    DiagramShapeKind.DIAMOND
)

