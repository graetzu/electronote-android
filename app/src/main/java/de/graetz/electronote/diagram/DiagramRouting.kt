package de.graetz.electronote.diagram

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Default distance a routed line detours away from a node's edge before turning — matches
 * iOS's OrthogonalRoutingEngine default. Adjustable per diagram (see [DiagramDocument.bypassDistancePx])
 * so denser diagrams with bigger nodes can spread routes out further to avoid collisions. */
const val DEFAULT_BYPASS_DISTANCE_PX = 28f

data class PapRoute(val points: List<Offset>, val labelPos: Offset)

/**
 * Full port of iOS's OrthogonalRoutingEngine (PAPDesignerView.swift) — hand-cased Manhattan
 * routing for every relative column/row/port combination (same column going down/up, same
 * row, different column going down/up), each producing an axis-aligned bypass corridor
 * around intervening nodes rather than a straight line through them. Kept in exact sync
 * with the iOS version; when changing one, change the other.
 */
fun routePapConnectionFull(from: DiagramNode, to: DiagramNode, fromPort: DiagramPort, bypassDistance: Float = DEFAULT_BYPASS_DISTANCE_PX): PapRoute {
    val p1 = Offset(from.x + from.widthPx / 2f, from.y + from.heightPx / 2f)
    val p2 = Offset(to.x + to.widthPx / 2f, to.y + to.heightPx / 2f)
    val w1 = from.widthPx / 2f
    val h1 = from.heightPx / 2f
    val w2 = to.widthPx / 2f
    val h2 = to.heightPx / 2f

    val c1 = from.col ?: 1
    val r1 = from.row ?: 0
    val c2 = to.col ?: 1
    val r2 = to.row ?: 0

    // ==========================================
    // 1. SAME COLUMN
    // ==========================================
    if (c1 == c2) {
        // A) Direct neighbor below, exiting bottom
        if (r2 == r1 + 1 && fromPort == DiagramPort.BOTTOM) {
            val start = Offset(p1.x, p1.y + h1)
            val end = Offset(p2.x, p2.y - h2)
            return PapRoute(listOf(start, end), Offset(p1.x + 16, (start.y + end.y) / 2))
        }

        // B) Skipping steps downwards in the same column, or side exit
        if (r2 > r1) {
            return when (fromPort) {
                DiagramPort.LEFT -> {
                    val bypassX = p1.x - w1 - bypassDistance
                    val start = Offset(p1.x - w1, p1.y)
                    val corner1 = Offset(bypassX, p1.y)
                    val corner2 = Offset(bypassX, p2.y)
                    val end = Offset(p2.x - w2, p2.y)
                    PapRoute(listOf(start, corner1, corner2, end), Offset(bypassX - 16, (p1.y + p2.y) / 2))
                }
                DiagramPort.RIGHT -> {
                    val bypassX = p1.x + w1 + bypassDistance
                    val start = Offset(p1.x + w1, p1.y)
                    val corner1 = Offset(bypassX, p1.y)
                    val corner2 = Offset(bypassX, p2.y)
                    val end = Offset(p2.x + w2, p2.y)
                    PapRoute(listOf(start, corner1, corner2, end), Offset(bypassX + 16, (p1.y + p2.y) / 2))
                }
                else -> {
                    val bypassX = p1.x + w1 + bypassDistance
                    val start = Offset(p1.x, p1.y + h1)
                    val stepY = p1.y + h1 + 14
                    val corner0 = Offset(p1.x, stepY)
                    val corner1 = Offset(bypassX, stepY)
                    val corner2 = Offset(bypassX, p2.y)
                    val end = Offset(p2.x + w2, p2.y)
                    PapRoute(listOf(start, corner0, corner1, corner2, end), Offset(bypassX + 16, (stepY + p2.y) / 2))
                }
            }
        }

        // C) Loopback upwards in same column
        if (r2 <= r1) {
            return when (fromPort) {
                DiagramPort.RIGHT -> {
                    val bypassX = p1.x + w1 + bypassDistance
                    val start = Offset(p1.x + w1, p1.y)
                    val corner1 = Offset(bypassX, p1.y)
                    val corner2 = Offset(bypassX, p2.y)
                    val end = Offset(p2.x + w2, p2.y)
                    PapRoute(listOf(start, corner1, corner2, end), Offset(bypassX + 16, (p1.y + p2.y) / 2))
                }
                DiagramPort.TOP -> {
                    val bypassX = p1.x - w1 - bypassDistance
                    val start = Offset(p1.x, p1.y - h1)
                    val stepY = max(0f, p1.y - h1 - 14)
                    val corner0 = Offset(p1.x, stepY)
                    val corner1 = Offset(bypassX, stepY)
                    val corner2 = Offset(bypassX, p2.y)
                    val end = Offset(p2.x - w2, p2.y)
                    PapRoute(listOf(start, corner0, corner1, corner2, end), Offset(bypassX - 16, (p1.y + p2.y) / 2))
                }
                else -> {
                    // Default loopback (left bypass) — used for BOTTOM and LEFT.
                    val bypassX = p1.x - w1 - bypassDistance
                    if (fromPort == DiagramPort.BOTTOM) {
                        val start = Offset(p1.x, p1.y + h1)
                        val stepY = p1.y + h1 + 14
                        val corner0 = Offset(p1.x, stepY)
                        val corner1 = Offset(bypassX, stepY)
                        val corner2 = Offset(bypassX, p2.y)
                        val end = Offset(p2.x - w2, p2.y)
                        PapRoute(listOf(start, corner0, corner1, corner2, end), Offset(bypassX - 16, (p1.y + p2.y) / 2))
                    } else {
                        val start = Offset(p1.x - w1, p1.y)
                        val corner1 = Offset(bypassX, p1.y)
                        val corner2 = Offset(bypassX, p2.y)
                        val end = Offset(p2.x - w2, p2.y)
                        PapRoute(listOf(start, corner1, corner2, end), Offset(bypassX - 16, (p1.y + p2.y) / 2))
                    }
                }
            }
        }
    }

    // ==========================================
    // 2. DIFFERENT COLUMNS
    // ==========================================

    // A) Same row
    if (r1 == r2) {
        return if (c2 > c1) {
            val start = Offset(p1.x + w1, p1.y)
            val end = Offset(p2.x - w2, p2.y)
            PapRoute(listOf(start, end), Offset((start.x + end.x) / 2, p1.y - 12))
        } else {
            val start = Offset(p1.x - w1, p1.y)
            val end = Offset(p2.x + w2, p2.y)
            PapRoute(listOf(start, end), Offset((start.x + end.x) / 2, p1.y - 12))
        }
    }

    // B) Target is downwards in another column
    if (r2 > r1) {
        return if (fromPort == DiagramPort.RIGHT || (c2 > c1 && fromPort != DiagramPort.LEFT && fromPort != DiagramPort.BOTTOM)) {
            if (c2 > c1) {
                val start = Offset(p1.x + w1, p1.y)
                val corner1 = Offset(p2.x, p1.y)
                val end = Offset(p2.x, p2.y - h2)
                PapRoute(listOf(start, corner1, end), Offset((start.x + corner1.x) / 2, p1.y - 12))
            } else {
                val bypassX = p1.x + w1 + (bypassDistance - 8f)
                val start = Offset(p1.x + w1, p1.y)
                val corner1 = Offset(bypassX, p1.y)
                val corner2 = Offset(bypassX, p2.y)
                val end = Offset(p2.x + w2, p2.y)
                PapRoute(listOf(start, corner1, corner2, end), Offset(bypassX + 14, (p1.y + p2.y) / 2))
            }
        } else if (fromPort == DiagramPort.LEFT || (c2 < c1 && fromPort != DiagramPort.RIGHT && fromPort != DiagramPort.BOTTOM)) {
            if (c2 < c1) {
                val start = Offset(p1.x - w1, p1.y)
                val corner1 = Offset(p2.x, p1.y)
                val end = Offset(p2.x, p2.y - h2)
                PapRoute(listOf(start, corner1, end), Offset((start.x + corner1.x) / 2, p1.y - 12))
            } else {
                val bypassX = p1.x - w1 - (bypassDistance - 8f)
                val start = Offset(p1.x - w1, p1.y)
                val corner1 = Offset(bypassX, p1.y)
                val corner2 = Offset(bypassX, p2.y)
                val end = Offset(p2.x - w2, p2.y)
                PapRoute(listOf(start, corner1, corner2, end), Offset(bypassX - 14, (p1.y + p2.y) / 2))
            }
        } else {
            // fromPort == BOTTOM
            val start = Offset(p1.x, p1.y + h1)
            val corner1 = Offset(p1.x, p2.y)
            val end = Offset(if (c1 > c2) p2.x + w2 else p2.x - w2, p2.y)
            PapRoute(listOf(start, corner1, end), Offset((start.x + end.x) / 2, p2.y - 12))
        }
    }

    // C) Target is upwards in another column
    if (r2 < r1) {
        return if (fromPort == DiagramPort.RIGHT || c2 > c1) {
            val rightColX = max(p1.x + PapGrid.COL_WIDTH, p2.x + w2 + (bypassDistance - 8f))
            val start = Offset(p1.x + w1, p1.y)
            val corner1 = Offset(rightColX, p1.y)
            val corner2 = Offset(rightColX, p2.y)
            val end = Offset(p2.x + w2, p2.y)
            PapRoute(listOf(start, corner1, corner2, end), Offset((start.x + corner1.x) / 2, p1.y - 12))
        } else if (fromPort == DiagramPort.LEFT || c2 < c1) {
            val leftColX = min(p1.x - PapGrid.COL_WIDTH, p2.x - w2 - (bypassDistance - 8f))
            val start = Offset(p1.x - w1, p1.y)
            val corner1 = Offset(leftColX, p1.y)
            val corner2 = Offset(leftColX, p2.y)
            val end = Offset(p2.x - w2, p2.y)
            PapRoute(listOf(start, corner1, corner2, end), Offset((start.x + corner1.x) / 2, p1.y - 12))
        } else {
            val start = Offset(p1.x, p1.y - h1)
            val corner1 = Offset(p1.x, p2.y)
            val end = Offset(if (c2 > c1) p2.x - w2 else p2.x + w2, p2.y)
            PapRoute(listOf(start, corner1, end), Offset(p1.x + if (c2 > c1) 16f else -16f, (start.y + p2.y) / 2))
        }
    }

    // Fallback
    val start = Offset(p1.x, p1.y + h1)
    val end = Offset(p2.x, p2.y - h2)
    return PapRoute(listOf(start, end), Offset(p1.x + 14, (p1.y + p2.y) / 2))
}

/** A small triangular arrowhead pointing from the second-to-last point towards [tip]. */
fun arrowHeadPath(points: List<Offset>): Path? {
    if (points.size < 2) return null
    val tip = points.last()
    val prev = points[points.size - 2]
    val dx = tip.x - prev.x
    val dy = tip.y - prev.y
    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.01f)
    val ux = dx / len
    val uy = dy / len
    val size = 12f
    val leftX = tip.x - ux * size - uy * size * 0.6f
    val leftY = tip.y - uy * size + ux * size * 0.6f
    val rightX = tip.x - ux * size + uy * size * 0.6f
    val rightY = tip.y - uy * size - ux * size * 0.6f
    return Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }
}

// MARK: - Crossing "jumps" (schematic-style line hops where two unrelated connections cross)

private const val CROSSING_EPS = 2f

private fun isHorizontal(a: Offset, b: Offset) = abs(a.y - b.y) < 0.5f
private fun isVertical(a: Offset, b: Offset) = abs(a.x - b.x) < 0.5f

/** Interior crossing point of a horizontal and a vertical segment, or null if they don't
 * actually cross (touching only at/near an endpoint doesn't count — that's a shared node
 * connection, not a real crossing). */
private fun crossing(h: Pair<Offset, Offset>, v: Pair<Offset, Offset>): Offset? {
    val hy = h.first.y
    val hx1 = min(h.first.x, h.second.x)
    val hx2 = max(h.first.x, h.second.x)
    val vx = v.first.x
    val vy1 = min(v.first.y, v.second.y)
    val vy2 = max(v.first.y, v.second.y)
    if (vx > hx1 + CROSSING_EPS && vx < hx2 - CROSSING_EPS && hy > vy1 + CROSSING_EPS && hy < vy2 - CROSSING_EPS) {
        return Offset(vx, hy)
    }
    return null
}

/**
 * Builds [points] as a drawable [Path], adding a small semicircular "hop" at every point
 * where a segment of this polyline crosses a segment of an earlier-drawn connection in
 * [priorPolylines] — the classic schematic convention so two unrelated connections
 * crossing on screen never look like they're joined, which matters most for loop-back
 * edges that have to cross the main flow. Only earlier polylines are checked (not later
 * ones) so exactly one of the two lines at any crossing gets the hop, never both.
 */
fun buildPathWithCrossingJumps(points: List<Offset>, priorPolylines: List<List<Offset>>, jumpRadius: Float = 7f): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)

    val otherSegments = priorPolylines.flatMap { poly -> poly.zipWithNext() }

    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        val horizontal = isHorizontal(a, b)
        val vertical = isVertical(a, b)
        if (!horizontal && !vertical) {
            path.lineTo(b.x, b.y)
            continue
        }

        val crossings = mutableListOf<Offset>()
        for ((oa, ob) in otherSegments) {
            val oHorizontal = isHorizontal(oa, ob)
            val oVertical = isVertical(oa, ob)
            if (horizontal && oVertical) {
                crossing(a to b, oa to ob)?.let { crossings.add(it) }
            } else if (vertical && oHorizontal) {
                crossing(oa to ob, a to b)?.let { crossings.add(it) }
            }
        }

        val forward = if (horizontal) (b.x > a.x) else (b.y > a.y)
        val sorted = crossings.sortedBy { c -> if (horizontal) c.x else c.y }.let { if (forward) it else it.reversed() }

        for (c in sorted) {
            if (horizontal) {
                val preX = if (forward) c.x - jumpRadius else c.x + jumpRadius
                path.lineTo(preX, a.y)
                path.arcTo(
                    rect = androidx.compose.ui.geometry.Rect(c.x - jumpRadius, c.y - jumpRadius, c.x + jumpRadius, c.y + jumpRadius),
                    startAngleDegrees = if (forward) 180f else 0f,
                    sweepAngleDegrees = if (forward) 180f else -180f,
                    forceMoveTo = false
                )
            } else {
                val preY = if (forward) c.y - jumpRadius else c.y + jumpRadius
                path.lineTo(a.x, preY)
                path.arcTo(
                    rect = androidx.compose.ui.geometry.Rect(c.x - jumpRadius, c.y - jumpRadius, c.x + jumpRadius, c.y + jumpRadius),
                    startAngleDegrees = if (forward) 270f else 90f,
                    sweepAngleDegrees = if (forward) 180f else -180f,
                    forceMoveTo = false
                )
            }
        }
        path.lineTo(b.x, b.y)
    }
    return path
}

/** Cubic-Bézier "branch" curve for MindMap connections, exiting the left/right side of
 * each node depending on relative horizontal position (matches iOS's always-horizontal
 * S-curve regardless of vertical offset). */
fun mindMapEndpoints(from: DiagramNode, to: DiagramNode): Pair<Offset, Offset> {
    val fromCenter = Offset(from.x + from.widthPx / 2f, from.y + from.heightPx / 2f)
    val toCenter = Offset(to.x + to.widthPx / 2f, to.y + to.heightPx / 2f)
    val fromPoint = if (toCenter.x >= fromCenter.x) Offset(from.x + from.widthPx, fromCenter.y) else Offset(from.x, fromCenter.y)
    val toPoint = if (toCenter.x >= fromCenter.x) Offset(to.x, toCenter.y) else Offset(to.x + to.widthPx, toCenter.y)
    return fromPoint to toPoint
}

fun mindMapCurvePath(start: Offset, end: Offset): Path {
    val path = Path()
    path.moveTo(start.x, start.y)
    val dx = end.x - start.x
    path.cubicTo(start.x + dx * 0.5f, start.y, start.x + dx * 0.5f, end.y, end.x, end.y)
    return path
}
