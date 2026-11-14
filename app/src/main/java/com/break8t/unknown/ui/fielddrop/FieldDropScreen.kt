package com.break8t.unknown.ui.fielddrop

import com.break8t.unknown.ui.components.ScreenTitle
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.break8t.unknown.ui.components.PixelBusy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.roundToInt
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
import kotlinx.coroutines.delay

// Figma "3. 필드 드랍". Sizes are Figma px x 1.3, see SideRail.kt.
private const val SCALE_BAR_MAX_DP = 120.0 // 좁은 창에서 카드 오른쪽 끝(오른쪽에서 173dp)에 닿지 않는다.
private const val TOAST_MS = 2000L
private val Margin = 35.dp
private val MarkerSize = 109.dp
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
    val carSource = FieldDropStore.car
    DisposableEffect(carSource) {
        carSource.start()
        onDispose { carSource.stop() }
    }
    val car by carSource.fix.collectAsState()
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
        car = car,
        state = state,
        mushrooms = model.mushrooms,
        config = config,
        toast = toast?.let { stringResource(it) },
        onTap = { model.tap(it, car.at, config) },
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
    car: CarFix,
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
    val handle = remember { MapHandle() }
    val preview = LocalInspectionMode.current
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().background(Color(MapPalette.LAND))) {
        val density = LocalDensity.current
        val previewMPerDp = metersPerDp(car.at.lat, defaultZoom(car.at, maxHeight.value.toDouble()))
        val markers = mapMarkers(car, carId, mushrooms, state, config)
        if (preview) {
            PreviewMap()
            val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
            PreviewMarkers(markers, previewProjection(car.at, center, density.density / previewMPerDp))
        } else {
            // 버섯, 광고 글자, 내 차는 지도 레이어(MapMarkers.kt)라서 지도와 같은 프레임에 움직인다.
            LiveMap(handle, car, maxHeight.value.toDouble(), markers, { ids ->
                mushroomAt(ids, mushrooms)?.let(onTap) ?: onDismissCard()
            }, Modifier.fillMaxSize())
        }
        if (!preview && handle.status == MapStatus.LOADING) {
            // Liberty 원래 색이 잠깐 보이지 않도록 다시 칠할 때까지 땅 색으로 덮는다.
            Box(Modifier.fillMaxSize().background(Color(MapPalette.LAND)))
            PixelBusy(Modifier.align(Alignment.Center))
        }

        TopChips(coins, state.todayCount, config.mushroomDailyLimit, Modifier.fillMaxWidth().padding(Margin))

        if (handle.status == MapStatus.OFFLINE) {
            Tag(
                stringResource(R.string.fielddrop_map_offline),
                AppColors.Card,
                Modifier.padding(start = Margin, top = Margin + 99.dp + 24.dp),
                height = 57.dp,
                fontSize = 31.sp,
                padding = 18.dp,
            )
        }

        val selected = state.selectedId?.let { id -> mushrooms.firstOrNull { it.id == id } }
        if (atLimit) {
            LimitCard(config.mushroomDailyLimit, Modifier.align(Alignment.Center))
        } else if (selected != null) {
            MushroomCard(
                selected,
                distanceRoundedM(car.at, selected),
                guiding = state.guidingId == selected.id,
                onGuide = { onGuide(selected) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = Margin, bottom = Margin, end = Margin + LocateSize + 24.dp)
                    .widthIn(max = 550.dp)
                    .fillMaxWidth(),
            )
        }
        val metersPerDp = if (preview) previewMPerDp else handle.metersPerDp(density.density)
        metersPerDp?.let {
            // 현재 위치 버튼 바로 위. 막대는 SCALE_BAR_MAX_DP보다 길지 않아서 카드와 겹치지 않는다.
            ScaleBar(it, Modifier.align(Alignment.BottomEnd).padding(end = 44.dp, bottom = 44.dp + LocateSize + 20.dp))
        }
        LocateButton(onClick = { handle.recenter(car, maxHeight.value.toDouble()) }, Modifier.align(Alignment.BottomEnd).padding(44.dp))
        // OpenStreetMap 이용 조건이라 지우지 않는다. 카드와 현재 위치 버튼이 닿지 않는 자리다.
        Text(
            stringResource(R.string.fielddrop_map_attribution),
            Modifier.align(Alignment.BottomEnd).padding(end = 44.dp, bottom = 8.dp).background(AppColors.Card).padding(horizontal = 8.dp),
            color = AppColors.TextSub,
            fontSize = 16.sp,
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

/** 미리보기에는 지도를 띄울 수 없어서 Figma 지도 그림을 깔고, 내 차를 가운데에 둔 평면 투영을 쓴다. */
@Composable
private fun PreviewMap() {
    Image(ImageBitmap.imageResource(R.drawable.fielddrop_map_city), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
}

/** 미리보기 전용. 지도 레이어에 넣는 것과 같은 그림을 같은 기준점에 찍는다. */
@Composable
private fun PreviewMarkers(markers: MapMarkers, project: (GeoPoint) -> Offset) {
    val context = LocalContext.current
    val images = remember(context) { markerImages(context) }
    val dim = remember { Paint().apply { alpha = (DIM_ALPHA * 255).roundToInt() } }
    Canvas(Modifier.fillMaxSize()) {
        val c = drawContext.canvas.nativeCanvas
        markers.mushrooms.forEach { m ->
            val b = images.getValue(m.icon)
            val p = project(m.at)
            c.drawBitmap(b, p.x - b.width / 2f, p.y - b.height, if (m.dim) dim else null)
        }
        val at = project(markers.car)
        val carArt = rotatedCar(images.getValue(markers.carIcon), markers.carRotate)
        c.drawBitmap(carArt, at.x - carArt.width / 2f, at.y - carArt.height / 2f, null)
    }
}

private fun previewProjection(car: GeoPoint, center: Offset, pxPerM: Double): (GeoPoint) -> Offset = { p ->
    val dx = (p.lng - car.lng) * METERS_PER_DEG * cos(Math.toRadians(car.lat))
    val dy = (p.lat - car.lat) * METERS_PER_DEG
    Offset(center.x + (dx * pxPerM).toFloat(), center.y - (dy * pxPerM).toFloat())
}

private const val METERS_PER_DEG = 111_320.0

/** 픽셀 그림은 보간 없이 키우고 줄인다. */
@Composable
private fun pixelPainter(@DrawableRes id: Int): Painter =
    BitmapPainter(ImageBitmap.imageResource(id), filterQuality = FilterQuality.None)

@Composable
private fun TopChips(coins: Int, today: Int, limit: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        ScreenTitle(stringResource(R.string.route_field_drop))
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
            .background(AppColors.Coin)
            .border(size / 20, AppColors.CoinBorder, CircleShape),
    )
}

@Composable
private fun AdTag(height: Dp, fontSize: TextUnit, padding: Dp) =
    Tag(stringResource(R.string.fielddrop_ad), AppColors.Accent, height = height, fontSize = fontSize, padding = padding, shape = PillShape)

@DrawableRes
private fun mushIcon(kind: MushroomKind): Int =
    if (kind == MushroomKind.GOLD) R.drawable.fielddrop_mush_gold else R.drawable.fielddrop_mush_blue

/** 픽셀 그림처럼 모서리가 각진 축척 막대. 줌을 바꾸면 눈금과 길이를 다시 고른다. */
@Composable
private fun ScaleBar(metersPerDp: Double, modifier: Modifier) {
    val (meters, lengthDp) = scaleBar(metersPerDp, SCALE_BAR_MAX_DP)
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(scaleLabel(meters), color = AppColors.Text, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .width(lengthDp.dp)
                .height(13.dp)
                .background(AppColors.Card)
                .border(BorderWidth, AppColors.Line),
        )
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
private fun MushroomCard(m: Mushroom, distanceM: Int, guiding: Boolean, onGuide: () -> Unit, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(29.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
            if (m.ad) AdTag(52.dp, 29.sp, 16.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                Image(pixelPainter(mushIcon(m.kind)), null, Modifier.size(MarkerSize))
                Column {
                    Text(m.place, color = AppColors.Text, fontSize = 47.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.fielddrop_distance, distanceText(distanceM), minutesFor(distanceM)),
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
            .background(AppColors.Scrim)
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
            Image(pixelPainter(mushIcon(m.kind)), null, Modifier.size(169.dp))
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
                    if (seed == "seed_carrot") Image(pixelPainter(R.drawable.fielddrop_seed_carrot), null, Modifier.size(83.dp))
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
                coins, "car_red", FieldDropSamples.CAR, state, FieldDropSamples.mushrooms, BalanceConfig.DEFAULT,
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
