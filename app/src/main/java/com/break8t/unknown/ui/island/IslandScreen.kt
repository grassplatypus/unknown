package com.break8t.unknown.ui.island

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
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
    var trip by rememberSaveable(stateSaver = TripSaver) { mutableStateOf(Trip(TripPhase.PARKED)) }
    var frameNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Frame clock for the drive in; the car's place is a pure function of the time since it started.
    LaunchedEffect(trip) {
        while (TripRules.driving(trip)) {
            withFrameMillis { }
            frameNow = System.currentTimeMillis()
            trip = TripRules.tick(trip, frameNow)
        }
    }
    val night = isNight(profile.islandRealTime, profile.islandNight, hourOf(now))
    val drive = CarDrive(trip.phase, TripRules.progress(trip, frameNow))
    val scene = IslandSceneState(night, SceneFocus.MAIN, plotViews(plots, now), profile.selectedCarId, drive)
    when (stage) {
        IslandStage.ENTRANCE, IslandStage.ISLAND -> IslandStageView(
            entrance = stage == IslandStage.ENTRANCE,
            scene,
            profile.coins,
            farmReady = scene.plots.any { it.grown },
            onEnter = {
                frameNow = System.currentTimeMillis()
                trip = TripRules.enter(frameNow)
                stage = IslandStage.ISLAND
            },
            onOpen = onOpen,
            onFarm = { stage = IslandStage.FARM },
            driving = TripRules.driving(trip),
            onSkip = { trip = TripRules.skip(trip) },
        )
        IslandStage.FARM -> FarmScreen(night, plots, now, profile, onBack = { stage = IslandStage.ISLAND }, onShop = { onOpen(IslandPlace.SHOP) })
    }
}

/**
 * Leaving P covers the island and drops this screen, so the drive does not hold anything up; when the screen
 * comes back (P again, or a window change) the drive is over and the car is parked.
 */
private val TripSaver = listSaver<Trip, Any>(
    save = { listOf(it.phase.name, it.startMs) },
    restore = { TripRules.interrupted(Trip(TripPhase.valueOf(it[0] as String), it[1] as Long)) },
)

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
internal fun IslandEntrance(scene: IslandSceneState, coins: Int, onEnter: () -> Unit) =
    IslandStageView(entrance = true, scene, coins, farmReady = false, onEnter = onEnter, onOpen = {}, onFarm = {})

/** S-12-a, S-12-b. 낮밤은 scene.night로 장면이 그린다. */
@Composable
internal fun IslandMain(
    scene: IslandSceneState,
    coins: Int,
    farmReady: Boolean,
    onOpen: (IslandPlace) -> Unit,
    onFarm: () -> Unit,
    driving: Boolean = false,
    onSkip: () -> Unit = {},
) = IslandStageView(entrance = false, scene, coins, farmReady, onEnter = {}, onOpen, onFarm, driving, onSkip)

/**
 * The entrance and the island share one scene, so entering keeps the rendered island on screen and the car
 * starts driving at once instead of after a fresh first render.
 */
@Composable
internal fun IslandStageView(
    entrance: Boolean,
    scene: IslandSceneState,
    coins: Int,
    farmReady: Boolean,
    onEnter: () -> Unit,
    onOpen: (IslandPlace) -> Unit,
    onFarm: () -> Unit,
    driving: Boolean = false,
    onSkip: () -> Unit = {},
) = Box(Modifier.fillMaxSize()) {
    val open: (IslandSpot) -> Unit = { spot ->
        when (spot) {
            IslandSpot.HOUSE -> onOpen(IslandPlace.HOUSE)
            IslandSpot.SHOP -> onOpen(IslandPlace.SHOP)
            IslandSpot.FARM -> onFarm()
        }
    }
    // S-11-a: the car is still on the road in, so the parking space is empty.
    IslandScene(
        if (entrance) scene.copy(drive = null) else scene, Modifier.fillMaxSize(),
        onSpot = if (entrance) { _ -> } else open,
        spotLabel = if (entrance || driving) null else { spot ->
            val text = when (spot) {
                IslandSpot.HOUSE -> R.string.island_house
                IslandSpot.SHOP -> R.string.island_shop
                IslandSpot.FARM -> R.string.island_farm
            }
            // Chips stand on their objects (Figma S-12): a gap above the roof, below the farm's front edge.
            val gap = if (spot == IslandSpot.FARM) Modifier.padding(top = 12.dp) else Modifier.padding(bottom = 16.dp)
            IslandChip(stringResource(text), gap, badge = spot == IslandSpot.FARM && farmReady, onClick = { open(spot) })
        },
    )
    if (entrance) {
        IslandTopBar(coins, start = { ScreenTitle(stringResource(R.string.island_entrance_title)) })
        PrimaryButton(
            stringResource(R.string.island_enter),
            onEnter,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp).widthIn(min = 520.dp),
        )
    } else {
        IslandTopBar(
            coins,
            start = { ScreenTitle(stringResource(R.string.island_title)) },
            end = {
                IslandChip(stringResource(R.string.island_shop), icon = R.drawable.island_ic_shop, onClick = { onOpen(IslandPlace.SHOP) })
                IslandChip(stringResource(R.string.island_inventory), icon = R.drawable.island_ic_box, onClick = { onOpen(IslandPlace.INVENTORY) })
                IslandChip(stringResource(R.string.island_photo), icon = R.drawable.island_ic_cam, onClick = { onOpen(IslandPlace.PHOTO) })
            },
        )
    }
    // While the car drives, any tap ends the drive instead of reaching the chips underneath.
    if (driving) {
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture { awaitFirstDown().consume(); onSkip() }
            },
        )
    }
}

/** 위쪽 칩 줄. 창이 좁아도 한 줄로 두고, 다 들어가지 않으면 줄 전체를 같은 비율로 줄인다. */
@Composable
internal fun IslandTopBar(coins: Int, start: @Composable RowScope.() -> Unit, end: @Composable RowScope.() -> Unit = {}) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= WideLayout
        Row(
            Modifier.fillMaxWidth().padding(if (wide) 35.dp else 24.dp).shrinkToFit(),
            Arrangement.spacedBy(if (wide) 35.dp else 16.dp),
            Alignment.CenterVertically,
        ) {
            start()
            Spacer(Modifier.weight(1f))
            end()
            IslandChip(coins.toString(), leading = { Coin() })
        }
    }
}

private fun Modifier.shrinkToFit() = layout { m, c ->
    val need = m.maxIntrinsicWidth(c.maxHeight)
    if (!c.hasBoundedWidth || need <= c.maxWidth) {
        val p = m.measure(c)
        return@layout layout(p.width, p.height) { p.place(0, 0) }
    }
    val p = m.measure(c.copy(minWidth = 0, maxWidth = need))
    val k = c.maxWidth.toFloat() / p.width
    layout(c.maxWidth, (p.height * k).roundToInt()) {
        p.placeWithLayer(0, 0) {
            scaleX = k
            scaleY = k
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

@Composable
private fun Coin() = Box(
    Modifier.size(44.dp).clip(CircleShape).background(AppColors.Coin).border(4.dp, AppColors.CoinBorder, CircleShape),
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
    val narrow = LocalConfiguration.current.screenWidthDp < WideLayout.value
    Row(
        modifier
            .heightIn(min = 99.dp)
            .clip(ControlShape)
            .background(if (selected) AppColors.Selected else AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = if (narrow) 22.dp else 31.dp),
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
