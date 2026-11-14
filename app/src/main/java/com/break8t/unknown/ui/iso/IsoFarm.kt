package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color

/**
 * Farm screen models at the S-13 art's own pixel grid. The farm view is a close-up, so one farm cell is
 * [FINE] engine cells and one voxel lands near one art pixel on screen instead of a 3 x 3 block.
 */
object IsoFarm {
    const val FINE = 4

    /** Voxels across one farm-screen bed (two farm cells). */
    const val BED_VOXELS = 2 * FINE * VPC

    private const val ART = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val ROW_H = 2 // the 55 degree camera squashes upright sprites to about half height

    private fun c(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, color: Color, glow: Color? = null) =
        Cuboid(x, y, z, w, d, h, color, glow)

    /** Lamp bulbs in the art are the only near-white warm pixels; they keep glowing at night. */
    private fun glows(argb: Int): Boolean = (argb shr 16 and 0xFF) > 235 && (argb shr 8 and 0xFF) > 190

    /** Stands [rows] upright with its left edge at [x0] and its front face at voxel [y], bottom at [z0]. */
    private fun MutableList<Cuboid>.art(
        rows: List<String>, x0: Int, y: Int, depth: Int, z0: Int, rowH: Int = ROW_H, recolor: (Int) -> Int = { it },
    ) = rows.forEachIndexed { r, line ->
        val z = z0 + (rows.size - 1 - r) * rowH
        line.forEachIndexed { i, ch ->
            val k = ART.indexOf(ch)
            if (k >= 0) {
                val argb = recolor(IsoFarmArt.palette[k])
                add(c(x0 + i, y - depth + 1, z, 1, depth, rowH, Color(argb), if (glows(argb)) Color(argb) else null))
            }
        }
    }

    /**
     * Sprite rows hidden in the soil, so the crop reads as planted (S-13): carrots and sweet potatoes show
     * their upper half, more while still growing; wheat stalks rise straight out of the ground.
     */
    internal fun buriedRows(stage: PlotStage, crop: Crop): Int = when {
        stage == PlotStage.EMPTY -> 0
        stage == PlotStage.SPROUT -> 3
        crop == Crop.WHEAT -> 3
        crop == Crop.CARROT -> if (stage == PlotStage.GROWN) 4 else 5
        else -> if (stage == PlotStage.GROWN) 4 else 6
    }

    /** Ripe wheat's gold turned to young green for a wheat bed that is still growing. */
    private fun unripe(c0: Int): Int {
        val r = c0 shr 16 and 0xFF; val g = c0 shr 8 and 0xFF; val b = c0 and 0xFF
        if (r < 150 || g < 90 || b > 120) return c0
        return argb(r * 0.45f, g * 0.9f, b * 0.55f + 20f)
    }

    fun bed(stage: PlotStage, crop: Crop): IsoShape = beds.getValue(stage to crop)

    private val beds: Map<Pair<PlotStage, Crop>, IsoShape> by lazy {
        PlotStage.entries.flatMap { st -> Crop.entries.map { st to it } }.associateWith { (st, cr) -> buildBed(st, cr) }
    }

    /** Voxels the bed is lowered by so the crop's buried rows sit under the ground; the scene sets the object's z. */
    fun sink(stage: PlotStage, crop: Crop): Int = buriedRows(stage, crop) * if (stage == PlotStage.SPROUT) 1 else ROW_H

    /**
     * Sprite from voxel 1 up, soil plate at [sink]: lowered by [sink], the plate is at ground level and the
     * rows under it are hidden by the floor, as if planted. A dark rim in front is the hole the art draws.
     */
    private fun buildBed(stage: PlotStage, crop: Crop): IsoShape = IsoShape(2 * FINE, 2 * FINE, buildList {
        val n = BED_VOXELS
        val ground = sink(stage, crop)
        val front = n * 5 / 8
        val rows: List<String> = when {
            stage == PlotStage.EMPTY -> emptyList()
            stage == PlotStage.SPROUT -> IsoFarmArt.CARROT.take(SPROUT_ROWS)
            crop == Crop.CARROT -> IsoFarmArt.CARROT
            crop == Crop.SWEET_POTATO -> IsoFarmArt.POTATO
            else -> IsoFarmArt.WHEAT
        }
        val x0 = if (rows.isEmpty()) 0 else (n - rows[0].length) / 2
        if (stage == PlotStage.SPROUT) art(rows, x0, front, 2, 1, rowH = 1)
        else if (rows.isNotEmpty()) {
            val recolor: (Int) -> Int = if (crop == Crop.WHEAT && stage != PlotStage.GROWN) ::unripe else { x -> x }
            art(rows, x0, front, 3, 1, recolor = recolor)
        }
        add(c(0, 0, ground, n, n, 1, IsoPalette.SoilC))
        // Diagonal furrows like the bed texture in the art.
        for (k in -n until n step 9) for (i in 0 until n) {
            val j = i + k
            if (j in 0 until n) add(c(i, j, ground, 1, 1, 1, IsoPalette.SoilDarkC))
        }
        if (ground > 0) {
            val low = rows.takeLast(buriedRows(stage, crop))
            for (i in rows[0].indices) {
                val near = (-1..1).any { d -> low.any { r -> r.getOrNull(i + d)?.let { ART.indexOf(it) >= 0 } == true } }
                if (near) add(c(x0 + i, front + 1, ground, 1, RIM, 1, IsoPalette.SoilDarkC))
            }
        }
    })

    /** The post-and-lights fence of the art, as one upright sheet; posts are made deeper so they read as posts. */
    val fence: IsoShape by lazy {
        val rows = IsoFarmArt.FENCE
        val w = (rows[0].length + VPC - 1) / VPC
        IsoShape(w, 1, buildList { art(rows, 0, VPC - 2, 4, 0) })
    }

    /** The three-blade tuft drawn on each grass tile of the art. */
    val tuft = IsoShape(2, 2, buildList {
        val rows = listOf("....d....", "d...d...d", ".d..d..d.", ".d..d..d.", "..d.d.d..", "..ddddd..")
        val green = Color(TUFT)
        rows.forEachIndexed { r, line ->
            line.forEachIndexed { i, ch -> if (ch == 'd') add(c(4 + i, 9, (rows.size - 1 - r) * ROW_H, 1, 2, ROW_H, green)) }
        }
    })

    private const val SPROUT_ROWS = 14
    private const val RIM = 3

    private const val TUFT = 0xFF2E6B2A.toInt()
}
