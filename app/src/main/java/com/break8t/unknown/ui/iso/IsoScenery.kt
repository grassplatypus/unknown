package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color

/** Island and room scenery modelled on the Figma art (S-12, S-13, S-14). Fronts face +y like [IsoShapes]. */
object IsoScenery {
    private val P = IsoPalette

    private fun c(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, color: Color, glow: Color? = null) =
        Cuboid(x, y, z, w, d, h, color, glow)

    /** A box with its four vertical edges cut off, the round lump the tree and bush art is made of. */
    private fun MutableList<Cuboid>.lump(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, color: Color) {
        add(c(x + 1, y, z, w - 2, d, h, color))
        add(c(x, y + 1, z, 1, d - 2, h, color))
        add(c(x + w - 1, y + 1, z, 1, d - 2, h, color))
    }

    /** Dark underside, mid body, light crown: the three-tone canopy of the art. */
    private fun MutableList<Cuboid>.canopy(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int) {
        lump(x, y, z, w, d, h, P.LeafDark)
        lump(x, y, z + 1, w, d, h - 1, P.Leaf)
        lump(x + 1, y + 1, z + h - 1, w - 2, d - 2, 1, P.LeafLight)
    }

    /** 16 x 16 x 26: thick trunk with roots and three canopy lumps. */
    val bigTree = IsoShape(2, 2, buildList {
        add(c(6, 6, 0, 4, 4, 12, P.Trunk))
        add(c(5, 7, 0, 6, 2, 2, P.TrunkDark)); add(c(7, 5, 0, 2, 6, 2, P.TrunkDark))
        add(c(6, 6, 6, 1, 1, 4, P.TrunkDark))
        // Overlapping lumps rather than tiers, so the outline reads as one round crown.
        canopy(1, 1, 10, 14, 14, 6)
        canopy(0, 4, 12, 5, 8, 5); canopy(11, 3, 13, 5, 8, 5)
        canopy(4, 0, 14, 8, 5, 5); canopy(3, 10, 12, 9, 6, 6)
        canopy(3, 3, 15, 10, 10, 6)
        canopy(5, 5, 20, 6, 6, 3)
        add(c(2, 13, 14, 1, 1, 1, P.LeafLight)); add(c(14, 6, 15, 1, 1, 1, P.LeafLight)); add(c(8, 15, 15, 1, 1, 1, P.LeafLight))
    })

    /** 8 x 8 x 15: a small round tree. */
    val tree = IsoShape(1, 1, buildList {
        add(c(3, 3, 0, 2, 2, 6, P.Trunk)); add(c(3, 3, 0, 1, 1, 2, P.TrunkDark))
        canopy(0, 1, 5, 8, 6, 5); canopy(1, 0, 6, 6, 8, 5)
        canopy(1, 1, 9, 6, 6, 4); canopy(2, 2, 12, 4, 4, 2)
        add(c(0, 3, 7, 1, 1, 1, P.LeafLight)); add(c(5, 7, 8, 1, 1, 1, P.LeafLight))
    })

    /** 8 x 8 x 14: round tree with red apples. */
    val appleTree = IsoShape(1, 1, buildList {
        add(c(3, 3, 0, 2, 2, 6, P.Trunk)); add(c(2, 3, 0, 4, 2, 1, P.TrunkDark))
        canopy(0, 1, 5, 8, 6, 5); canopy(1, 0, 6, 6, 8, 5)
        canopy(1, 1, 9, 6, 6, 4)
        for ((x, y, z) in listOf(Triple(1, 7, 7), Triple(5, 7, 8), Triple(7, 4, 7), Triple(3, 6, 11), Triple(7, 1, 9))) {
            add(c(x, y, z, 1, 1, 1, P.Red))
        }
    })

    /** 8 x 8 x 5: two rounded clumps. */
    val bush = IsoShape(1, 1, buildList {
        lump(0, 2, 0, 5, 5, 4, P.LeafDark); lump(0, 2, 1, 5, 5, 3, P.Leaf); add(c(1, 3, 3, 3, 3, 1, P.LeafLight))
        lump(3, 0, 0, 5, 5, 5, P.LeafDark); lump(3, 0, 1, 5, 5, 4, P.Leaf); add(c(4, 1, 4, 3, 3, 1, P.LeafLight))
    })

    private fun flowers(petal: Color, center: Color) = IsoShape(1, 1, buildList {
        for ((x, y) in listOf(1 to 1, 5 to 2, 2 to 5, 6 to 6)) {
            add(c(x, y, 0, 1, 1, 2, P.Leaf))
            add(c(x - 1, y, 2, 3, 1, 1, petal)); add(c(x, y - 1, 2, 1, 3, 1, petal)); add(c(x, y, 2, 1, 1, 1, center))
        }
    })

    val redFlowers = flowers(P.Red, P.Yellow)
    val yellowFlowers = flowers(P.Yellow, P.Orange)

    /** The little grass tuft drawn on each border tile of the farm art. */
    val tuft = IsoShape(1, 1, listOf(
        c(2, 4, 0, 1, 1, 1, P.LeafDark), c(4, 4, 0, 1, 1, 2, P.LeafDark), c(6, 4, 0, 1, 1, 1, P.LeafDark),
    ))

    /** 8 x 8 x 3 grey stones. */
    val rocks = IsoShape(1, 1, listOf(
        c(1, 1, 0, 4, 3, 2, P.StoneC), c(2, 1, 2, 2, 2, 1, P.StoneLight),
        c(4, 4, 0, 3, 3, 2, P.StoneC), c(5, 4, 2, 1, 2, 1, P.StoneLight),
    ))

    /** 8 x 8 x 12: wooden signpost with two boards. */
    val signpost = IsoShape(1, 1, listOf(
        c(3, 3, 0, 2, 2, 11, P.Wood), c(3, 3, 11, 2, 2, 1, P.WoodLight),
        c(0, 2, 7, 7, 1, 3, P.WoodLight), c(0, 2, 8, 6, 1, 1, P.WoodDark),
        c(2, 5, 3, 6, 1, 3, P.WoodLight), c(3, 5, 4, 5, 1, 1, P.WoodDark),
    ))

    /** 8 x 8 x 9: red mailbox on a post. */
    val mailbox = IsoShape(1, 1, listOf(
        c(3, 3, 0, 2, 2, 6, P.WoodDark),
        c(2, 2, 6, 4, 5, 3, P.Red), c(2, 6, 7, 4, 1, 1, P.Cream), c(6, 3, 7, 1, 1, 2, P.Yellow),
    ))

    /** 16 x 16 x 20: market stall with produce crates, lanterns and a red and white awning. */
    val stall = IsoShape(2, 2, buildList {
        // posts first so the counter and awning overwrite where they cross
        for ((x, y, h) in listOf(Triple(1, 2, 17), Triple(14, 2, 17), Triple(1, 13, 12), Triple(14, 13, 12))) add(c(x, y, 0, 1, 1, h, P.WoodDark))
        add(c(1, 3, 0, 14, 4, 12, P.WoodDark)) // back shelf
        add(c(1, 8, 0, 14, 6, 6, P.Wood))
        for (z in intArrayOf(1, 3)) add(c(1, 13, z, 14, 1, 1, P.WoodDark))
        add(c(1, 8, 6, 14, 6, 1, P.WoodLight))
        val crates = listOf(P.Carrot, P.Purple, P.Yellow)
        crates.forEachIndexed { k, color ->
            val x = 2 + k * 4
            add(c(x, 9, 7, 4, 4, 1, P.WoodDark)); add(c(x, 9, 8, 4, 4, 1, color))
            add(c(x + 1, 10, 9, 2, 2, 1, color))
            if (color == P.Carrot) { add(c(x + 1, 9, 9, 1, 1, 1, P.Leaf)); add(c(x + 3, 12, 9, 1, 1, 1, P.Leaf)) }
            add(c(x, 4, 8, 4, 2, 2, color)) // the same goods on the shelf
        }
        // Awning slopes from the back posts down to the front.
        for (k in 0 until 7) for (s in 0 until 8) {
            add(c(s * 2, 1 + k * 2, 18 - k, 2, 2, 1, if (s % 2 == 0) P.Red else P.White))
        }
        for (s in 0 until 8) add(c(s * 2, 15, 10, 2, 1, 2, if (s % 2 == 0) P.Red else P.White))
        add(c(2, 14, 8, 1, 1, 2, P.Light, P.Light)); add(c(13, 14, 8, 1, 1, 2, P.Light, P.Light))
    })

    /** 8 x 8 x 10, runs along x: two posts, a rail and a string of lights that glow at night. */
    val lightFence = IsoShape(1, 1, listOf(
        c(0, 2, 0, 3, 3, 11, P.Wood), c(0, 2, 11, 3, 3, 1, P.WoodLight),
        c(1, 5, 8, 1, 1, 1, P.WoodDeep), c(1, 5, 4, 1, 1, 1, P.WoodDeep),
        c(3, 3, 8, 5, 2, 2, P.WoodDark), c(3, 3, 9, 5, 2, 1, P.WoodLight),
        c(3, 4, 6, 1, 1, 2, P.WoodDeep), c(4, 4, 5, 3, 1, 1, P.WoodDeep), c(7, 4, 6, 1, 1, 2, P.WoodDeep),
        c(3, 5, 5, 2, 1, 2, P.Light, P.Light), c(6, 5, 5, 2, 1, 2, P.Light, P.Light),
    ))

    /** A closing post for the end of a [lightFence] run. */
    val fencePost = IsoShape(1, 1, listOf(c(0, 2, 0, 3, 3, 11, P.Wood), c(0, 2, 11, 3, 3, 1, P.WoodLight)))

    /** Flat road paint for a road running along y: kerbs on both sides, a dash in the middle. */
    val laneY = IsoShape(1, 1, listOf(
        c(0, 0, 0, 1, 8, 1, P.RoadKerb), c(7, 0, 0, 1, 8, 1, P.RoadKerb), c(3, 1, 0, 2, 4, 1, P.White),
    ))

    /** Flat white parking box on road cells. */
    val parking = IsoShape(1, 2, listOf(
        c(0, 0, 0, 8, 1, 1, P.White), c(0, 15, 0, 8, 1, 1, P.White),
        c(0, 0, 0, 1, 16, 1, P.White), c(7, 0, 0, 1, 16, 1, P.White),
    ))

    /** A puffy sky cloud; place it with a z offset. */
    val cloud = IsoShape(2, 1, listOf(
        c(0, 2, 0, 16, 4, 2, P.White), c(3, 1, 2, 8, 5, 2, P.White), c(5, 2, 4, 4, 3, 1, P.White),
        c(0, 2, 0, 16, 4, 1, P.CloudShade),
    ))

    /** A tiny floating grass block from the sky in the art; place it with a z offset. */
    val islet = IsoShape(1, 1, listOf(
        c(2, 2, 2, 4, 4, 2, P.GrassC), c(3, 3, 0, 2, 2, 2, P.DirtC),
    ))

    /** Window for a room wall: frame, four panes and a sill. */
    val window = IsoShape(1, 1, listOf(
        c(0, 0, 0, 8, 1, 12, P.WoodDeep),
        c(1, 0, 1, 6, 1, 10, P.Glass), c(1, 0, 7, 6, 1, 4, P.GlassLight),
        c(4, 0, 1, 1, 1, 10, P.WoodDeep), c(1, 0, 6, 6, 1, 1, P.WoodDeep),
        c(0, 0, 0, 8, 2, 1, P.WoodLight),
    ), wallMounted = true)

    /** Wall lantern: bracket and a glowing box. */
    val lantern = IsoShape(1, 1, listOf(
        c(3, 0, 8, 2, 3, 1, P.WoodDeep), c(4, 2, 7, 1, 1, 1, P.WoodDeep),
        c(2, 1, 1, 5, 3, 6, P.Light, P.Light), c(2, 1, 6, 5, 3, 1, P.WoodDeep), c(2, 1, 0, 5, 3, 1, P.WoodDeep),
        c(4, 1, 1, 1, 3, 5, P.Shade, P.Light),
    ), wallMounted = true)

    /** Corner post for room walls, [h] voxels tall. */
    fun post(h: Int) = IsoShape(1, 1, listOf(c(0, 0, 0, 3, 3, h, P.WoodDark), c(0, 0, h, 3, 3, 1, P.WoodLight)))
}
