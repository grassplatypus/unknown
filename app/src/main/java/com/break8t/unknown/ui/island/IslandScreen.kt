package com.break8t.unknown.ui.island

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** 섬에서 다른 묶음의 화면으로 가는 곳. 연결은 AppShell이 한다. */
enum class IslandPlace { HOUSE, PHOTO, SHOP, INVENTORY }

private enum class IslandStage { ENTRANCE, ISLAND, FARM }

private const val TICK_MS = 10_000L

/** 섬 진입점. 입구, 내 섬, 텃밭을 이 안에서 오간다. */
@Composable
fun IslandScreen(onOpen: (IslandPlace) -> Unit) {
    val profile by LocalDemoState.current.profile.collectAsState()
    val plots by FarmStore.plots.collectAsState()
    val now = rememberNow()
    var stage by rememberSaveable { mutableStateOf(IslandStage.ENTRANCE) }
    val night = isNight(profile.islandRealTime, profile.islandNight, hourOf(now))
    val scene = IslandSceneState(night, SceneFocus.MAIN, plotViews(plots, now), profile.selectedCarId)
    when (stage) {
        IslandStage.ENTRANCE -> IslandEntrance(scene, profile.coins, onEnter = { stage = IslandStage.ISLAND })
        IslandStage.ISLAND -> IslandMain(
            scene,
            profile.coins,
            farmReady = scene.plots.any { it.grown },
            onOpen = onOpen,
            onFarm = { stage = IslandStage.FARM },
        )
        IslandStage.FARM -> FarmScreen(night, plots, now, profile, onBack = { stage = IslandStage.ISLAND }, onShop = { onOpen(IslandPlace.SHOP) })
    }
}

private fun hourOf(epochMs: Long) = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).hour

/** 남은 시간과 낮밤이 화면을 연 채로도 바뀌도록 시계를 주기적으로 다시 읽는다. */
@Composable
private fun rememberNow(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    return now
}

/** S-11-a. */
@Composable
internal fun IslandEntrance(scene: IslandSceneState, coins: Int, onEnter: () -> Unit) = Box(Modifier.fillMaxSize()) {
    IslandScene(scene, Modifier.fillMaxSize())
    IslandTopBar(coins, start = { IslandChip(stringResource(R.string.island_entrance_title), selected = true) })
    PrimaryButton(
        stringResource(R.string.island_enter),
        onEnter,
        Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp).widthIn(min = 520.dp),
    )
}

/** S-12-a, S-12-b. 낮밤은 scene.night로 장면이 그린다. */
@Composable
internal fun IslandMain(scene: IslandSceneState, coins: Int, farmReady: Boolean, onOpen: (IslandPlace) -> Unit, onFarm: () -> Unit) =
    Box(Modifier.fillMaxSize()) {
        IslandScene(scene, Modifier.fillMaxSize())
        IslandTopBar(
            coins,
            start = { IslandChip(stringResource(R.string.island_title), selected = true) },
            end = {
                IslandChip(stringResource(R.string.island_inventory), icon = R.drawable.island_ic_box, onClick = { onOpen(IslandPlace.INVENTORY) })
                IslandChip(stringResource(R.string.island_photo), icon = R.drawable.island_ic_cam, onClick = { onOpen(IslandPlace.PHOTO) })
            },
        )
        // Figma pins these on the illustration; until the scene exists they sit in one row near the bottom.
        FlowRow(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 35.dp, vertical = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(35.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            IslandChip(stringResource(R.string.island_house), onClick = { onOpen(IslandPlace.HOUSE) })
            IslandChip(stringResource(R.string.island_shop), onClick = { onOpen(IslandPlace.SHOP) })
            IslandChip(stringResource(R.string.island_farm), badge = farmReady, onClick = onFarm)
        }
    }

/** 위쪽 칩 줄. 화면을 나눠 좁아지면 오른쪽 칩이 다음 줄로 내려간다. */
@Composable
internal fun IslandTopBar(coins: Int, start: @Composable FlowRowScope.() -> Unit, end: @Composable FlowRowScope.() -> Unit = {}) {
    FlowRow(
        Modifier.fillMaxWidth().padding(35.dp),
        horizontalArrangement = Arrangement.spacedBy(35.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        start()
        Spacer(Modifier.weight(1f))
        end()
        IslandChip(coins.toString(), leading = { Coin() })
    }
}

@Composable
private fun Coin() = Box(
    Modifier.size(44.dp).clip(CircleShape).background(AppColors.Selected).border(4.dp, AppColors.CoinBorder, CircleShape),
)

/** Figma Chip: 글자 앞에 아이콘이 붙고, 텃밭에는 알림 점이 붙는다. 공용 AppChip은 아이콘을 받지 않는다. */
@Composable
internal fun IslandChip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    @DrawableRes icon: Int? = null,
    badge: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .heightIn(min = 99.dp)
            .clip(ControlShape)
            .background(if (selected) AppColors.Selected else AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 31.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        if (icon != null) Image(painterResource(icon), contentDescription = null, Modifier.size(44.dp))
        Text(text, color = AppColors.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        if (badge) {
            Box(Modifier.size(57.dp).clip(CircleShape).background(AppColors.Selected), Alignment.Center) {
                Text(stringResource(R.string.island_alert), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
internal annotation class IslandPreviews

@Composable
internal fun IslandPreviewFrame(content: @Composable BoxScope.() -> Unit) = AppTheme {
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = Route.ISLAND, onSelect = {})
        Box(Modifier.weight(1f).fillMaxHeight(), content = content)
    }
}

internal const val PREVIEW_NOW = 1_700_000_000_000L

private fun previewScene(night: Boolean) =
    IslandSceneState(night, SceneFocus.MAIN, plotViews(demoPlots(PREVIEW_NOW, 3), PREVIEW_NOW), Profile().selectedCarId)

@IslandPreviews
@Composable
private fun EntrancePreview() = IslandPreviewFrame { IslandEntrance(previewScene(false), 240, onEnter = {}) }

@IslandPreviews
@Composable
private fun DayPreview() = IslandPreviewFrame { IslandMain(previewScene(false), 240, farmReady = true, onOpen = {}, onFarm = {}) }

@IslandPreviews
@Composable
private fun NightPreview() = IslandPreviewFrame { IslandMain(previewScene(true), 240, farmReady = true, onOpen = {}, onFarm = {}) }
