package de.graetz.electronote.diagram

import java.util.UUID

enum class PapTemplate(val id: String, val title: String, val description: String) {
    TUTORIAL_1("was_ist_das", "Ablaufplan - Was ist das?", "Tutorial 1 (Seite 1): Grundelemente & Erklärungen"),
    TUTORIAL_2("eva_prinzip", "Ablaufplan - Beispiel 1", "Tutorial 1 (Seite 2): EVA-Prinzip mit Mittelwert"),
    TUTORIAL_3("installationsablauf", "Ablaufplan - Beispiel 2", "Tutorial 1 (Seite 3): Installationsablauf mit Verzweigungen & Abbruchbus"),
    EMPTY("empty", "Leerer Ablaufplan", "Neuer Startblock mit DIN-Schriftfeld")
}

data class TemplateResult(
    val name: String,
    val nodes: List<DiagramNode>,
    val connections: List<DiagramConnection>
)

object PapTemplates {
    private fun createNode(
        shape: DiagramShapeKind,
        text: String,
        col: Int,
        row: Int,
        tag: String = "",
        customWidth: Float? = null,
        customHeight: Float? = null
    ): DiagramNode {
        val w = customWidth ?: shape.widthPx
        val h = customHeight ?: shape.heightPx
        return DiagramNode(
            id = UUID.randomUUID().toString(),
            x = PapGrid.centerX(col) - w / 2f,
            y = PapGrid.centerY(row) - h / 2f,
            shape = shape,
            text = text,
            col = col,
            row = row,
            tag = tag
        )
    }

    fun load(template: PapTemplate): TemplateResult = when (template) {
        PapTemplate.TUTORIAL_1 -> loadTutorial1()
        PapTemplate.TUTORIAL_2 -> loadTutorial2()
        PapTemplate.TUTORIAL_3 -> loadTutorial3()
        PapTemplate.EMPTY -> loadEmpty()
    }

    private fun loadTutorial1(): TemplateResult {
        val nStart = createNode(DiagramShapeKind.START, "Start", 1, 0)
        val nEingabe = createNode(DiagramShapeKind.IO, "Eingabe", 1, 1, tag = "E")
        val nVorgang = createNode(DiagramShapeKind.PROCESS, "Vorgang", 1, 2)
        val nUnterprog = createNode(DiagramShapeKind.SUBROUTINE, "Unterprogramm", 1, 3)
        val nAusgabe = createNode(DiagramShapeKind.IO, "Ausgabe", 1, 4, tag = "A")
        val nEnde = createNode(DiagramShapeKind.END, "Ende", 1, 5)

        // Left Comments
        val cL0 = createNode(
            DiagramShapeKind.COMMENT,
            "Programmablaufpläne (PAPs) sind grafische Diagramme. Sie zeigen, welche Vorgänge oder Aktivitäten nacheinander ausgeführt werden.",
            0, 0
        )
        val cL1 = createNode(
            DiagramShapeKind.COMMENT,
            "Beispiele für Abläufe:\n• Rechenverfahren\n• Bedienungsanleitung\n• Garagentorsteuerung",
            0, 1
        )
        val cL2 = createNode(
            DiagramShapeKind.COMMENT,
            "Programmablaufpläne werden auch als Flussdiagramme bezeichnet, weil sie Vorgänge als zeitlichen Fluss darstellen.",
            0, 2
        )
        val cL3 = createNode(
            DiagramShapeKind.COMMENT,
            "Fahren Sie also gedanklich mit dem Boot entlang der Pfeile flussabwärts von Start bis Ende.",
            0, 3
        )
        val cL4 = createNode(
            DiagramShapeKind.COMMENT,
            "Verschiedene Symbole verdeutlichen unterschiedliche Arten von Vorgängen (DIN 66001 / PapDesigner).",
            0, 4
        )

        // Right Comments
        val cR0 = createNode(
            DiagramShapeKind.COMMENT,
            "Die verschiedenen Vorgänge oder Aktivitäten werden als Symbole dargestellt. Die Pfeile kennzeichnen die Reihenfolge.",
            2, 0
        )
        val cR1 = createNode(
            DiagramShapeKind.COMMENT,
            "Eingabevorgang:\nDaten werden in das System eingegeben bzw. eingelesen.\n(Hinweis: 'E' ist PapDesigner-Kennung)",
            2, 1
        )
        val cR2 = createNode(
            DiagramShapeKind.COMMENT,
            "Elementarer Vorgang,\ndessen interne Details nicht in einem anderen Diagramm näher dargestellt werden.",
            2, 2
        )
        val cR3 = createNode(
            DiagramShapeKind.COMMENT,
            "Komplexer Vorgang bzw. Unterprogramm,\ndessen Ablaufdetails meist in einem weiteren Diagramm dargestellt werden.",
            2, 3
        )
        val cR4 = createNode(
            DiagramShapeKind.COMMENT,
            "Ausgabevorgang:\nDaten werden vom System ausgegeben oder auf einem Sichtgerät angezeigt.",
            2, 4
        )

        val nodes = listOf(
            nStart, nEingabe, nVorgang, nUnterprog, nAusgabe, nEnde,
            cL0, cL1, cL2, cL3, cL4,
            cR0, cR1, cR2, cR3, cR4
        )

        val connections = listOf(
            DiagramConnection(fromNodeId = nStart.id, toNodeId = nEingabe.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nEingabe.id, toNodeId = nVorgang.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nVorgang.id, toNodeId = nUnterprog.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nUnterprog.id, toNodeId = nAusgabe.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nAusgabe.id, toNodeId = nEnde.id, fromPort = DiagramPort.BOTTOM)
        )

        return TemplateResult("Ablaufplan - Was ist das?", nodes, connections)
    }

    private fun loadTutorial2(): TemplateResult {
        val nStart = createNode(DiagramShapeKind.START, "Start", 1, 0)
        val nEingabeA = createNode(DiagramShapeKind.IO, "Eingabe von Zahl a", 1, 1, tag = "E")
        val nEingabeB = createNode(DiagramShapeKind.IO, "Eingabe von Zahl b", 1, 2, tag = "E")
        val nCalc = createNode(DiagramShapeKind.PROCESS, "Berechnung des Mittelwertes\nc = (a + b)/2", 1, 3)
        val nAusgabe = createNode(DiagramShapeKind.IO, "Ausgabe des Ergebnisses\nc", 1, 4, tag = "A")
        val nEnde = createNode(DiagramShapeKind.END, "Ende", 1, 5)

        val cL0 = createNode(
            DiagramShapeKind.COMMENT,
            "Zeigt ein Beispiel für das grundlegende EVA-Prinzip einfacher Datenverarbeitungssysteme.",
            0, 0
        )
        val cL1 = createNode(DiagramShapeKind.COMMENT, "E = Eingabe", 0, 1)
        val cL3 = createNode(DiagramShapeKind.COMMENT, "V = Verarbeitung", 0, 3)
        val cL4 = createNode(DiagramShapeKind.COMMENT, "A = Ausgabe", 0, 4)

        val cR3 = createNode(
            DiagramShapeKind.COMMENT,
            "Beachten Sie, dass die Symbole knapp aber aussagekräftig beschriftet werden.\nZiel: Spontanes Verstehen!",
            2, 3
        )

        val nodes = listOf(
            nStart, nEingabeA, nEingabeB, nCalc, nAusgabe, nEnde,
            cL0, cL1, cL3, cL4, cR3
        )

        val connections = listOf(
            DiagramConnection(fromNodeId = nStart.id, toNodeId = nEingabeA.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nEingabeA.id, toNodeId = nEingabeB.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nEingabeB.id, toNodeId = nCalc.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nCalc.id, toNodeId = nAusgabe.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nAusgabe.id, toNodeId = nEnde.id, fromPort = DiagramPort.BOTTOM)
        )

        return TemplateResult("Ablaufplan - Beispiel 1", nodes, connections)
    }

    private fun loadTutorial3(): TemplateResult {
        // Main Spine
        val nStart = createNode(DiagramShapeKind.START, "Start", 1, 0)
        val nLizenz = createNode(DiagramShapeKind.IO, "Lizenzabkommen akzeptieren", 1, 1, tag = "E")
        val dLizenz = createNode(DiagramShapeKind.DECISION, "Lizenzvereinbarung akzeptiert?", 1, 2)
        val nAuswahl = createNode(DiagramShapeKind.IO, "Auswahl der Installationsbestandteile", 1, 3, tag = "E")
        val dDotNet = createNode(DiagramShapeKind.DECISION, ".NET 2 Test ausgewählt?", 1, 4)
        val nVerz = createNode(DiagramShapeKind.IO, "Eingabe von Zielverzeichnis", 1, 5, tag = "E")
        val dVerz = createNode(DiagramShapeKind.DECISION, "Verzeichnis ok und schreibberechtigt?", 1, 6)
        val nInstall = createNode(DiagramShapeKind.PROCESS, "PapDesigner installieren", 1, 7)
        val dInstall = createNode(DiagramShapeKind.DECISION, "Installation erfolgreich?", 1, 8)
        val dDeinst = createNode(DiagramShapeKind.DECISION, "Deinstaller ausgewählt?", 1, 9)
        val dMenu = createNode(DiagramShapeKind.DECISION, "Startmenüeintrag ausgewählt?", 1, 10)
        val nEnde = createNode(DiagramShapeKind.END, "Ende", 1, 12)

        // Side Branches (Col 2 & 3)
        val pDotNet = createNode(DiagramShapeKind.PROCESS, ".NET 2 Installation testen", 2, 4)
        val dDotNetOk = createNode(DiagramShapeKind.DECISION, "Test erfolgreich?", 3, 4)

        val pDeinst = createNode(DiagramShapeKind.PROCESS, "Deinstaller installieren", 2, 9)
        val dDeinstOk = createNode(DiagramShapeKind.DECISION, "Installation erfolgreich?", 3, 9)

        val pMenuAll = createNode(DiagramShapeKind.PROCESS, "Startmenü anlegen alle Anwender (Admin)", 2, 10)
        val dMenuAllOk = createNode(DiagramShapeKind.DECISION, "Eintrag erfolgreich?", 3, 10)

        val pMenuUser = createNode(DiagramShapeKind.PROCESS, "Startmenü anlegen aktueller Anwender", 2, 11)
        val dMenuUserOk = createNode(DiagramShapeKind.DECISION, "Eintrag erfolgreich?", 3, 11)

        // Abort Bus
        val pAbbruch = createNode(DiagramShapeKind.PROCESS, "Abbruch der Installation", 4, 12)

        // Comments
        val cIntro = createNode(DiagramShapeKind.COMMENT, "Dieses Beispiel demonstriert den Installationsprozess vom PapDesigner-Setup", 2, 1)
        val cOpt = createNode(DiagramShapeKind.COMMENT, "Optional:\n• .NET 2 Test\n• Deinstaller einrichten\n• Startmenüeintrag vornehmen", 2, 3)
        val cDefDir = createNode(DiagramShapeKind.COMMENT, "Standardvorgabe:\nC:\\Programme\\PapDesigner", 2, 5)

        val nodes = listOf(
            nStart, nLizenz, dLizenz, nAuswahl, dDotNet, nVerz, dVerz, nInstall, dInstall, dDeinst, dMenu, nEnde,
            pDotNet, dDotNetOk,
            pDeinst, dDeinstOk,
            pMenuAll, dMenuAllOk,
            pMenuUser, dMenuUserOk,
            pAbbruch,
            cIntro, cOpt, cDefDir
        )

        val connections = listOf(
            // Main stem
            DiagramConnection(fromNodeId = nStart.id, toNodeId = nLizenz.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = nLizenz.id, toNodeId = dLizenz.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dLizenz.id, toNodeId = nAuswahl.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dLizenz.id, toNodeId = pAbbruch.id, label = "nein", fromPort = DiagramPort.RIGHT),

            DiagramConnection(fromNodeId = nAuswahl.id, toNodeId = dDotNet.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dDotNet.id, toNodeId = nVerz.id, label = "nein", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dDotNet.id, toNodeId = pDotNet.id, label = "ja", fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = pDotNet.id, toNodeId = dDotNetOk.id, fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = dDotNetOk.id, toNodeId = nVerz.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dDotNetOk.id, toNodeId = pAbbruch.id, label = "nein", fromPort = DiagramPort.RIGHT),

            DiagramConnection(fromNodeId = nVerz.id, toNodeId = dVerz.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dVerz.id, toNodeId = nInstall.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dVerz.id, toNodeId = nVerz.id, label = "nein", fromPort = DiagramPort.LEFT), // Loopback!

            DiagramConnection(fromNodeId = nInstall.id, toNodeId = dInstall.id, fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dInstall.id, toNodeId = dDeinst.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dInstall.id, toNodeId = pAbbruch.id, label = "nein", fromPort = DiagramPort.RIGHT),

            DiagramConnection(fromNodeId = dDeinst.id, toNodeId = dMenu.id, label = "nein", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dDeinst.id, toNodeId = pDeinst.id, label = "ja", fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = pDeinst.id, toNodeId = dDeinstOk.id, fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = dDeinstOk.id, toNodeId = dMenu.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dDeinstOk.id, toNodeId = pAbbruch.id, label = "nein", fromPort = DiagramPort.RIGHT),

            DiagramConnection(fromNodeId = dMenu.id, toNodeId = nEnde.id, label = "nein", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dMenu.id, toNodeId = pMenuAll.id, label = "ja", fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = pMenuAll.id, toNodeId = dMenuAllOk.id, fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = dMenuAllOk.id, toNodeId = nEnde.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dMenuAllOk.id, toNodeId = pMenuUser.id, label = "nein", fromPort = DiagramPort.RIGHT),

            DiagramConnection(fromNodeId = pMenuUser.id, toNodeId = dMenuUserOk.id, fromPort = DiagramPort.RIGHT),
            DiagramConnection(fromNodeId = dMenuUserOk.id, toNodeId = nEnde.id, label = "ja", fromPort = DiagramPort.BOTTOM),
            DiagramConnection(fromNodeId = dMenuUserOk.id, toNodeId = pAbbruch.id, label = "nein", fromPort = DiagramPort.RIGHT),

            DiagramConnection(fromNodeId = pAbbruch.id, toNodeId = nEnde.id, fromPort = DiagramPort.BOTTOM)
        )

        return TemplateResult("Ablaufplan - Beispiel 2", nodes, connections)
    }

    private fun loadEmpty(): TemplateResult {
        val nStart = createNode(DiagramShapeKind.START, "Start", 1, 0)
        return TemplateResult("Neuer Ablaufplan", listOf(nStart), emptyList())
    }
}
