package com.break8t.unknown.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.break8t.unknown.R
import com.break8t.unknown.data.Profile
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.theme.AppColors

// 이보다 좁으면(화면 나눔) 카드를 세로로 쌓는다.
internal val WideMin = 1000.dp
private val CardInner = Modifier.padding(horizontal = 26.fdp, vertical = 18.fdp)

/** Figma S-08 홈 (주차 중). */
@Composable
internal fun ParkedHome(profile: Profile, onVoiceChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= WideMin
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(27.fdp),
            verticalArrangement = Arrangement.spacedBy(20.fdp),
        ) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(27.fdp),
                verticalArrangement = Arrangement.spacedBy(12.fdp),
            ) {
                Tag(AppColors.Selected) { HomeText(stringResource(R.string.route_home), 34, weight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                Tag(AppColors.Accent, Modifier.widthIn(min = 220.fdp)) {
                    HomeText(stringResource(R.string.home_chip_parked), 30, weight = FontWeight.Bold)
                }
                Tag(AppColors.Card) {
                    Coin(34)
                    HomeText(profile.coins.toString(), 34, weight = FontWeight.Bold)
                }
            }
            TodayCard(wide)
            if (wide) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(27.fdp)) {
                    RecentJamCard(Modifier.weight(640f).fillMaxHeight())
                    WeekCard(Modifier.weight(423f).fillMaxHeight())
                }
            } else {
                RecentJamCard(Modifier.fillMaxWidth())
                WeekCard(Modifier.fillMaxWidth())
            }
            VoiceGuideButton(profile.voiceGuideOn, onVoiceChange)
        }
    }
}

@Composable
private fun TodayCard(wide: Boolean) = AppCard(Modifier.fillMaxWidth().heightIn(min = 150.fdp)) {
    val s = HomeDemo.SESSION
    val sentence = @Composable { m: Modifier ->
        HomeText(stringResource(R.string.home_today_parked, HomeDemo.TODAY_KM_PARKED, durationText(s.totalDriveSec)), 34, m)
    }
    val stats = @Composable {
        SmallStat(stringResource(R.string.home_jams_passed), stringResource(R.string.home_places, s.jamSegmentsSec.size))
        SmallStat(stringResource(R.string.home_safety_score), stringResource(R.string.home_points, s.safetyScore))
    }
    if (wide) {
        Row(Modifier.padding(start = 22.fdp, end = 24.fdp), verticalAlignment = Alignment.CenterVertically) {
            sentence(Modifier.weight(1f))
            stats()
        }
    } else {
        Column(Modifier.padding(22.fdp), verticalArrangement = Arrangement.spacedBy(16.fdp)) {
            sentence(Modifier)
            Row { stats() }
        }
    }
}

@Composable
private fun SmallStat(label: String, value: String) = Column(Modifier.width(170.fdp)) {
    HomeText(label, 24, color = AppColors.TextSub)
    HomeText(value, 40, Modifier.padding(top = 8.fdp), weight = FontWeight.Bold)
}

@Composable
private fun RecentJamCard(modifier: Modifier) = AppCard(modifier) {
    Column(CardInner, verticalArrangement = Arrangement.spacedBy(14.fdp)) {
        HomeText(stringResource(R.string.home_recent_jam), 26, color = AppColors.TextSub, weight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(24.fdp)) {
            Image(
                painterResource(R.drawable.home_card_seongsan),
                contentDescription = null,
                Modifier.weight(1f).widthIn(max = 240.fdp).aspectRatio(1.5f).clip(RoundedCornerShape(10.fdp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(10.fdp)) {
                HomeText(stringResource(R.string.home_demo_jam_name), 34, weight = FontWeight.Bold)
                HomeText(stringResource(R.string.home_demo_road), 26, color = AppColors.TextSub)
                HomeText(stringResource(R.string.home_demo_jam_time), 26, color = AppColors.TextSub)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.fdp)) {
            HomeText(
                stringResource(R.string.home_regular_jam, HomeDemo.JAM_MEET_COUNT),
                26,
                color = AppColors.Error,
                weight = FontWeight.Bold,
            )
            HomeText(stringResource(R.string.home_jam_took, durationText(HomeDemo.RAID_JAM_SEC)), 26)
        }
    }
}

@Composable
private fun WeekCard(modifier: Modifier) = AppCard(modifier) {
    Column(CardInner, verticalArrangement = Arrangement.spacedBy(30.fdp)) {
        HomeText(stringResource(R.string.home_this_week), 26, color = AppColors.TextSub, weight = FontWeight.Bold)
        WeekRow(stringResource(R.string.home_jams_passed), stringResource(R.string.home_places, HomeDemo.WEEK_JAMS))
        WeekRow(stringResource(R.string.home_safety_score), stringResource(R.string.home_points, HomeDemo.WEEK_SAFETY))
        WeekRow(stringResource(R.string.home_collected_coins), HomeDemo.WEEK_COINS.toString())
    }
}

@Composable
private fun WeekRow(label: String, value: String) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    HomeText(label, 26, Modifier.weight(1f), color = AppColors.TextSub)
    HomeText(value, 38, weight = FontWeight.Bold)
}

@Composable
private fun VoiceGuideButton(on: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.fdp)
    val content = if (on) AppColors.Text else AppColors.TextSub
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 130.fdp)
            .clip(shape)
            .background(if (on) AppColors.Selected else AppColors.Muted)
            .border(BorderWidth, AppColors.Line, shape)
            .toggleable(on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 26.fdp, vertical = 18.fdp),
        horizontalArrangement = Arrangement.spacedBy(20.fdp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.home_ic_vol), contentDescription = null, Modifier.size(48.fdp), tint = content)
        Column {
            HomeText(stringResource(R.string.home_voice_guide), 34, color = content, weight = FontWeight.Bold)
            HomeText(stringResource(if (on) R.string.components_on else R.string.components_off), 26, color = content)
        }
    }
}
