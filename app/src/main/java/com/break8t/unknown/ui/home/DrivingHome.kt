package com.break8t.unknown.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.theme.AppColors

private enum class DrivingDialog { NONE, PEOPLE, QR }

/** Figma S-09 홈 (주행 중). 움직이는 연출 없이 상태가 바뀔 때만 다시 그린다. */
@Composable
internal fun DrivingHome(
    raid: RaidState,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    config: BalanceConfig = BalanceConfig.DEFAULT,
) {
    // 레이드 상태가 바뀌면 그 상태에서 연 창(모두 보기, 동승자 QR)은 닫는다.
    var dialog by remember(raid::class) { mutableStateOf(DrivingDialog.NONE) }
    val inRaid = raid is RaidState.Solo || raid is RaidState.Group
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= WideMin
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 34.fdp, vertical = 27.fdp),
            verticalArrangement = Arrangement.spacedBy(27.fdp),
        ) {
            TopBar(
                showQr = inRaid,
                showEnd = raid !is RaidState.None,
                onQr = { dialog = DrivingDialog.QR },
                onEnd = onEnd,
            )
            when (raid) {
                RaidState.None -> NoRaid()
                else -> TwoColumns(
                    wide,
                    left = { if (raid is RaidState.Passed) PassedInfo(raid, wide) else RaidInfo(wide) },
                    right = { RaidSide(raid, config.raidVisibleAvatars) { dialog = DrivingDialog.PEOPLE } },
                )
            }
        }
    }
    val people = (raid as? RaidState.Group)?.participants ?: (raid as? RaidState.Passed)?.participants
    when {
        dialog == DrivingDialog.PEOPLE && people != null -> PeopleDialog(people) { dialog = DrivingDialog.NONE }
        dialog == DrivingDialog.QR -> PassengerQrDialog { dialog = DrivingDialog.NONE }
    }
}

@Composable
private fun TopBar(showQr: Boolean, showEnd: Boolean, onQr: () -> Unit, onEnd: () -> Unit) = FlowRow(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(27.fdp),
    verticalArrangement = Arrangement.spacedBy(12.fdp),
    itemVerticalAlignment = Alignment.CenterVertically,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.fdp)) {
        Box(Modifier.size(48.fdp).clip(RoundedCornerShape(10.fdp)).background(AppColors.Success))
        HomeText(stringResource(R.string.app_name), 34, weight = FontWeight.Bold)
    }
    Spacer(Modifier.weight(1f))
    if (showQr) SecondaryButton(stringResource(R.string.home_passenger_qr), onQr)
    Tag(AppColors.Accent, Modifier.widthIn(min = 220.fdp)) {
        HomeText(stringResource(R.string.home_chip_driving), 30, weight = FontWeight.Bold)
    }
    if (showEnd) SecondaryButton(stringResource(R.string.home_end), onEnd)
}

/** S-09-a. 종료 버튼이 없다. */
@Composable
private fun NoRaid() = Column {
    HomeText(
        stringResource(R.string.home_today_driving, HomeDemo.TODAY_KM_DRIVING, HomeDemo.TODAY_MIN_DRIVING),
        40,
        Modifier.padding(top = 20.fdp),
    )
    Box(Modifier.padding(vertical = 30.fdp).fillMaxWidth().height(2.fdp).background(AppColors.Muted))
    Row {
        BigStat(stringResource(R.string.home_jams_passed), stringResource(R.string.home_places, HomeDemo.DRIVING_JAMS))
        BigStat(stringResource(R.string.home_safety_score), stringResource(R.string.home_points, HomeDemo.DRIVING_SAFETY))
    }
}

@Composable
private fun BigStat(label: String, value: String) = Column(Modifier.width(260.fdp)) {
    HomeText(label, 26, color = AppColors.TextSub)
    HomeText(value, 48, Modifier.padding(top = 6.fdp), weight = FontWeight.Bold)
}

@Composable
private fun TwoColumns(wide: Boolean, left: @Composable ColumnScope.() -> Unit, right: @Composable ColumnScope.() -> Unit) {
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(26.fdp)) {
            Column(Modifier.weight(560f).padding(top = 20.fdp), content = left)
            Column(Modifier.weight(497f), content = right)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(40.fdp)) {
            Column(content = left)
            Column(content = right)
        }
    }
}

/** S-09-b, S-09-c 왼쪽: 지나는 중인 정체와 남은 거리. */
@Composable
private fun ColumnScope.RaidInfo(wide: Boolean) {
    HomeText(
        stringResource(R.string.home_regular_jam, HomeDemo.JAM_MEET_COUNT),
        30,
        color = AppColors.Error,
        weight = FontWeight.Bold,
    )
    HomeText(stringResource(R.string.home_demo_road), if (wide) 84 else 64, Modifier.padding(top = 6.fdp), weight = FontWeight.Bold)
    HomeText(stringResource(R.string.home_demo_segment), 36, Modifier.padding(top = 20.fdp), color = AppColors.TextSub)
    ProgressTrack(HomeDemo.RAID_PROGRESS, Modifier.padding(top = 31.fdp))
    Row(Modifier.fillMaxWidth().padding(top = 12.fdp), verticalAlignment = Alignment.CenterVertically) {
        HomeText(stringResource(R.string.home_remaining_km, HomeDemo.RAID_REMAINING_KM), 40, Modifier.weight(1f), weight = FontWeight.Bold)
        HomeText(stringResource(R.string.home_usual_minutes, HomeDemo.RAID_USUAL_MIN), 32, color = AppColors.TextSub)
    }
}

@Composable
private fun ProgressTrack(fraction: Float, modifier: Modifier) = BoxWithConstraints(modifier.fillMaxWidth().height(48.fdp)) {
    val marker = 48.fdp
    val bar = Modifier.height(32.fdp).clip(RoundedCornerShape(16.fdp)).align(Alignment.CenterStart)
    Box(bar.fillMaxWidth().background(AppColors.Muted))
    Box(bar.fillMaxWidth(fraction).background(AppColors.Error))
    val x = (maxWidth * fraction - marker / 2).coerceIn(0.fdp, maxWidth - marker)
    Box(
        Modifier
            .offset(x = x)
            .size(marker)
            .clip(CircleShape)
            .background(AppColors.Card)
            .border(5.fdp, AppColors.Error, CircleShape),
    )
}

/** S-09-d 왼쪽: 지난 정체와 받을 코인. */
@Composable
private fun ColumnScope.PassedInfo(raid: RaidState.Passed, wide: Boolean) {
    HomeText(
        stringResource(R.string.home_passed_title, HomeDemo.JAM_MEET_COUNT),
        34,
        color = AppColors.Success,
        weight = FontWeight.Bold,
    )
    HomeText(stringResource(R.string.home_demo_road), if (wide) 72 else 60, Modifier.padding(top = 6.fdp), weight = FontWeight.Bold)
    HomeText(stringResource(R.string.home_demo_segment), 34, Modifier.padding(top = 20.fdp), color = AppColors.TextSub)
    Row(Modifier.padding(top = 39.fdp), horizontalArrangement = Arrangement.spacedBy(23.fdp)) {
        ResultCard(stringResource(R.string.home_took_time)) {
            HomeText(durationText(raid.tookSec), 34, weight = FontWeight.Bold)
        }
        ResultCard(stringResource(R.string.home_coins_to_get)) {
            Coin(30)
            HomeText(raid.coins.toString(), 34, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultCard(label: String, value: @Composable () -> Unit) = AppCard(Modifier.widthIn(min = 176.fdp).heightIn(min = 150.fdp)) {
    Column(Modifier.padding(horizontal = 14.fdp, vertical = 18.fdp), verticalArrangement = Arrangement.spacedBy(14.fdp)) {
        HomeText(label, 24, color = AppColors.TextSub)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.fdp)) { value() }
    }
}

/** 오른쪽: 레이드 그림과 함께 지나는 사람. */
@Composable
private fun ColumnScope.RaidSide(raid: RaidState, visibleAvatars: Int, onSeeAll: () -> Unit) {
    val title = when (raid) {
        is RaidState.Group -> stringResource(R.string.home_raid_group, raid.participants.size)
        is RaidState.Passed -> stringResource(R.string.home_passed_with, raid.participants.size)
        else -> stringResource(R.string.home_raid_solo)
    }
    HomeText(title, 30, weight = FontWeight.Bold)
    val shape = RoundedCornerShape(16.fdp)
    Box(
        Modifier
            .padding(top = 12.fdp)
            .fillMaxWidth()
            .aspectRatio(497f / 511f)
            .clip(shape)
            .border(BorderWidth, AppColors.Line, shape),
        contentAlignment = Alignment.Center,
    ) {
        val passed = raid is RaidState.Passed
        Image(
            painterResource(R.drawable.home_jam_raid),
            contentDescription = null,
            Modifier.fillMaxSize().then(if (passed) Modifier.blur(7.fdp) else Modifier),
            contentScale = ContentScale.Crop,
        )
        if (passed) SuccessMark()
    }
    val people = (raid as? RaidState.Group)?.participants ?: (raid as? RaidState.Passed)?.participants
    if (people == null) {
        HomeText(
            stringResource(R.string.home_raid_nobody),
            28,
            Modifier.fillMaxWidth().padding(top = 51.fdp),
            color = AppColors.TextSub,
            align = TextAlign.Center,
        )
    } else {
        ParticipantRow(people, visibleAvatars, onSeeAll)
    }
}

@Composable
private fun SuccessMark() = Box(
    Modifier
        .fillMaxWidth(280f / 497f)
        .aspectRatio(1f)
        .clip(CircleShape)
        .background(AppColors.Card.copy(alpha = 0.92f))
        .border(10.fdp, AppColors.Error, CircleShape),
    contentAlignment = Alignment.Center,
) {
    HomeText(stringResource(R.string.home_success), 96, color = AppColors.Error, weight = FontWeight.Bold)
}

@Composable
private fun ParticipantRow(people: List<Participant>, visible: Int, onSeeAll: () -> Unit) {
    val shape = RoundedCornerShape(12.fdp)
    val tile = Modifier.fillMaxHeight().heightIn(min = 150.fdp).clip(shape)
    Row(
        Modifier.padding(top = 16.fdp).fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.fdp),
    ) {
        people.take(visible).forEachIndexed { i, p ->
            Column(
                Modifier.weight(1f).then(tile).background(AppColors.Card).border(BorderWidth, AppColors.Line, shape).padding(6.fdp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(p, i, 40, Modifier.fillMaxWidth().aspectRatio(1f))
                HomeText(p.name, 22, Modifier.padding(top = 6.fdp), weight = FontWeight.Medium, align = TextAlign.Center)
            }
        }
        val more = people.size - visible
        if (more > 0) {
            Column(
                Modifier
                    .weight(1f)
                    .then(tile)
                    .background(AppColors.Accent)
                    .border(BorderWidth, AppColors.Line, shape)
                    .clickable(role = Role.Button, onClick = onSeeAll)
                    .padding(vertical = 18.fdp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                HomeText(stringResource(R.string.home_more_people, more), 44, weight = FontWeight.Bold)
                HomeText(stringResource(R.string.home_see_all), 22, Modifier.padding(top = 20.fdp), weight = FontWeight.Medium)
            }
        }
    }
}
