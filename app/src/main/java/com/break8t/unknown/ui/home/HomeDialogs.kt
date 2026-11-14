package com.break8t.unknown.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.break8t.unknown.R
import com.break8t.unknown.ui.collection.POSTCARD_ASPECT
import com.break8t.unknown.data.Profile
import com.break8t.unknown.reward.DriveSession
import com.break8t.unknown.reward.RewardResult
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors

private val DialogShape = RoundedCornerShape(20.fdp)
private val RowShape = RoundedCornerShape(12.fdp)

// 바깥을 직접 그려야 어둡게 칠할 수 있어서, 바깥 터치도 플랫폼 대신 여기서 받는다.
@Composable
private fun HomeDialog(
    onDismiss: () -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) =
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(interactionSource = null, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 720.fdp)
                    .pointerInput(Unit) { detectTapGestures {} } // 카드 안을 누르면 닫히지 않게 터치를 먹는다
                    .clip(DialogShape)
                    .background(AppColors.Card)
                    .border(BorderWidth, AppColors.Line, DialogShape)
                    .verticalScroll(rememberScrollState())
                    .padding(40.fdp),
                verticalArrangement = Arrangement.spacedBy(24.fdp),
                content = content,
            )
            overlay()
        }
    }

@Composable
private fun Title(text: String, align: TextAlign? = null) =
    HomeText(text, 40, Modifier.fillMaxWidth(), weight = FontWeight.Bold, align = align, lineHeight = 54)

/** D-02 주행 보상. 레벨 줄은 보상을 더한 뒤의 값이다. */
@Composable
internal fun RewardDialog(reward: RewardResult, session: DriveSession, profile: Profile, onOk: () -> Unit) =
    HomeDialog(onOk) {
        val jamSec = session.jamSegmentsSec.sum()
        Title(stringResource(R.string.home_reward_title))
        RewardRow(
            stringResource(R.string.home_reward_drive),
            stringResource(R.string.home_reward_drive_sub, durationText(session.totalDriveSec - jamSec)),
            stringResource(R.string.home_plus_coin, reward.driveCoin),
            coin = true,
        )
        RewardRow(
            stringResource(R.string.home_reward_raid),
            stringResource(R.string.home_reward_raid_sub, session.jamSegmentsSec.size, durationText(jamSec)),
            stringResource(R.string.home_plus_coin, reward.raidCoin),
            coin = true,
        )
        RewardRow(
            stringResource(R.string.home_reward_safe),
            stringResource(R.string.home_reward_safe_sub, session.safetyScore),
            stringResource(R.string.home_plus_coin, reward.safeCoin),
            coin = true,
        )
        RewardRow(
            stringResource(R.string.home_reward_xp),
            stringResource(R.string.home_reward_xp_sub, durationText(session.totalDriveSec)),
            stringResource(R.string.home_plus_xp, reward.xp),
            coin = false,
        )
        LevelRow(profile)
        PrimaryButton(stringResource(R.string.home_ok), onOk, Modifier.fillMaxWidth())
    }

@Composable
private fun RewardRow(title: String, sub: String, value: String, coin: Boolean) = Row(
    Modifier
        .fillMaxWidth()
        .heightIn(min = 96.fdp)
        .clip(RowShape)
        .background(AppColors.Background)
        .padding(horizontal = 24.fdp, vertical = 10.fdp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.fdp),
) {
    Column(Modifier.weight(1f)) {
        HomeText(title, 30, weight = FontWeight.Bold)
        HomeText(sub, 24, color = AppColors.TextSub)
    }
    if (coin) Coin(32)
    HomeText(value, 34, weight = FontWeight.Bold)
}

@Composable
private fun LevelRow(profile: Profile) = Row(
    Modifier
        .fillMaxWidth()
        .heightIn(min = 96.fdp)
        .clip(RowShape)
        .background(AppColors.Background)
        .padding(horizontal = 24.fdp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(40.fdp),
) {
    HomeText(stringResource(R.string.home_level, profile.level), 34, weight = FontWeight.Bold)
    val track = RoundedCornerShape(10.fdp)
    Box(Modifier.weight(1f).height(20.fdp).clip(track).background(AppColors.Muted)) {
        val fraction = (profile.xp.toFloat() / profile.xpToNext).coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(AppColors.Success))
    }
    HomeText(stringResource(R.string.home_xp_progress, profile.xp, profile.xpToNext), 26, color = AppColors.TextSub)
}

/** D-03 레벨 업. */
@Composable
internal fun LevelUpDialog(level: Int, onOk: () -> Unit) = HomeDialog(onOk, overlay = { Confetti(Modifier.matchParentSize()) }) {
    Box(Modifier.fillMaxWidth().height(220.fdp), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.home_ch_sprout),
            contentDescription = null,
            Modifier.size(200.fdp).clip(RoundedCornerShape(20.fdp)),
        )
    }
    Title(levelUpText(level), TextAlign.Center)
    PrimaryButton(stringResource(R.string.home_ok), onOk, Modifier.fillMaxWidth())
}

/** D-04 새 엽서, D-05 새 정체 카드. */
@Composable
internal fun NewItemDialog(
    @DrawableRes image: Int,
    title: String,
    name: String,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onOpenEncyclopedia: () -> Unit,
    postcard: Boolean = false,
) = HomeDialog(onClose) {
    Image(
        painterResource(image),
        contentDescription = null,
        if (postcard) Modifier.fillMaxWidth().aspectRatio(POSTCARD_ASPECT) else Modifier.fillMaxWidth().aspectRatio(640f / 300f).clip(RowShape),
        contentScale = if (postcard) ContentScale.Fit else ContentScale.Crop,
    )
    Title(title)
    HomeText(name, 28, Modifier.fillMaxWidth(), color = AppColors.TextSub, lineHeight = 42)
    // 고정 최소 폭 합이 카드 안쪽 폭과 같아서 글자 폭만큼 넘쳐 줄이 바뀌었다. 긴 버튼은 글자만큼, 나머지가 남은 폭을 나눈다.
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(25.fdp)) {
        PrimaryButton(stringResource(R.string.home_close), onClose, Modifier.weight(1f))
        SecondaryButton(stringResource(R.string.home_share), onShare, Modifier.weight(1f))
        SecondaryButton(stringResource(R.string.home_open_encyclopedia), onOpenEncyclopedia)
    }
}

/** D-06 함께 지나는 사람 모두 보기. */
@Composable
internal fun PeopleDialog(people: List<Participant>, onClose: () -> Unit) = HomeDialog(onClose) {
    Title(stringResource(R.string.home_raid_group, people.size))
    val scroll = rememberScrollState()
    Row(Modifier.fillMaxWidth().height(430.fdp)) {
        Column(Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.fdp)) {
            people.forEachIndexed { i, p -> PersonRow(p, i) }
        }
        Spacer(Modifier.width(24.fdp))
        ScrollBar(scroll, Modifier.width(8.fdp).fillMaxHeight())
    }
    PrimaryButton(stringResource(R.string.home_close), onClose, Modifier.fillMaxWidth())
}

@Composable
private fun PersonRow(p: Participant, index: Int) = Row(
    Modifier
        .fillMaxWidth()
        .heightIn(min = 96.fdp)
        .clip(RowShape)
        .background(AppColors.Background)
        .padding(start = 10.fdp, end = 52.fdp, top = 10.fdp, bottom = 10.fdp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(18.fdp),
) {
    Avatar(p, index, 32, Modifier.size(76.fdp))
    HomeText(p.name, 34, Modifier.weight(1f), weight = FontWeight.Medium)
    HomeText(stringResource(R.string.home_level, p.level), 26, color = AppColors.TextSub)
    if (p.friend) HomeText(stringResource(R.string.home_friend), 26, color = AppColors.TextSub)
}

@Composable
private fun ScrollBar(state: ScrollState, modifier: Modifier) = Canvas(modifier) {
    val r = CornerRadius(size.width / 2)
    drawRoundRect(AppColors.Muted, cornerRadius = r)
    val viewport = size.height
    val thumb = viewport * viewport / (viewport + state.maxValue)
    val top = if (state.maxValue == 0) 0f else (viewport - thumb) * state.value / state.maxValue
    drawRoundRect(AppColors.Line, Offset(0f, top), Size(size.width, thumb), r)
}

/** D-07 동승자 QR. 실제 코드는 아직 없어서 스캔되지 않는 견본 무늬를 그린다. */
@Composable
internal fun PassengerQrDialog(onClose: () -> Unit) = HomeDialog(onClose) {
    HomeText(stringResource(R.string.home_passenger_qr), 40, weight = FontWeight.Bold)
    QrPlaceholder(Modifier.padding(vertical = 68.fdp).size(264.fdp).align(Alignment.CenterHorizontally))
    PrimaryButton(stringResource(R.string.home_close), onClose, Modifier.fillMaxWidth())
}

private const val QR_CELLS = 25

@Composable
private fun QrPlaceholder(modifier: Modifier) = Canvas(
    modifier.clip(RoundedCornerShape(8.fdp)).background(Color.White).padding(16.fdp),
) {
    val cell = size.minDimension / QR_CELLS
    fun dot(x: Int, y: Int) = drawRect(AppColors.Text, Offset(x * cell, y * cell), Size(cell, cell))
    val far = QR_CELLS - 7
    // 세 모서리의 위치 표시(7칸)와 그 둘레 한 칸은 무늬를 비운다.
    fun finder(x: Int, y: Int): Pair<Int, Int>? = when {
        x <= 7 && y <= 7 -> 0 to 0
        x >= far - 1 && y <= 7 -> far to 0
        x <= 7 && y >= far - 1 -> 0 to far
        else -> null
    }
    for (y in 0 until QR_CELLS) for (x in 0 until QR_CELLS) {
        val origin = finder(x, y)
        if (origin != null) {
            val dx = x - origin.first
            val dy = y - origin.second
            val ring = dx == 0 || dy == 0 || dx == 6 || dy == 6
            val core = dx in 2..4 && dy in 2..4
            if (dx in 0..6 && dy in 0..6 && (ring || core)) dot(x, y)
        } else if ((x * 7 + y * 13 + x * y) % 3 == 0) {
            dot(x, y)
        }
    }
}
