package com.break8t.unknown.ui.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.break8t.unknown.R
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.config.CropSpec
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.AppToast
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.delay

private const val TOAST_MS = 3_000L
private val WideLayout = 1000.dp
private val DialogShape = RoundedCornerShape(26.dp)

private val seedArt = mapOf(
    "seed_carrot" to R.drawable.island_seed_carrot,
    "seed_potato" to R.drawable.island_seed_potato,
    "seed_wheat" to R.drawable.island_seed_wheat,
)

private fun crop(seedId: String?): CropSpec? = Catalog.crops.firstOrNull { it.seedId == seedId }

/** S-13 텃밭. 빈 칸은 골라서 심고, 다 자란 칸은 누르면 모두 거두고, 자라는 칸은 뽑을지 묻는다. */
@Composable
internal fun FarmScreen(night: Boolean, plots: List<Plot>, now: Long, profile: Profile, onBack: () -> Unit, onShop: () -> Unit) {
    val demo = LocalDemoState.current
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    var seedId by rememberSaveable { mutableStateOf(Catalog.crops.first().seedId) }
    var pullAt by rememberSaveable { mutableIntStateOf(-1) }
    var missingSeed by rememberSaveable { mutableStateOf<String?>(null) }
    var harvested by remember { mutableStateOf<Harvest?>(null) }
    val target = picked.takeIf { plots.getOrNull(it)?.isEmpty == true } ?: plots.indexOfFirst { it.isEmpty }

    fun harvest() {
        val (next, h) = harvestAll(FarmStore.plots.value, System.currentTimeMillis(), Catalog.crops)
        if (h.counts.isEmpty()) return
        FarmStore.plots.value = next
        demo.update { it.copy(coins = it.coins + h.coins) }
        harvested = h
    }

    fun plantPicked() {
        val crop = crop(seedId) ?: return
        if (target < 0) return
        if (!demo.addItem(seedId, -1)) {
            missingSeed = seedId
            return
        }
        FarmStore.plots.value = plant(FarmStore.plots.value, target, crop, System.currentTimeMillis())
    }

    Box(Modifier.fillMaxSize()) {
        FarmContent(
            night, plots, now, profile, target, seedId,
            onBack = onBack,
            onPlot = { i ->
                val p = plots[i]
                when {
                    p.isEmpty -> picked = i
                    p.isGrown(System.currentTimeMillis()) -> harvest() // the ticker can be seconds old
                    else -> pullAt = i
                }
            },
            onSeed = { seedId = it },
            onPlant = ::plantPicked,
        )
        harvested?.let { HarvestToast(it, Modifier.align(Alignment.BottomCenter)) }
    }
    LaunchedEffect(harvested) {
        if (harvested != null) {
            delay(TOAST_MS)
            harvested = null
        }
    }

    plots.getOrNull(pullAt)?.takeIf { it.isGrowing(now) }?.let { plot ->
        FarmDialog(onDismiss = { pullAt = -1 }) { m ->
            PullPanel(plot, now, onPull = {
                FarmStore.plots.value = pull(FarmStore.plots.value, pullAt)
                pullAt = -1
            }, onKeep = { pullAt = -1 }, modifier = m)
        }
    }
    missingSeed?.let { id ->
        FarmDialog(onDismiss = { missingSeed = null }) { m ->
            NoSeedPanel(id, onShop = { missingSeed = null; onShop() }, onClose = { missingSeed = null }, modifier = m)
        }
    }
}

@Composable
internal fun FarmContent(
    night: Boolean,
    plots: List<Plot>,
    now: Long,
    profile: Profile,
    target: Int,
    seedId: String,
    onBack: () -> Unit,
    onPlot: (Int) -> Unit,
    onSeed: (String) -> Unit,
    onPlant: () -> Unit,
) {
    val scene = IslandSceneState(night, SceneFocus.FARM, plotViews(plots, now), profile.selectedCarId)
    Column(Modifier.fillMaxSize().background(sceneSky(night))) {
        IslandTopBar(profile.coins, start = {
            IslandChip(stringResource(R.string.island_back), icon = R.drawable.island_ic_back, onClick = onBack)
            IslandChip(stringResource(R.string.island_farm), selected = true)
        })
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= WideLayout) {
                Row(Modifier.fillMaxSize().padding(start = 35.dp, end = 35.dp, bottom = 35.dp), Arrangement.spacedBy(35.dp)) {
                    FarmScene(scene, plots, now, target, onPlot, Modifier.weight(1f).fillMaxHeight())
                    SeedCard(
                        profile.items, seedId, target >= 0, onSeed, onPlant, fill = true,
                        Modifier.width(490.dp).heightIn(max = 720.dp).fillMaxHeight().align(Alignment.Bottom),
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 35.dp, end = 35.dp, bottom = 35.dp),
                    Arrangement.spacedBy(35.dp),
                ) {
                    FarmScene(scene, plots, now, target, onPlot, Modifier.fillMaxWidth().aspectRatio(1f))
                    SeedCard(profile.items, seedId, target >= 0, onSeed, onPlant, fill = false, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** 칸 그림은 장면이 그리고, 남은 시간과 다 자람 표시는 여기서 칸 위에 얹는다. */
@Composable
private fun FarmScene(scene: IslandSceneState, plots: List<Plot>, now: Long, target: Int, onPlot: (Int) -> Unit, modifier: Modifier) {
    IslandScene(scene, modifier, onPlot, plotOverlay = { i -> plots.getOrNull(i)?.let { PlotOverlay(it, now, picked = i == target) } })
}

@Composable
private fun BoxScope.PlotOverlay(plot: Plot, now: Long, picked: Boolean) {
    if (picked) Box(Modifier.matchParentSize().border(6.dp, AppColors.Selected))
    when {
        plot.isEmpty -> Unit
        plot.isGrown(now) -> {
            FitText(
                stringResource(R.string.island_grown), 23.sp,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).fillMaxWidth(0.6f)
                    .clip(LabelShape).background(AppColors.Selected).border(BorderWidth, AppColors.Line, LabelShape)
                    .padding(4.dp),
            )
            AlertDot(Modifier.align(Alignment.TopEnd).padding(10.dp))
        }
        else -> GrowingLabel(plot, now, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).fillMaxWidth(0.78f))
    }
}

private val LabelShape = RoundedCornerShape(13.dp)

@Composable
private fun GrowingLabel(plot: Plot, now: Long, modifier: Modifier) {
    Column(
        modifier.clip(LabelShape).background(AppColors.Card).border(BorderWidth, AppColors.Line, LabelShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        Arrangement.spacedBy(3.dp),
    ) {
        FitText(stringResource(R.string.island_remaining, durationText(plot.readyAt - now)), 18.sp, Modifier.fillMaxWidth())
        ProgressBar(plot.progress(now), 12.dp)
    }
}

@Composable
private fun ProgressBar(fraction: Float, height: Dp) {
    val shape = RoundedCornerShape(percent = 50)
    Box(Modifier.fillMaxWidth().height(height).progressSemantics(fraction).clip(shape).background(AppColors.Muted)) {
        Box(Modifier.widthIn(min = height).fillMaxWidth(fraction).fillMaxHeight().clip(shape).background(AppColors.Success))
    }
}

@Composable
private fun AlertDot(modifier: Modifier) {
    Box(modifier.size(44.dp).clip(CircleShape).background(AppColors.Error), Alignment.Center) {
        Text(stringResource(R.string.island_alert), color = AppColors.Card, fontSize = 29.sp, fontWeight = FontWeight.Bold)
    }
}

/** 칸이 좁아지는 화면 나눔에서도 한 줄에 들어가도록 글자를 줄인다. */
@Composable
private fun FitText(text: String, maxSize: TextUnit, modifier: Modifier, color: Color = AppColors.Text, bold: Boolean = true) {
    BasicText(
        text,
        modifier,
        style = TextStyle(color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = maxSize),
    )
}

@Composable
private fun SeedCard(
    items: Map<String, Int>,
    seedId: String,
    canPlant: Boolean,
    onSeed: (String) -> Unit,
    onPlant: () -> Unit,
    fill: Boolean,
    modifier: Modifier,
) {
    AppCard(modifier) {
        Column(if (fill) Modifier.fillMaxSize().padding(23.dp) else Modifier.padding(23.dp), Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.island_plant_title), color = AppColors.TextSub, fontSize = 31.sp)
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                Catalog.crops.forEach { c ->
                    SeedCell(c, items[c.seedId] ?: 0, c.seedId == seedId, onClick = { onSeed(c.seedId) }, Modifier.weight(1f))
                }
            }
            if (fill) Spacer(Modifier.weight(1f))
            val name = crop(seedId)?.name
            if (canPlant && name != null) {
                PrimaryButton(stringResource(R.string.island_plant, name), onPlant, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SeedCell(crop: CropSpec, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(if (selected) 6.dp else BorderWidth, if (selected) AppColors.Selected else AppColors.Line, ControlShape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        Arrangement.spacedBy(6.dp),
        Alignment.CenterHorizontally,
    ) {
        seedArt[crop.seedId]?.let {
            Image(painterResource(it), null, Modifier.size(83.dp).alpha(if (count > 0) 1f else 0.4f))
        }
        FitText(
            stringResource(R.string.island_seed_count, crop.name, count), 31.sp, Modifier.fillMaxWidth(),
            color = if (count > 0) AppColors.Text else AppColors.DisabledText,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(painterResource(R.drawable.island_ic_clock), null, Modifier.size(31.dp))
            Text(durationText(crop.growMinutes * 60_000L), color = AppColors.TextSub, fontSize = 29.sp)
        }
    }
}

@Composable
internal fun durationText(ms: Long): String {
    val d = remainingTime(ms)
    return when {
        d.days > 0 && d.hours > 0 -> stringResource(R.string.island_days_hours, d.days, d.hours)
        d.days > 0 -> stringResource(R.string.island_days, d.days)
        d.hours > 0 -> stringResource(R.string.island_hours, d.hours)
        else -> stringResource(R.string.island_minutes, d.minutes)
    }
}

@Composable
private fun HarvestToast(h: Harvest, modifier: Modifier) {
    val what = h.counts.map { (id, n) -> stringResource(R.string.island_harvest_part, crop(id)?.name ?: id, n) }.joinToString(", ")
    val text = stringResource(if (takesReul(h.coins)) R.string.island_harvested_reul else R.string.island_harvested_eul, what, h.coins)
    AppToast(text, modifier.padding(bottom = 48.dp))
}

// AppDialog always labels its dismiss button 취소; D-09 and D-10 need 그대로 두기 and 닫기.
@Composable
private fun FarmDialog(onDismiss: () -> Unit, panel: @Composable (Modifier) -> Unit) {
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) { panel(Modifier.padding(24.dp)) }
}

@Composable
private fun DialogPanel(
    title: String,
    dismissText: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
    body: String? = null,
    header: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.widthIn(max = 832.dp).clip(DialogShape).background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, DialogShape).padding(55.dp),
        Arrangement.spacedBy(31.dp),
    ) {
        header?.invoke()
        Text(title, color = AppColors.Text, fontSize = 52.sp, lineHeight = 70.sp, fontWeight = FontWeight.Bold)
        if (body != null) Text(body, color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(35.dp)) {
            SecondaryButton(dismissText, onDismiss, Modifier.weight(1f))
            PrimaryButton(confirmText, onConfirm, Modifier.weight(1f))
        }
    }
}

/** D-09. 다 자라기 전에 뽑으면 코인도 씨앗도 돌려주지 않는다. */
@Composable
internal fun PullPanel(plot: Plot, now: Long, onPull: () -> Unit, onKeep: () -> Unit, modifier: Modifier = Modifier) {
    val c = crop(plot.seedId)
    DialogPanel(
        stringResource(R.string.island_pull_title),
        stringResource(R.string.island_keep),
        stringResource(R.string.island_pull),
        onConfirm = onPull,
        onDismiss = onKeep,
        modifier = modifier,
        body = stringResource(R.string.island_pull_body),
        header = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(31.dp)) {
                Box(Modifier.size(130.dp).clip(ControlShape).background(AppColors.Muted), Alignment.Center) {
                    seedArt[plot.seedId]?.let { Image(painterResource(it), null, Modifier.size(94.dp)) }
                }
                Column {
                    Text(c?.name.orEmpty(), color = AppColors.Text, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.island_remaining, durationText(plot.readyAt - now)),
                        color = AppColors.TextSub, fontSize = 31.sp,
                    )
                }
            }
        },
    )
}

/** D-10. 상점으로 가기는 상점 묶음으로 넘긴다. */
@Composable
internal fun NoSeedPanel(seedId: String, onShop: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    DialogPanel(
        stringResource(R.string.island_no_seed, crop(seedId)?.name.orEmpty()),
        stringResource(R.string.island_close),
        stringResource(R.string.island_go_shop),
        onConfirm = onShop,
        onDismiss = onClose,
        modifier = modifier,
    )
}

// Previews: fixed clock so the Figma sample labels show; dialogs drawn without a window.
private val previewPlots = demoPlots(PREVIEW_NOW, 3)

@Composable
private fun FarmPreviewBody(at: Alignment = Alignment.Center, overlay: @Composable (Modifier) -> Unit = {}) = IslandPreviewFrame {
    FarmContent(false, previewPlots, PREVIEW_NOW, Profile(), target = 8, seedId = "seed_carrot", {}, {}, {}, {})
    overlay(Modifier.align(at))
}

@Composable
private fun Scrim() = Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

@IslandPreviews
@Composable
private fun FarmPreview() = FarmPreviewBody()

@IslandPreviews
@Composable
private fun HarvestPreview() = FarmPreviewBody(Alignment.BottomCenter) { m -> HarvestToast(Harvest(mapOf("seed_wheat" to 2), 40), m) }

@IslandPreviews
@Composable
private fun PullPreview() = FarmPreviewBody { m ->
    Scrim()
    PullPanel(previewPlots[0], PREVIEW_NOW, {}, {}, m.padding(24.dp))
}

@IslandPreviews
@Composable
private fun NoSeedPreview() = FarmPreviewBody { m ->
    Scrim()
    NoSeedPanel("seed_wheat", {}, {}, m.padding(24.dp))
}
