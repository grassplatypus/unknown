package com.break8t.unknown.ui.island

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.break8t.unknown.config.BalanceConfig
import java.time.LocalTime

/**
 * 친구 섬 구경. 내 섬과 같은 장면을 보기만 한다(상점, 보관함, 텃밭, 넓히기 없음).
 * [header]는 부르는 쪽이 넣는 돌아가기 칩과 제목이다. 친구 데이터는 견본이라 텃밭은 내 섬 견본을 쓴다.
 */
@Composable
fun FriendIslandScreen(carId: String, islandLevel: Int, header: @Composable () -> Unit) = Box(Modifier.fillMaxSize()) {
    val now = remember { System.currentTimeMillis() }
    val cfg = BalanceConfig.DEFAULT
    val scene = IslandSceneState(
        night = remember { isNight(realTime = true, manualNight = false, hour = LocalTime.now().hour) },
        focus = SceneFocus.MAIN,
        plots = plotViews(demoPlots(now, cfg.farmGridSize), now),
        carId = carId,
        islandSize = islandSize(islandLevel, cfg),
    )
    IslandScene(scene, Modifier.fillMaxSize())
    Box(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(35.dp)) { header() }
}

@IslandPreviews
@Composable
private fun FriendIslandPreview() = IslandPreviewFrame {
    FriendIslandScreen("car_blue", 1) { com.break8t.unknown.ui.components.ScreenTitle("도토리길의 섬") }
}
