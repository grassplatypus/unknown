package com.break8t.unknown.ui.iso

import androidx.compose.ui.graphics.Color
import com.break8t.unknown.ui.theme.AppColors

/**
 * Scene palette sampled from the Figma illustrations (cam_rot, room_rot, item and character art).
 * Ints are ARGB for the rasterizer's floor and wall textures; Colors are for voxel shapes.
 */
object IsoPalette {
    const val Grass = 0xFFA0C740.toInt()
    const val GrassDark = 0xFF7CA938.toInt()
    const val GrassLight = 0xFFB0CD5A.toInt()
    const val Dirt = 0xFFAF5D37.toInt()
    const val DirtDark = 0xFF8A4A2C.toInt()
    const val Stone = 0xFF8F8B8C.toInt()
    const val StoneDark = 0xFF707067.toInt()
    const val Sand = 0xFFE9D39A.toInt()
    const val SandDark = 0xFFD4B979.toInt()
    const val Soil = 0xFF88512E.toInt()
    const val SoilDark = 0xFF5E3820.toInt()
    const val Water = 0xFF34A1E2.toInt()
    const val WaterLight = 0xFF8FD0F5.toInt()
    const val Road = 0xFF6B6A70.toInt()
    const val RoadLight = 0xFF8A8990.toInt()
    const val Floor = 0xFFD77634.toInt()
    const val FloorGrain = 0xFFC96C31.toInt()
    const val FloorSeam = 0xFFA9572A.toInt()
    const val WallWood = 0xFFD87934.toInt()
    const val WallGrain = 0xFFC1692F.toInt()
    const val WallSeam = 0xFF9A5426.toInt()
    const val WallTop = 0xFFEE8E3D.toInt()
    const val Baseboard = 0xFFB19489.toInt()
    const val BaseboardDark = 0xFF967C76.toInt()
    const val SelectFrame = 0xFFFFFAF0.toInt() // AppColors.Card
    const val GhostOk = 0xFFFFFFFF.toInt()

    /** Behind the scene at night; the scene itself darkens toward this color. */
    val Night = Color(0xFF1B2440)

    internal val T = Color.Transparent
    internal val Ink = AppColors.Text
    internal val Cream = Color(0xFFF4E3C8)
    internal val WoodLight = Color(0xFFD08750)
    internal val Wood = Color(0xFFC07742)
    internal val WoodDark = Color(0xFF8A4F2A)
    internal val WoodDeep = Color(0xFF5D3727)
    internal val Trunk = Color(0xFFAC5D31)
    internal val Leaf = Color(0xFF578730)
    internal val LeafLight = Color(0xFF9EC540)
    internal val LeafDark = Color(0xFF39652B)
    internal val Sofa = Color(0xFF29A343)
    internal val SofaDark = Color(0xFF047344)
    internal val SofaLight = Color(0xFF67B43D)
    internal val PlantLeaf = Color(0xFF32872A)
    internal val PlantLight = Color(0xFF72B922)
    internal val Pot = Color(0xFFAF4429)
    internal val PotRim = Color(0xFFE1743F)
    internal val Roof = Color(0xFFD2473B)
    internal val RoofDark = Color(0xFFBC3A34)
    internal val StoneC = Color(Stone)
    internal val StoneLight = Color(0xFFB9B6A2)
    internal val Glass = Color(0xFF8EC1E9)
    internal val Light = Color(0xFFFFD873)
    internal val Shade = Color(0xFFFFE7A8)
    internal val SoilC = Color(Soil)
    internal val SoilDarkC = Color(SoilDark)
    internal val Sprout = Color(0xFF9BD46B)
    internal val Carrot = Color(0xFFF08A24)
    internal val WaterC = Color(Water)
    internal val Tire = Color(0xFF3A3532)
    internal val Red = Color(0xFFD9483B)
    internal val Blue = Color(0xFF3F7FD0)
    internal val Yellow = Color(0xFFF2B33D)
    internal val Pink = Color(0xFFF0A0A0)
    internal val White = Color(0xFFF7F4EC)
    internal val CloudShade = Color(0xFFE0DDD5)
    internal val Orange = Color(0xFFF2A93B)
    internal val Stripe = Color(0xFFD9822B)
    internal val SproutBody = Color(0xFFB5D96A)
}

enum class PlotStage { EMPTY, SPROUT, GROWING, GROWN }

/** Voxel models for the known ids, VPC voxels per cell edge. Fronts face +y. */
object IsoShapes {
    private val P = IsoPalette

    private fun c(x: Int, y: Int, z: Int, w: Int, d: Int, h: Int, color: Color, glow: Color? = null) =
        Cuboid(x, y, z, w, d, h, color, glow)

    /** 16 x 8 x 7. */
    val sofa = IsoShape(2, 1, listOf(
        c(1, 1, 0, 1, 1, 1, P.WoodDeep), c(14, 1, 0, 1, 1, 1, P.WoodDeep),
        c(1, 6, 0, 1, 1, 1, P.WoodDeep), c(14, 6, 0, 1, 1, 1, P.WoodDeep),
        c(0, 0, 1, 16, 8, 2, P.SofaDark),
        c(2, 2, 3, 12, 6, 1, P.Sofa), c(8, 2, 3, 1, 6, 1, P.SofaDark),
        c(0, 0, 3, 16, 2, 4, P.Sofa), c(0, 0, 6, 16, 2, 1, P.SofaLight),
        c(0, 2, 3, 2, 6, 2, P.Sofa), c(14, 2, 3, 2, 6, 2, P.Sofa),
        c(0, 2, 4, 2, 6, 1, P.SofaLight), c(14, 2, 4, 2, 6, 1, P.SofaLight),
    ))

    /** 8 x 8 x 11. */
    val plant = IsoShape(1, 1, listOf(
        c(2, 2, 0, 4, 4, 3, P.Pot), c(2, 2, 3, 4, 4, 1, P.PotRim), c(3, 3, 3, 2, 2, 1, P.SoilDarkC),
        c(3, 3, 4, 2, 2, 1, P.Trunk),
        c(1, 1, 5, 6, 6, 1, P.LeafDark), c(1, 1, 6, 6, 6, 2, P.PlantLeaf), c(2, 2, 8, 4, 4, 2, P.PlantLight),
        c(3, 3, 10, 2, 2, 1, P.PlantLight),
        c(1, 1, 5, 1, 1, 3, P.T), c(6, 1, 5, 1, 1, 3, P.T), c(1, 6, 5, 1, 1, 3, P.T), c(6, 6, 5, 1, 1, 3, P.T),
        c(1, 3, 7, 1, 1, 1, P.PlantLight), c(4, 6, 6, 1, 1, 1, P.PlantLight), c(6, 4, 7, 1, 1, 1, P.LeafDark),
    ))

    /** 8 x 1 x 6 on the wall, 12 voxels up. */
    val picture = IsoShape(1, 1, listOf(
        c(1, 0, 12, 6, 1, 6, P.Wood),
        c(2, 0, 13, 4, 1, 2, P.PlantLight), c(2, 0, 15, 4, 1, 2, Color(0xFF74BDF7)), c(4, 0, 16, 1, 1, 1, P.White),
    ), wallMounted = true)

    /** 16 x 16 x 1, an oval with diagonal stripes like the item art. */
    val rug = IsoShape(2, 2, buildList {
        for (j in 0 until 16) for (i in 0 until 16) {
            val e = ((i - 7.5f) / 7.5f).let { it * it } + ((j - 7.5f) / 6.5f).let { it * it }
            if (e > 1f) continue
            val color = when {
                e > 0.72f -> P.Wood
                (i + j) / 3 % 2 == 0 -> P.Red
                else -> P.Cream
            }
            add(c(i, j, 0, 1, 1, 1, color))
        }
    })

    /** 8 x 8 x 14; the shade glows at night. */
    val lamp = IsoShape(1, 1, listOf(
        c(2, 2, 0, 4, 4, 1, P.WoodDark), c(3, 3, 1, 2, 2, 8, P.Wood),
        c(1, 1, 9, 6, 6, 4, P.Shade, P.Light), c(1, 1, 9, 6, 6, 1, P.WoodLight, P.Light),
        c(3, 3, 13, 2, 2, 1, P.WoodDark),
    ))

    /** 16 x 8 x 18, open side to +y. */
    val shelf = IsoShape(2, 1, buildList {
        add(c(0, 0, 0, 1, 6, 18, P.WoodDark)); add(c(15, 0, 0, 1, 6, 18, P.WoodDark))
        add(c(1, 0, 0, 14, 1, 17, P.WoodDeep)); add(c(0, 0, 17, 16, 6, 1, P.Wood))
        for (z in intArrayOf(0, 6, 12)) add(c(1, 1, z, 14, 5, 1, P.Wood))
        val books = listOf(P.Red, P.Blue, P.Leaf, P.Yellow)
        for ((row, z) in intArrayOf(1, 7).withIndex()) for (k in 0 until 6) {
            add(c(2 + k * 2, 1, z, if (k % 3 == 2) 1 else 2, 4, 3 + (k + row) % 3, books[(k + row) % books.size]))
        }
        add(c(11, 2, 13, 2, 2, 1, P.Pot)); add(c(11, 2, 14, 2, 2, 2, P.PlantLeaf))
    })

    /** 24 x 24 x 24: stone footing, plank walls, gable roof running along x, chimney. */
    val house = IsoShape(3, 3, buildList {
        add(c(1, 1, 0, 22, 22, 2, P.StoneC))
        add(c(1, 1, 2, 22, 22, 10, P.Wood))
        for (z in intArrayOf(4, 7, 10)) add(c(1, 1, z, 22, 22, 1, P.WoodDark))
        for ((x, y) in listOf(1 to 1, 22 to 1, 1 to 22, 22 to 22)) add(c(x, y, 2, 1, 1, 10, P.WoodDeep))
        add(c(10, 22, 2, 4, 1, 7, P.WoodDeep)); add(c(9, 23, 0, 6, 1, 1, P.StoneLight))
        fun window(x: Int, y: Int, alongX: Boolean) {
            if (alongX) {
                add(c(x, y, 6, 4, 1, 3, P.Glass, P.Light)); add(c(x + 2, y, 6, 1, 1, 3, P.WoodDeep))
            } else {
                add(c(x, y, 6, 1, 4, 3, P.Glass, P.Light)); add(c(x, y + 2, 6, 1, 1, 3, P.WoodDeep))
            }
        }
        window(3, 22, true); window(17, 22, true); window(10, 1, true)
        window(22, 10, false); window(1, 10, false)
        for (l in 0 until 12) {
            add(c(1, l, 12 + l, 22, 24 - 2 * l, 1, if (l % 2 == 0) P.Roof else P.RoofDark))
            if (22 - 2 * l > 0) add(c(1, l + 1, 12 + l, 22, 22 - 2 * l, 1, P.Wood))
        }
        add(c(16, 5, 14, 3, 3, 10, P.StoneC)); add(c(16, 5, 23, 3, 3, 1, P.StoneLight))
    })

    /** 8 x 8, crops on four 2 x 2 spots. [fruit] is the ripe color (carrot by default). */
    fun plot(stage: PlotStage, fruit: Color = P.Carrot): IsoShape = IsoShape(1, 1, buildList {
        add(c(0, 0, 0, 8, 8, 1, P.SoilC))
        add(c(0, 2, 0, 8, 1, 1, P.SoilDarkC)); add(c(0, 5, 0, 8, 1, 1, P.SoilDarkC))
        for ((x, y) in listOf(1 to 1, 5 to 1, 1 to 5, 5 to 5)) when (stage) {
            PlotStage.EMPTY -> Unit
            PlotStage.SPROUT -> { add(c(x, y, 1, 1, 1, 2, P.Sprout)); add(c(x + 1, y, 2, 1, 1, 1, P.Sprout)) }
            PlotStage.GROWING -> { add(c(x, y, 1, 2, 2, 2, P.Leaf)); add(c(x, y, 3, 1, 1, 1, P.LeafLight)) }
            PlotStage.GROWN -> {
                add(c(x, y, 1, 2, 2, 1, fruit)); add(c(x, y, 2, 2, 2, 2, P.Leaf))
                add(c(x, y, 4, 1, 2, 1, P.LeafLight)); add(c(x + 1, y + 1, 4, 1, 1, 1, P.LeafDark))
            }
        }
    })

    private val plots = PlotStage.entries.associateWith { plot(it) }

    /** 8 x 8 x 12, a round canopy over a short trunk. */
    val tree = IsoShape(1, 1, listOf(
        c(3, 3, 0, 2, 2, 5, P.Trunk),
        c(0, 0, 4, 8, 8, 1, P.LeafDark), c(0, 0, 5, 8, 8, 4, P.Leaf), c(1, 1, 9, 6, 6, 2, P.LeafLight),
        c(2, 2, 11, 4, 4, 1, P.LeafLight),
        c(0, 0, 4, 1, 1, 5, P.T), c(7, 0, 4, 1, 1, 5, P.T), c(0, 7, 4, 1, 1, 5, P.T), c(7, 7, 4, 1, 1, 5, P.T),
        c(1, 7, 6, 1, 1, 1, P.LeafLight), c(7, 3, 7, 1, 1, 1, P.LeafLight), c(4, 7, 5, 1, 1, 1, P.LeafDark),
    ))

    /** 8 x 8 x 6, runs along x through the middle. Rotate once to run along y. */
    val fence = IsoShape(1, 1, listOf(
        c(0, 3, 0, 2, 2, 6, P.Wood), c(6, 3, 0, 2, 2, 6, P.Wood),
        c(0, 3, 5, 2, 2, 1, P.WoodLight), c(6, 3, 5, 2, 2, 1, P.WoodLight),
        c(2, 4, 2, 4, 1, 1, P.WoodDark), c(2, 4, 4, 4, 1, 1, P.WoodDark),
    ))

    /** Flat stepping stones. */
    val path = IsoShape(1, 1, listOf(
        c(0, 0, 0, 4, 4, 1, P.StoneLight), c(4, 0, 0, 4, 3, 1, P.StoneC),
        c(0, 4, 0, 3, 4, 1, P.StoneC), c(3, 3, 0, 5, 5, 1, P.StoneLight),
    ))

    /** 16 x 16 x 1 pond with a stone rim and a lily pad. */
    val water = IsoShape(2, 2, listOf(
        c(0, 0, 0, 16, 16, 1, P.StoneC), c(2, 2, 0, 12, 12, 1, P.WaterC), c(5, 6, 0, 2, 2, 1, P.Leaf),
        c(0, 0, 0, 1, 1, 1, P.T), c(15, 0, 0, 1, 1, 1, P.T), c(0, 15, 0, 1, 1, 1, P.T), c(15, 15, 0, 1, 1, 1, P.T),
    ))

    /** 16 x 8 x 8, facing +x. */
    fun car(body: Color): IsoShape = IsoShape(2, 1, listOf(
        c(2, 0, 0, 3, 1, 2, P.Tire), c(11, 0, 0, 3, 1, 2, P.Tire), c(2, 7, 0, 3, 1, 2, P.Tire), c(11, 7, 0, 3, 1, 2, P.Tire),
        c(0, 1, 1, 16, 6, 3, body),
        c(4, 1, 4, 8, 6, 3, P.Glass), c(4, 1, 4, 1, 6, 3, body), c(8, 1, 4, 1, 6, 3, body), c(11, 1, 4, 1, 6, 3, body),
        c(4, 1, 7, 8, 6, 1, body),
        c(15, 1, 2, 1, 1, 1, P.Cream, P.Light), c(15, 6, 2, 1, 1, 1, P.Cream, P.Light),
        c(15, 2, 1, 1, 4, 1, P.StoneC), c(0, 1, 2, 1, 1, 1, P.Red), c(0, 6, 2, 1, 1, 1, P.Red),
    ))

    /** Characters: chunky 8 x 8 figures facing +y. */
    val sprout = IsoShape(1, 1, listOf(
        c(1, 2, 0, 6, 5, 6, P.SproutBody),
        c(1, 2, 0, 1, 5, 1, P.T), c(6, 2, 0, 1, 5, 1, P.T), c(1, 2, 5, 1, 5, 1, P.T), c(6, 2, 5, 1, 5, 1, P.T),
        c(2, 6, 1, 4, 1, 2, P.Cream),
        c(2, 6, 4, 1, 1, 1, P.Ink), c(5, 6, 4, 1, 1, 1, P.Ink), c(3, 6, 3, 2, 1, 1, P.Roof),
        c(1, 6, 3, 1, 1, 1, P.Pink), c(6, 6, 3, 1, 1, 1, P.Pink),
        c(3, 4, 6, 2, 1, 2, P.Leaf), c(1, 4, 7, 2, 1, 2, P.LeafLight), c(5, 4, 7, 2, 1, 2, P.LeafLight),
    ))

    val cat = IsoShape(1, 1, listOf(
        c(2, 3, 0, 4, 3, 4, P.Orange), c(6, 2, 1, 1, 1, 4, P.Stripe),
        c(1, 2, 4, 6, 5, 4, P.Orange), c(1, 2, 4, 6, 5, 1, P.WoodDark),
        c(1, 3, 8, 2, 2, 1, P.Orange), c(5, 3, 8, 2, 2, 1, P.Orange),
        c(3, 2, 7, 2, 1, 1, P.Stripe), c(1, 4, 6, 1, 1, 1, P.Stripe), c(6, 4, 6, 1, 1, 1, P.Stripe),
        c(2, 6, 6, 1, 1, 1, P.Ink), c(5, 6, 6, 1, 1, 1, P.Ink), c(3, 6, 5, 2, 1, 1, P.Cream),
        c(2, 5, 1, 4, 1, 2, P.Cream),
    ))

    val mush = IsoShape(1, 1, listOf(
        c(2, 2, 0, 4, 4, 4, P.Cream),
        c(2, 5, 2, 1, 1, 1, P.Ink), c(5, 5, 2, 1, 1, 1, P.Ink), c(3, 5, 1, 2, 1, 1, P.Pink),
        c(0, 0, 4, 8, 8, 2, P.Roof), c(1, 1, 6, 6, 6, 2, P.Roof),
        c(0, 0, 4, 1, 1, 2, P.T), c(7, 0, 4, 1, 1, 2, P.T), c(0, 7, 4, 1, 1, 2, P.T), c(7, 7, 4, 1, 1, 2, P.T),
        c(2, 2, 7, 2, 2, 1, P.Cream), c(5, 4, 7, 1, 2, 1, P.Cream), c(0, 3, 5, 1, 2, 1, P.Cream),
        c(7, 4, 4, 1, 2, 1, P.Cream), c(3, 7, 4, 2, 1, 1, P.Cream),
    ))

    val cloud = IsoShape(1, 1, listOf(
        c(2, 3, 0, 1, 2, 1, P.Blue), c(5, 3, 0, 1, 2, 1, P.Blue),
        c(1, 2, 1, 6, 5, 4, P.White), c(0, 3, 2, 8, 3, 2, P.White), c(2, 3, 5, 4, 3, 1, P.White),
        c(1, 2, 1, 6, 5, 1, P.CloudShade),
        c(2, 6, 3, 1, 1, 1, P.Ink), c(5, 6, 3, 1, 1, 1, P.Ink), c(1, 6, 2, 1, 1, 1, P.Pink), c(6, 6, 2, 1, 1, 1, P.Pink),
    ))

    /** The abdomen glows at night. */
    val firefly = IsoShape(1, 1, listOf(
        c(2, 2, 0, 4, 4, 3, P.Light, P.Light), c(2, 2, 1, 4, 4, 1, P.Ink),
        c(2, 2, 3, 4, 4, 3, P.Yellow),
        c(2, 5, 4, 1, 1, 1, P.Ink), c(5, 5, 4, 1, 1, 1, P.Ink),
        c(0, 2, 4, 2, 2, 1, P.White), c(6, 2, 4, 2, 2, 1, P.White),
        c(2, 3, 6, 1, 1, 2, P.Ink), c(5, 3, 6, 1, 1, 2, P.Ink), c(2, 3, 8, 1, 1, 1, P.Yellow), c(5, 3, 8, 1, 1, 1, P.Yellow),
    ))

    private val cars = mapOf("car_red" to car(P.Red), "car_blue" to car(P.Blue), "car_yellow" to car(P.Yellow))

    /** Shape for a catalog or island id, or null for ids this library does not know. */
    fun of(id: String): IsoShape? = when (id) {
        "it_sofa" -> sofa
        "it_plant" -> plant
        "it_picture" -> picture
        "it_rug" -> rug
        "it_lamp" -> lamp
        "it_shelf" -> shelf
        "house" -> house
        "farm_empty" -> plots[PlotStage.EMPTY]
        "farm_sprout" -> plots[PlotStage.SPROUT]
        "farm_growing" -> plots[PlotStage.GROWING]
        "farm_grown" -> plots[PlotStage.GROWN]
        "tree" -> tree
        "fence" -> fence
        "path" -> path
        "water" -> water
        "car" -> cars["car_red"]
        "ch_sprout" -> sprout
        "ch_cat" -> cat
        "ch_mush" -> mush
        "ch_cloud" -> cloud
        "ch_firefly" -> firefly
        else -> cars[id]
    }
}
