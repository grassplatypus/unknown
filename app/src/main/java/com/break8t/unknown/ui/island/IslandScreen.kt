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
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.guestbook.GuestbookNoticeDialog
import com.break8t.unknown.ui.guestbook.GuestbookStore
import com.break8t.unknown.ui.guestbook.MyGuestbookPanel
import com.break8t.unknown.ui.guestbook.guestbookOn
import com.break8t.unknown.ui.guestbook.noticeCount
import androidx.compose.runtime.mutableIntStateOf
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** 섬에서 다른 묶음의 화면으로 가는 곳. 연결은 AppShell이 한다. */
enum class IslandPlace { HOUSE, PHOTO, SHOP, INVENTORY, FRIENDS }

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
    val demo = LocalDemoState.current
    val cfg = BalanceConfig.DEFAULT
    var expanding by rememberSaveable { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val cost = expandCost(profile.islandLevel, cfg)
    val night = isNight(profile.islandRealTime, profile.islandNight, hourOf(now))
    val drive = CarDrive(trip.phase, TripRules.progress(trip, frameNow))
    val board = guestbookOn(profile)
    val scene = IslandSceneState(
        night, SceneFocus.MAIN, plotViews(plots, now), profile.selectedCarId, drive, islandSize(profile.islandLevel, cfg), board,
    )
    // Worked out once on entering and saved, so a trip to the shop or a window split neither repeats nor loses it.
    var visitSince by rememberSaveable { mutableStateOf(GuestbookStore.sampleLastVisitMs) }
    var notice by rememberSaveable { mutableIntStateOf(0) }
    var unread by rememberSaveable { mutableStateOf(false) }
    var guestbookOpen by rememberSaveable { mutableStateOf(false) }
    val driving = TripRules.driving(trip)
    Box(Modifier.fillMaxSize()) {
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
                    val mine = GuestbookStore.entries.value[GuestbookStore.MY_ISLAND].orEmpty()
                    visitSince = profile.guestbookLastVisitMs ?: GuestbookStore.sampleLastVisitMs
                    notice = noticeCount(profile, mine, GuestbookStore.sampleLastVisitMs)
                    unread = notice > 0
                    demo.update { it.copy(guestbookLastVisitMs = frameNow) }
                },
                onOpen = onOpen,
                onFarm = { stage = IslandStage.FARM },
                driving = driving,
                onSkip = { trip = TripRules.skip(trip) },
                expandCost = cost.takeUnless { expanded }, // the toast takes the bottom edge in the split window
                onExpand = { expanding = true },
                onBoard = { notice = 0; unread = false; guestbookOpen = true },
                boardBadge = unread,
            )
            IslandStage.FARM -> FarmScreen(night, plots, now, profile, onBack = { stage = IslandStage.ISLAND }, onShop = { onOpen(IslandPlace.SHOP) })
        }
        if (expanded && stage == IslandStage.ISLAND) ExpandedToast(Modifier.align(Alignment.BottomCenter))
        if (stage == IslandStage.ISLAND && board && guestbookOpen) {
            MyGuestbookPanel(visitSince, onClose = { guestbookOpen = false }, onOpenFriend = { onOpen(IslandPlace.FRIENDS) })
        }
        if (stage == IslandStage.ISLAND && board && !driving && !guestbookOpen && notice > 0) {
            GuestbookNoticeDialog(
                notice,
                onOpen = { notice = 0; unread = false; guestbookOpen = true },
                onLater = { notice = 0 },
            )
        }
    }
    if (expanding && cost != null && stage == IslandStage.ISLAND) {
        FarmDialog(onDismiss = { expanding = false }) { m ->
            ExpandPanel(cost, profile.coins, onExpand = {
                var ok = false
                demo.update { p -> expandIsland(p, cfg)?.also { ok = true } ?: p }
                expanding = false
                expanded = ok
            }, onClose = { expanding = false }, modifier = m)
        }
    }
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(TOAST_MS)
            expanded = false
        }
    }
}

/** 섬 넓히기 확인 창. 코인이 모자라면 얼마나 모자란지 보이고 넓히기 버튼을 끈다. */
@Composable
internal fun ExpandPanel(cost: Int, coins: Int, onExpand: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val short = cost - coins
    DialogPanel(
        stringResource(R.string.island_expand_title),
        stringResource(if (short > 0) R.string.island_close else R.string.components_cancel),
        stringResource(R.string.island_expand_confirm),
        onConfirm = onExpand,
        onDismiss = onClose,
        modifier = modifier,
        body = if (short > 0) stringResource(R.string.island_expand_short, short, coins) else stringResource(R.string.island_expand_body),
        header = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Coin()
                Text(stringResource(R.string.island_expand_cost, cost), color = AppColors.Text, fontSize = 47.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmEnabled = short <= 0,
    )
}

@Composable
private fun ExpandedToast(modifier: Modifier) = AppToast(stringResource(R.string.island_expanded), modifier.padding(bottom = 48.dp))

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
    expandCost: Int? = null,
) = IslandStageView(entrance = false, scene, coins, farmReady, onEnter = {}, onOpen, onFarm, driving, onSkip, expandCost)

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
    expandCost: Int? = null,
    onExpand: () -> Unit = {},
    onBoard: () -> Unit = {},
    boardBadge: Boolean = false,
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
        onBoard = if (entrance) ({}) else onBoard,
        boardLabel = if (entrance || driving) null else {
            { IslandChip(stringResource(R.string.guestbook_chip), Modifier.padding(bottom = 16.dp), badge = boardBadge, onClick = onBoard) }
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
        // Bottom right, away from the road that leaves to the bottom left; gone once the island is at its largest.
        if (expandCost != null && !driving) {
            val narrow = LocalConfiguration.current.screenWidthDp < WideLayout.value
            IslandChip(
                stringResource(R.string.island_expand),
                Modifier.align(Alignment.BottomEnd).padding(if (narrow) 24.dp else 35.dp),
                leading = { Coin() },
                onClick = onExpand,
            )
        }
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

private fun previewScene(night: Boolean, level: Int = 0) = IslandSceneState(
    night, SceneFocus.MAIN, plotViews(demoPlots(PREVIEW_NOW, 3), PREVIEW_NOW), Profile().selectedCarId,
    islandSize = islandSize(level, BalanceConfig.DEFAULT),
)

private val previewCost = expandCost(0, BalanceConfig.DEFAULT)

@IslandPreviews
@Composable
private fun EntrancePreview() = IslandPreviewFrame { IslandEntrance(previewScene(false), 240, onEnter = {}) }

@IslandPreviews
@Composable
private fun DayPreview() = IslandPreviewFrame {
    IslandMain(previewScene(false), 240, farmReady = true, onOpen = {}, onFarm = {}, expandCost = previewCost)
}

@IslandPreviews
@Composable
private fun NightPreview() = IslandPreviewFrame {
    IslandMain(previewScene(true), 240, farmReady = true, onOpen = {}, onFarm = {}, expandCost = previewCost)
}

/** 가장 넓힌 섬. 넓히기 칩이 사라진다. */
@IslandPreviews
@Composable
private fun LargestIslandPreview() = IslandPreviewFrame {
    IslandMain(previewScene(false, level = BalanceConfig.DEFAULT.islandSizes.lastIndex), 40, farmReady = true, onOpen = {}, onFarm = {})
}

@Composable
private fun ExpandPreviewBody(coins: Int) = IslandPreviewFrame {
    IslandMain(previewScene(false), coins, farmReady = true, onOpen = {}, onFarm = {}, expandCost = previewCost)
    Box(Modifier.fillMaxSize().background(AppColors.Scrim))
    ExpandPanel(previewCost ?: 0, coins, {}, {}, Modifier.align(Alignment.Center).padding(24.dp))
}

@IslandPreviews
@Composable
private fun ExpandConfirmPreview() = ExpandPreviewBody(240)

@IslandPreviews
@Composable
private fun ExpandShortPreview() = ExpandPreviewBody(40)

@IslandPreviews
@Composable
private fun ExpandedPreview() = IslandPreviewFrame {
    IslandMain(previewScene(false, level = 1), 40, farmReady = true, onOpen = {}, onFarm = {}, expandCost = expandCost(1, BalanceConfig.DEFAULT))
    ExpandedToast(Modifier.align(Alignment.BottomCenter))
}

/** 신월, 초승달, 상현, 보름, 하현, 그믐달. 별은 같고 달만 바뀐다. */
@IslandPreviews
@Composable
private fun MoonPhasesPreview() = IslandPreviewFrame {
    Column(Modifier.fillMaxSize()) {
        for (row in listOf(listOf(0.0, 0.12, 0.25), listOf(0.5, 0.75, 0.88))) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (p in row) PixelSky(true, Modifier.weight(1f).fillMaxHeight().background(sceneSky(true)), phase = p)
            }
        }
    }
}

/** 지난 방문 뒤 새 방명록이 있어서 게시판 칩에 알림 점이 붙은 섬. */
@IslandPreviews
@Composable
private fun BoardNewPreview() = IslandPreviewFrame {
    IslandStageView(false, previewScene(false), 240, farmReady = false, onEnter = {}, onOpen = {}, onFarm = {}, boardBadge = true)
}

/** 게시판을 보관함에 넣어 둔 섬. 게시판과 방명록 칩이 없다. */
@IslandPreviews
@Composable
private fun BoardStoredPreview() = IslandPreviewFrame {
    IslandMain(previewScene(false).copy(board = false), 240, farmReady = true, onOpen = {}, onFarm = {})
}
