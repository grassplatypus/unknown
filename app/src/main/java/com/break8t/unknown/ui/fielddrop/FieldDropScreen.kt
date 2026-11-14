package com.break8t.unknown.ui.fielddrop

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.ui.Route
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.components.SideRail
import com.break8t.unknown.ui.theme.AppColors
import com.break8t.unknown.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Figma "3. 필드 드랍". Sizes are Figma px x 1.3, see SideRail.kt.
private const val MAP_PX_W = 2048f
private const val MAP_PX_H = 1360f
private const val MAP_ZOOM = 1.3f // 지도가 창보다 커야 가로세로 모두 끌 수 있다.
private const val TOAST_MS = 2000L
private val Margin = 35.dp
private val MarkerSize = 109.dp
private val CarSize = 114.dp
private val LocateSize = 114.dp
private val PillShape = RoundedCornerShape(percent = 50)

/** 필드 드랍 진입점. 주차 중에만 보이고, 주행 중에는 AppShell이 덮는다. */
@Composable
fun FieldDropScreen() {
    val config = BalanceConfig.DEFAULT
    val demo = LocalDemoState.current
    val profile by demo.profile.collectAsState()
    val model = FieldDropStore.model
    val state by model.state.collectAsState()
    var toast by remember { mutableStateOf<Int?>(null) }
    var toastSeq by remember { mutableIntStateOf(0) }
    LaunchedEffect(toastSeq) {
        if (toast != null) {
            delay(TOAST_MS)
            toast = null
        }
    }
    FieldDropContent(
        coins = profile.coins,
        carId = profile.selectedCarId,
        state = state,
        mushrooms = model.mushrooms,
        config = config,
        toast = toast?.let { stringResource(it) },
        onTap = { model.tap(it, config) },
        onDismissCard = model::dismissCard,
        onGuide = { m ->
            toast = if (model.toggleGuide(m)) R.string.fielddrop_toast_guide else R.string.fielddrop_toast_cancel
            toastSeq++
        },
        onReceive = { model.receive(config, demo) },
        onCloseReward = model::closeReward,
    )
}

@Composable
internal fun FieldDropContent(
    coins: Int,
    carId: String,
    state: FieldDropState,
    mushrooms: List<Mushroom>,
    config: BalanceConfig,
    toast: String?,
    onTap: (Mushroom) -> Unit,
    onDismissCard: () -> Unit,
    onGuide: (Mushroom) -> Unit,
    onReceive: () -> Unit,
    onCloseReward: () -> Unit,
) {
    val atLimit = state.atLimit(config)
    // 지도 가운데에 오는 지점을 지도 비율로 둔다. 창 크기가 바뀌어도 같은 곳을 본다.
    var centerX by rememberSaveable { mutableFloatStateOf(FieldDropSamples.CAR_X) }
    var centerY by rememberSaveable { mutableFloatStateOf(FieldDropSamples.CAR_Y) }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().background(AppColors.Muted)) {
        val vw = maxWidth.value
        val vh = maxHeight.value
        val scale = maxOf(vw / MAP_PX_W, vh / MAP_PX_H) * MAP_ZOOM
        val mapW = MAP_PX_W * scale
        val mapH = MAP_PX_H * scale
        val offX = clampPan(vw / 2 - centerX * mapW, mapW, vw)
        val offY = clampPan(vh / 2 - centerY * mapH, mapH, vh)

        val bitmap = rememberMapBitmap()
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(mapW, mapH, vw, vh) {
                    detectDragGestures { _, drag ->
                        val x = clampPan(vw / 2 - centerX * mapW + drag.x / density, mapW, vw)
                        val y = clampPan(vh / 2 - centerY * mapH + drag.y / density, mapH, vh)
                        centerX = (vw / 2 - x) / mapW
                        centerY = (vh / 2 - y) / mapH
                    }
                }
                .pointerInput(Unit) { detectTapGestures { onDismissCard() } },
        ) {
            // The map node keeps its full size so markers anywhere on it stay inside its hit bounds.
            Box(
                Modifier.layout { measurable, constraints ->
                    val map = measurable.measure(Constraints.fixed(mapW.dp.roundToPx(), mapH.dp.roundToPx()))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        map.place(offX.dp.roundToPx(), offY.dp.roundToPx())
                    }
                },
            ) {
                if (bitmap != null) {
                    Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                }
                mushrooms.forEach { m ->
                    MushroomMarker(
                        m,
                        enabled = !atLimit,
                        onClick = { onTap(m) },
                        modifier = Modifier.offset(mapW.dp * m.x - MarkerSize / 2, mapH.dp * m.y - MarkerSize / 2),
                    )
                }
                CarMarker(
                    carId,
                    Modifier.offset(
                        mapW.dp * FieldDropSamples.CAR_X - CarSize / 2,
                        mapH.dp * FieldDropSamples.CAR_Y - CarSize / 2,
                    ),
                )
            }
        }
        if (bitmap == null) CircularProgressIndicator(Modifier.align(Alignment.Center), color = AppColors.Primary)

        TopChips(coins, state.todayCount, config.mushroomDailyLimit, Modifier.fillMaxWidth().padding(Margin))

        val selected = state.selectedId?.let { id -> mushrooms.firstOrNull { it.id == id } }
        if (atLimit) {
            LimitCard(config.mushroomDailyLimit, Modifier.align(Alignment.Center))
        } else if (selected != null) {
            MushroomCard(
                selected,
                guiding = state.guidingId == selected.id,
                onGuide = { onGuide(selected) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Margin, bottom = Margin, end = Margin + LocateSize + 24.dp)
                    .widthIn(max = 550.dp)
                    .fillMaxWidth(),
            )
        }
        LocateButton(
            onClick = {
                centerX = FieldDropSamples.CAR_X
                centerY = FieldDropSamples.CAR_Y
            },
            Modifier.align(Alignment.BottomEnd).padding(44.dp),
        )

        if (toast != null) {
            // 넓은 창은 Figma처럼 카드 오른쪽, 좁은 창은 카드와 겹치지 않게 위쪽 칩 아래에 둔다.
            val place = if (maxWidth >= 1000.dp) {
                Modifier.align(Alignment.BottomCenter).padding(bottom = 38.dp).offset(x = 62.dp)
            } else {
                Modifier.align(Alignment.TopCenter).padding(top = 280.dp)
            }
            AppToast(toast, place)
        }

        val open = state.openId?.let { id -> mushrooms.firstOrNull { it.id == id } }
        if (open != null) RewardOverlay(open, onReceive, onCloseReward)
    }
}

/** 큰 지도 그림은 디스크에서 읽어 푸는 데 시간이 걸려서 메인 스레드 밖에서 읽는다. */
@Composable
private fun rememberMapBitmap(): ImageBitmap? {
    if (LocalInspectionMode.current) return ImageBitmap.imageResource(R.drawable.fielddrop_map_city)
    val resources = LocalContext.current.resources
    val bitmap by produceState<ImageBitmap?>(null, resources) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(resources, R.drawable.fielddrop_map_city).asImageBitmap()
        }
    }
    return bitmap
}

@Composable
private fun TopChips(coins: Int, today: Int, limit: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Tag(stringResource(R.string.route_field_drop), AppColors.Selected)
        Spacer(Modifier.weight(1f))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Margin, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Tag(stringResource(R.string.fielddrop_today, today, limit), AppColors.Card)
            Tag(coins.toString(), AppColors.Card) { Coin(44.dp) }
        }
    }
}

/** 누를 수 없는 표시용 칩. AppChip은 고르는 칩이라 쓰지 않는다. */
@Composable
private fun Tag(
    text: String,
    fill: Color,
    modifier: Modifier = Modifier,
    height: Dp = 99.dp,
    fontSize: TextUnit = 44.sp,
    padding: Dp = 31.dp,
    shape: Shape = ControlShape,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .height(height)
            .clip(shape)
            .background(fill)
            .border(BorderWidth, AppColors.Line, shape)
            .padding(horizontal = padding),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, color = AppColors.Text, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun Coin(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(AppColors.Selected)
            .border(size / 20, AppColors.CoinBorder, CircleShape),
    )
}

@Composable
private fun AdTag(height: Dp, fontSize: TextUnit, padding: Dp) =
    Tag(stringResource(R.string.fielddrop_ad), AppColors.Accent, height = height, fontSize = fontSize, padding = padding, shape = PillShape)

@DrawableRes
private fun mushIcon(kind: MushroomKind): Int =
    if (kind == MushroomKind.GOLD) R.drawable.fielddrop_mush_gold else R.drawable.fielddrop_mush_blue

@DrawableRes
private fun carIcon(carId: String): Int = when (carId) {
    "car_blue" -> R.drawable.fielddrop_car_blue
    "car_yellow" -> R.drawable.fielddrop_car_yellow
    else -> R.drawable.fielddrop_car_red
}

@Composable
private fun MushroomMarker(m: Mushroom, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.alpha(if (enabled) 1f else 0.45f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(MarkerSize)
                .clip(shape)
                .background(AppColors.Card)
                .border(BorderWidth, AppColors.Line, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = m.place },
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(mushIcon(m.kind)), null, Modifier.size(83.dp))
        }
        if (m.ad) AdTag(42.dp, 23.sp, 13.dp)
    }
}

@Composable
private fun CarMarker(carId: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painterResource(carIcon(carId)),
            null,
            Modifier.size(CarSize).rotate(carRotation(FieldDropSamples.DEMO_HEADING_DEG)),
        )
        Spacer(Modifier.height(5.dp))
        Tag(stringResource(R.string.fielddrop_my_car), AppColors.Card, height = 57.dp, fontSize = 31.sp, padding = 18.dp)
    }
}

@Composable
private fun LocateButton(onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .size(LocateSize)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.fielddrop_ic_locate),
            contentDescription = stringResource(R.string.fielddrop_locate),
            Modifier.size(62.dp),
            tint = Color.Unspecified,
        )
    }
}

@Composable
private fun MushroomCard(m: Mushroom, guiding: Boolean, onGuide: () -> Unit, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(29.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
            if (m.ad) AdTag(52.dp, 29.sp, 16.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                Image(painterResource(mushIcon(m.kind)), null, Modifier.size(MarkerSize))
                Column {
                    Text(m.place, color = AppColors.Text, fontSize = 47.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.fielddrop_distance, distanceText(m.distanceM), m.minutes),
                        color = AppColors.TextSub,
                        fontSize = 31.sp,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (guiding) {
                SecondaryButton(stringResource(R.string.fielddrop_guide_cancel), onGuide, Modifier.fillMaxWidth())
            } else {
                PrimaryButton(stringResource(R.string.fielddrop_guide), onGuide, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LimitCard(limit: Int, modifier: Modifier) {
    AppCard(modifier.padding(horizontal = Margin).widthIn(max = 780.dp).fillMaxWidth().heightIn(min = 286.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.fielddrop_limit, limit),
                Modifier.padding(Margin),
                color = AppColors.Text,
                fontSize = 49.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Window Dialog would sit above AppShell's DrivingCover, so D-08 is drawn in the tree.
@Composable
private fun RewardOverlay(m: Mushroom, onReceive: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Text.copy(alpha = 0.5f))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        RewardPanel(
            m,
            onReceive,
            Modifier
                .padding(24.dp)
                .pointerInput(Unit) { detectTapGestures {} }
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun RewardPanel(m: Mushroom, onReceive: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val color = stringResource(if (m.kind == MushroomKind.GOLD) R.string.fielddrop_gold else R.string.fielddrop_blue)
    Column(
        modifier
            .widthIn(max = 832.dp)
            .clip(shape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, shape)
            .padding(52.dp),
        verticalArrangement = Arrangement.spacedBy(31.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(195.dp).clip(ControlShape).background(AppColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(mushIcon(m.kind)), null, Modifier.size(169.dp))
        }
        Text(
            stringResource(R.string.fielddrop_open_title, color),
            color = AppColors.Text,
            fontSize = 52.sp,
            lineHeight = 70.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.fielddrop_open_body, m.place), color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Margin, Alignment.CenterHorizontally)) {
            RewardTile(stringResource(R.string.fielddrop_coin), m.rewardCoins) { Coin(78.dp) }
            m.rewardSeedId?.let { seed ->
                RewardTile(Catalog.item(seed)?.name ?: seed, 1) {
                    if (seed == "seed_carrot") Image(painterResource(R.drawable.fielddrop_seed_carrot), null, Modifier.size(83.dp))
                }
            }
        }
        PrimaryButton(stringResource(R.string.fielddrop_receive), onReceive, Modifier.fillMaxWidth())
    }
}

@Composable
private fun RewardTile(label: String, qty: Int, icon: @Composable () -> Unit) {
    Column(
        Modifier
            .width(195.dp)
            .heightIn(min = 195.dp)
            .clip(ControlShape)
            .background(AppColors.Background)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(83.dp), contentAlignment = Alignment.Center) { icon() }
        Text(label, color = AppColors.TextSub, fontSize = 29.sp, maxLines = 1)
        Text(stringResource(R.string.fielddrop_qty, qty), color = AppColors.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(name = "기본 창", widthDp = 1682, heightDp = 1210)
@Preview(name = "화면 나눔", widthDp = 841, heightDp = 1210)
private annotation class WindowPreviews

@Composable
private fun PreviewFrame(state: FieldDropState, coins: Int = 240, toast: Int? = null) = AppTheme {
    Row(Modifier.fillMaxSize().background(AppColors.Background)) {
        SideRail(current = Route.FIELD_DROP, onSelect = {})
        Box(Modifier.weight(1f)) {
            FieldDropContent(
                coins, "car_red", state, FieldDropSamples.mushrooms, BalanceConfig.DEFAULT,
                toast?.let { stringResource(it) }, {}, {}, {}, {}, {},
            )
        }
    }
}

@WindowPreviews @Composable
private fun MapPreview() = PreviewFrame(FieldDropState())

@WindowPreviews @Composable
private fun ShopPreview() = PreviewFrame(FieldDropState(selectedId = "shop_bossam"))

@WindowPreviews @Composable
private fun ParkingPreview() = PreviewFrame(FieldDropState(selectedId = "park_sisa"))

@WindowPreviews @Composable
private fun GuidingPreview() = PreviewFrame(
    FieldDropState(selectedId = "shop_bossam", guidingId = "shop_bossam"),
    toast = R.string.fielddrop_toast_guide,
)

@WindowPreviews @Composable
private fun CancelPreview() = PreviewFrame(FieldDropState(selectedId = "shop_bossam"), toast = R.string.fielddrop_toast_cancel)

@WindowPreviews @Composable
private fun LimitPreview() = PreviewFrame(FieldDropState(todayCount = 10), coins = 520)

@WindowPreviews @Composable
private fun RewardPreview() = PreviewFrame(FieldDropState(openId = "shop_cafe"))
