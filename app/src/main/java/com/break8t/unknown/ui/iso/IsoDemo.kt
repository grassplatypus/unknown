package com.break8t.unknown.ui.iso

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.break8t.unknown.ui.components.AppChip
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme

/** Sample scenes for the engine demo. Screens build their own from saved state. */
object IsoSamples {
    fun room(): IsoScene = IsoScene(
        width = 5, depth = 5,
        cells = List(25) { Cell(FloorType.WOOD) },
        wallHeight = 4f,
        objects = listOf(
            IsoObject("sofa", IsoShapes.sofa, 0, 0),
            IsoObject("lamp", IsoShapes.lamp, 2, 0),
            IsoObject("shelf", IsoShapes.shelf, 3, 0),
            IsoObject("rug", IsoShapes.rug, 1, 2),
            IsoObject("plant", IsoShapes.plant, 4, 4),
            IsoObject("picture", IsoShapes.picture, 0, 2, wall = WallSide.WEST, z = 1.5f),
            IsoObject("picture2", IsoShapes.picture, 1, 0, wall = WallSide.NORTH, z = 1.5f),
            IsoObject("cat", IsoShapes.cat, 2, 3),
        ),
    )

    /** The farm screen (Figma S-13): 3 x 3 plots, one row per crop, a fence, seen with [FARM_CAMERA]. */
    fun farm(): IsoScene {
        val plots = (0 until 9).map { k ->
            val stage = if (k / 3 == 2) PlotStage.GROWN else PlotStage.entries[k % 3 + 1]
            IsoObject("plot$k", IsoShapes.plot(stage, Crop.entries[k / 3]), 1 + k % 3, 1 + k / 3)
        }
        return IsoScene(5, 5, objects = plots + (1..3).map { IsoObject("fence$it", IsoShapes.fence, it, 0) }, floorDepth = 0.75f)
    }

    /** Front and steep like S-13. Not a photo preset; pass it as IsoSceneView(camera = ...). */
    val FARM_CAMERA = IsoCamera(0f, 55f)

    /** A floating island laid out like Figma S-12-a: house, road, 3 x 3 farm, pond, trees, characters. */
    fun island(): IsoScene {
        val w = 10; val d = 9
        val road = setOf(6 to 4) + (0..6).map { it to 5 }
        val cells = List(w * d) { i ->
            if ((i % w to i / w) in road) Cell(FloorType.ROAD) else Cell(FloorType.GRASS)
        }
        val crops = Crop.entries
        val farm = (0 until 9).map { k ->
            val stage = PlotStage.entries[(k + k / 3) % PlotStage.entries.size]
            IsoObject("plot$k", IsoShapes.plot(stage, crops[k % 3]), 1 + k % 3, 1 + k / 3)
        }
        return IsoScene(
            w, d, cells, floorDepth = 1.75f,
            objects = listOf(
                IsoObject("house", IsoShapes.house, 5, 1),
                IsoObject("car", IsoShapes.of("car_red")!!, 3, 5),
                IsoObject("pond", IsoShapes.water, 1, 6),
                IsoObject("tree1", IsoShapes.tree, 0, 0),
                IsoObject("tree2", IsoShapes.tree, 9, 0),
                IsoObject("tree3", IsoShapes.tree, 9, 4),
                IsoObject("tree4", IsoShapes.tree, 8, 7),
                IsoObject("tree5", IsoShapes.tree, 4, 7),
                IsoObject("sprout", IsoShapes.sprout, 8, 5),
                IsoObject("mush", IsoShapes.mush, 6, 7),
                IsoObject("cloud", IsoShapes.cloud, 0, 3),
                IsoObject("firefly", IsoShapes.firefly, 4, 1),
            ) + farm + (1..3).map { IsoObject("fence$it", IsoShapes.fence, it, 0) },
        )
    }
}

/** Engine demo: both scenes, view presets, rotation, night and editing. Labels are dev-only. */
@Composable
fun IsoDemo(
    startInRoom: Boolean = true,
    preset: ViewPreset = ViewPreset.OBLIQUE,
    night: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var inRoom by remember { mutableStateOf(startInRoom) }
    var room by remember { mutableStateOf(IsoSamples.room()) }
    var island by remember { mutableStateOf(IsoSamples.island()) }
    var edit by remember { mutableStateOf(false) }
    var lastTap by remember { mutableStateOf<IsoHit?>(null) }
    val state = rememberIsoViewState(preset, night)
    val scene = if (inRoom) room else island
    val commit: (Placement) -> Unit = { p -> if (inRoom) room = room.with(p) else island = island.with(p) }

    Column(modifier.fillMaxSize().background(AppColors.Background)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppChip("방", inRoom, { inRoom = true; state.selectedId = null })
            AppChip("섬", !inRoom, { inRoom = false; state.selectedId = null })
            for (v in ViewPreset.entries) AppChip(v.name, state.preset == v, { state.preset = v })
            SecondaryButton("시점 돌리기", { state.rotateView() })
            AppChip("밤", state.night, { state.night = !state.night })
            AppChip("편집", edit, { edit = !edit; state.selectedId = null })
            SecondaryButton("물건 돌리기", { state.rotateSelected(scene, scene::isFree, commit) })
            SecondaryButton("처음 크기", { state.resetView() })
            Text("${state.lastRenderMs} ms  ${lastTap ?: ""}", color = AppColors.TextSub)
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(if (state.night) IsoPalette.Night else AppColors.Sky)) {
            IsoSceneView(
                scene = scene,
                state = state,
                modifier = Modifier.fillMaxSize(),
                editMode = edit,
                onTap = { lastTap = it },
                canPlace = scene::isFree,
                onPlace = commit,
            )
        }
    }
}

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
private annotation class IsoSizes

@IsoSizes @Composable private fun RoomOblique() = AppTheme { IsoDemo(startInRoom = true) }
@IsoSizes @Composable private fun RoomNight() = AppTheme { IsoDemo(startInRoom = true, night = true) }
@IsoSizes @Composable private fun RoomLowFront() = AppTheme { IsoDemo(startInRoom = true, preset = ViewPreset.LOW_FRONT) }
@IsoSizes @Composable private fun IslandOblique() = AppTheme { IsoDemo(startInRoom = false) }
@IsoSizes @Composable private fun IslandNight() = AppTheme { IsoDemo(startInRoom = false, night = true) }
@IsoSizes @Composable private fun IslandTop() = AppTheme { IsoDemo(startInRoom = false, preset = ViewPreset.TOP) }
@IsoSizes @Composable private fun IslandSide() = AppTheme { IsoDemo(startInRoom = false, preset = ViewPreset.SIDE) }

@IsoSizes @Composable
private fun FarmFront() = AppTheme {
    IsoSceneView(
        IsoSamples.farm(), rememberIsoViewState(),
        Modifier.fillMaxSize().background(AppColors.Sky), camera = IsoSamples.FARM_CAMERA,
    )
}
