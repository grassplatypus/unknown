package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color

/** An axis-aligned box in world cells, with the object it belongs to (null for floor and walls). */
data class WorldBox(
    val x0: Float, val y0: Float, val z0: Float,
    val x1: Float, val y1: Float, val z1: Float,
    val color: Color,
    val emissive: Color? = null,
    val ownerId: String? = null,
)

/** Boxes in paint order. Objects (hit-testable) start at [firstObject]. */
internal class DrawList(val boxes: List<WorldBox>, val firstObject: Int)

private const val E = 1e-4f

/**
 * Painter's order for non-intersecting boxes under any orthographic camera. A pair that overlaps on
 * screen is ordered by an axis that separates them and faces the camera; the graph is then sorted,
 * nearest-last, falling back to centre depth to break the rare cycle.
 */
fun paintOrder(boxes: List<WorldBox>, camera: IsoCamera): List<WorldBox> {
    val n = boxes.size
    if (n < 2) return boxes
    val p = Projection(camera, 1f, 0f, 0f, 0f, 0f)
    val minU = FloatArray(n); val maxU = FloatArray(n); val minV = FloatArray(n); val maxV = FloatArray(n)
    val depth = FloatArray(n)
    boxes.forEachIndexed { i, b ->
        var u0 = Float.MAX_VALUE; var u1 = -Float.MAX_VALUE; var v0 = Float.MAX_VALUE; var v1 = -Float.MAX_VALUE
        for (k in 0 until 8) {
            val x = if (k and 1 == 0) b.x0 else b.x1
            val y = if (k and 2 == 0) b.y0 else b.y1
            val z = if (k and 4 == 0) b.z0 else b.z1
            val u = p.screenX(x, y, z); val v = p.screenY(x, y, z)
            if (u < u0) u0 = u; if (u > u1) u1 = u; if (v < v0) v0 = v; if (v > v1) v1 = v
        }
        minU[i] = u0; maxU[i] = u1; minV[i] = v0; maxV[i] = v1
        depth[i] = p.depth((b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2, (b.z0 + b.z1) / 2)
    }
    val after = Array(n) { BooleanArray(n) } // after[i][j]: j must be painted after i
    val indeg = IntArray(n)
    for (i in 0 until n) for (j in i + 1 until n) {
        if (maxU[i] <= minU[j] + E || maxU[j] <= minU[i] + E || maxV[i] <= minV[j] + E || maxV[j] <= minV[i] + E) continue
        val iFirst = behind(boxes[i], boxes[j], p)
        val jFirst = behind(boxes[j], boxes[i], p)
        if (iFirst && !jFirst) { after[i][j] = true; indeg[j]++ }
        if (jFirst && !iFirst) { after[j][i] = true; indeg[i]++ }
    }
    val done = BooleanArray(n)
    val out = ArrayList<WorldBox>(n)
    repeat(n) {
        var pick = -1
        for (i in 0 until n) if (!done[i] && indeg[i] == 0 && (pick < 0 || depth[i] < depth[pick])) pick = i
        if (pick < 0) for (i in 0 until n) if (!done[i] && (pick < 0 || depth[i] < depth[pick])) pick = i
        done[pick] = true
        out += boxes[pick]
        for (j in 0 until n) if (after[pick][j]) indeg[j]--
    }
    return out
}

/** True when a plane separates [a] from [b] with [a] on the side away from the camera. */
private fun behind(a: WorldBox, b: WorldBox, p: Projection): Boolean =
    (p.toCamX > E && a.x1 <= b.x0 + E) || (p.toCamX < -E && a.x0 >= b.x1 - E) ||
        (p.toCamY > E && a.y1 <= b.y0 + E) || (p.toCamY < -E && a.y0 >= b.y1 - E) ||
        (p.toCamZ > E && a.z1 <= b.z0 + E) || (p.toCamZ < -E && a.z0 >= b.z1 - E)

internal fun IsoObject.worldBoxes(baseZ: Float): List<WorldBox> = shape.parts.map { c ->
    val r = c.rotated(shape.w, shape.d, rot)
    WorldBox(
        x + r.x, y + r.y, baseZ + r.z,
        x + r.x + r.w, y + r.y + r.d, baseZ + r.z + r.h,
        r.color, r.emissive, id,
    )
}

/** Walls are drawn only where they stand behind the room, so they never hide it; all four from the top. */
internal fun wallVisible(side: WallSide, p: Projection): Boolean {
    if (p.toCamZ > 0.99f) return true
    val facing = when (side) {
        WallSide.NORTH -> p.toCamY
        WallSide.SOUTH -> -p.toCamY
        WallSide.WEST -> p.toCamX
        WallSide.EAST -> -p.toCamX
    }
    return facing > E
}

/** Order: walls, floor, flat objects, standing objects. Flat things are ~0 high, so a pass is enough. */
internal fun buildDrawList(scene: IsoScene, camera: IsoCamera): DrawList {
    val p = Projection(camera, 1f, 0f, 0f, 0f, 0f)
    val out = ArrayList<WorldBox>()
    if (scene.wallHeight > 0f) {
        val w = scene.width.toFloat(); val d = scene.depth.toFloat(); val h = scene.wallHeight; val t = WALL_THICKNESS
        val c = IsoPalette.Wall
        val walls = buildList {
            if (wallVisible(WallSide.NORTH, p)) add(WorldBox(-t, -t, FLOOR_BASE, w + t, 0f, h, c))
            if (wallVisible(WallSide.SOUTH, p)) add(WorldBox(-t, d, FLOOR_BASE, w + t, d + t, h, c))
            if (wallVisible(WallSide.WEST, p)) add(WorldBox(-t, 0f, FLOOR_BASE, 0f, d, h, c))
            if (wallVisible(WallSide.EAST, p)) add(WorldBox(w, 0f, FLOOR_BASE, w + t, d, h, c))
        }
        out += paintOrder(walls, camera)
    }
    // Equal-size grid columns: centre depth on the floor plane already agrees with the separating axis.
    val floor = ArrayList<WorldBox>(scene.cells.size)
    for (y in 0 until scene.depth) for (x in 0 until scene.width) {
        val cell = scene.cell(x, y)
        if (cell.floor == FloorType.NONE) continue
        floor += WorldBox(x.toFloat(), y.toFloat(), FLOOR_BASE, x + 1f, y + 1f, cell.height, IsoPalette.floor(cell.floor))
    }
    floor.sortBy { p.toCamX * (it.x0 + 0.5f) + p.toCamY * (it.y0 + 0.5f) }
    out += floor
    val firstObject = out.size
    val shown = scene.objects.filter { it.wall == null || (scene.wallHeight > 0f && wallVisible(it.wall, p)) }
    val (flat, standing) = shown.partition { it.shape.flat }
    out += paintOrder(flat.flatMap { it.worldBoxes(scene.baseZ(it)) }, camera)
    out += paintOrder(standing.flatMap { it.worldBoxes(scene.baseZ(it)) }, camera)
    return DrawList(out, firstObject)
}

private const val WALL_THICKNESS = 0.25f
