package com.break8t.unknown.ui.iso

import com.break8t.unknown.ui.island.IslandMap
import com.break8t.unknown.ui.island.demoPlots
import com.break8t.unknown.ui.island.plotViews
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Renders the shipped scenes at the content size of the Figma frames into build/iso-snapshots,
 * for side by side review without an emulator. The assertion only checks something was drawn.
 */
class SceneSnapshotTest {
    private val now = 1_700_000_000_000L
    private val plots = plotViews(demoPlots(now, 3), now)
    private val out = File("build/iso-snapshots").apply { mkdirs() }

    private fun save(name: String, f: IsoFrame, bg: Int, scale: Int = 3) {
        val img = BufferedImage(f.w * scale, f.h * scale, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until f.h * scale) for (x in 0 until f.w * scale) {
            val c = f.pixels[(y / scale) * f.w + x / scale]
            img.setRGB(x, y, if (c ushr 24 == 0) bg else c)
        }
        ImageIO.write(img, "png", File(out, "$name.png"))
        assertTrue(name, f.pixels.any { it ushr 24 != 0 })
    }

    // Figma content area 1144 x 901 px is 1487 x 1171 dp; one buffer pixel is PIXEL_DP.
    private val w = (1487 / PIXEL_DP).toInt()
    private val h = (1171 / PIXEL_DP).toInt()

    @Test
    fun island() {
        val scene = IslandMap.scene(plots, "car_red")
        save("island_day", renderScene(scene, IsoCamera.of(ViewPreset.OBLIQUE, 0), w, h), 0x74BDF7)
        save("island_night", renderScene(scene, IsoCamera.of(ViewPreset.OBLIQUE, 0), w, h, night = true), 0x011043)
        for (p in ViewPreset.entries) save("island_${p.name.lowercase()}_1", renderScene(scene, IsoCamera.of(p, 1), w, h), 0x74BDF7)
        for (size in com.break8t.unknown.config.BalanceConfig.DEFAULT.islandSizes) {
            val big = IslandMap.scene(plots, "car_red", size = size)
            save("island_size_$size", renderScene(big, IsoCamera.of(ViewPreset.OBLIQUE, 0), w, h), 0x74BDF7)
        }
    }

    @Test
    fun room() {
        val cfg = com.break8t.unknown.config.BalanceConfig.DEFAULT
        val layout = com.break8t.unknown.ui.house.DefaultLayout
        for (step in 0..1) {
            val cam = IsoCamera.of(ViewPreset.OBLIQUE, step)
            save("room_$step", renderScene(com.break8t.unknown.ui.house.RoomMap.scene(layout, cfg, cam), cam, w, h), 0x020A12)
        }
    }

    @Test
    fun photo() {
        val cfg = com.break8t.unknown.config.BalanceConfig.DEFAULT
        val poses = listOf("ch_sprout", "pose_wave", "pose_jump", "pose_sit", "pose_side", "pose_back", "ch_cat", "ch_mush")
        val extra = poses.mapIndexed { i, id ->
            com.break8t.unknown.ui.photo.characterObject(
                com.break8t.unknown.ui.house.SceneCharacter(id, 0.1f + (i % 4) * 0.25f, 0.3f + (i / 4) * 0.4f, 0.1f, 0), 5,
            ).copy(id = "c$i")
        }
        val cam = IsoCamera.of(ViewPreset.OBLIQUE, 0)
        save("photo_room", renderScene(com.break8t.unknown.ui.house.RoomMap.scene(emptyList(), cfg, cam, extra), cam, w, h), 0x020A12)
    }

    /** All five characters in every pose (rows: characters, columns: stand and the five poses), as the app builds them. */
    @Test
    fun characters() {
        val ids = IsoCharacters.ids
        val poses = IsoCharacters.poses
        fun char(id: String, pose: String?, x: Float, y: Float, size: Float, grid: Int, base: Float = 1f) =
            com.break8t.unknown.ui.photo.characterObject(
                com.break8t.unknown.ui.house.SceneCharacter(pose ?: id, x, y, size, 0), grid, base, characterId = id,
            )
        val tw = 120; val th = 150; val k = 3
        for ((name, step, night) in listOf(Triple("cam0", 0, false), Triple("cam2", 2, false), Triple("night", 0, true))) {
            val sheet = BufferedImage(tw * k * poses.size, th * k * ids.size, BufferedImage.TYPE_INT_RGB)
            for ((r, id) in ids.withIndex()) for ((col, pose) in poses.withIndex()) {
                val o = char(id, pose, 0.5f, 0.5f, com.break8t.unknown.ui.photo.CharacterSizes[1], 1)
                val tile = IsoScene(1, 1, listOf(Cell(FloorType.GRASS)), listOf(o), floorDepth = 0.12f, frame = FrameBox(0f, 0f, 1f, 1f, -0.12f, 1.9f))
                val f = renderScene(tile, IsoCamera.of(ViewPreset.OBLIQUE, step), tw, th, night = night)
                assertTrue("$id $pose", f.pixels.any { it ushr 24 != 0 })
                for (y in 0 until th * k) for (x in 0 until tw * k) {
                    val c = f.pixels[(y / k) * f.w + x / k]
                    sheet.setRGB(col * tw * k + x, r * th * k + y, if (c ushr 24 == 0) 0x8C9AA6 else c)
                }
            }
            ImageIO.write(sheet, "png", File(out, "characters_sheet_$name.png"))
        }
        // The same models at the smallest photo size on the largest island, on the app's own pixel grid.
        val size = com.break8t.unknown.config.BalanceConfig.DEFAULT.islandSizes.last()
        val island = IslandMap.scene(plots, "car_red", size = size)
        val chars = ids.flatMapIndexed { r, id ->
            poses.mapIndexed { col, pose ->
                char(id, pose, (1.5f + col * 2f) / size, (2f + r * 2.3f) / size, com.break8t.unknown.ui.photo.CharacterSizes[0], size, 1.6f).copy(id = "c$r$col")
            }
        }
        save("characters_island_small", renderScene(island.copy(objects = chars, roads = emptyList()), IsoCamera.of(ViewPreset.OBLIQUE, 0), w, h), 0x74BDF7)
    }

    @Test
    fun farm() {
        save("farm", renderScene(IslandMap.farmScene(plots), IslandMap.FARM_CAMERA, 330, 330), 0x74BDF7)
    }
}
