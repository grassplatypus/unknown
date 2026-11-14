package com.break8t.unknown.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.break8t.unknown.R
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.DriveMode
import com.break8t.unknown.ui.Route

/** 홈 진입점. P일 때만 S-08과 보상 알림을, 그 밖(DRIVING, UNKNOWN)에는 S-09를 보인다. */
@Composable
fun HomeScreen(mode: DriveMode, onNavigate: (Route) -> Unit) {
    val demo = LocalDemoState.current
    val home = HomeStore.controller
    val context = LocalContext.current
    LaunchedEffect(Unit) { home.attachNavi(context) }
    LaunchedEffect(mode) { home.onMode(mode, demo) }
    val profile by demo.profile.collectAsStateWithLifecycle()

    if (!mode.isParked) {
        val raid by home.raid.state.collectAsStateWithLifecycle()
        DrivingHome(raid, onEnd = home.raid::end, voiceOn = profile.voiceGuideOn, onVoiceChange = { on -> demo.update { it.copy(voiceGuideOn = on) } })
        return
    }
    ParkedHome(profile, onVoiceChange = { on -> demo.update { it.copy(voiceGuideOn = on) } })
    val popups by home.popups.collectAsStateWithLifecycle()
    val openEncyclopedia = { home.dismiss(); onNavigate(Route.ENCYCLOPEDIA) }
    // 공유 화면은 다른 묶음 몫이라 지금은 알림만 닫는다.
    when (popups.firstOrNull()) {
        HomePopup.REWARD -> RewardDialog(home.reward, HomeDemo.SESSION, profile, home::dismiss)
        HomePopup.LEVEL_UP -> LevelUpDialog(profile.level, home::dismiss)
        HomePopup.POSTCARD -> NewItemDialog(
            R.drawable.home_post_blossom,
            stringResource(R.string.home_new_postcard),
            stringResource(R.string.home_demo_postcard),
            onClose = home::dismiss,
            onShare = home::dismiss,
            onOpenEncyclopedia = openEncyclopedia,
            postcard = true,
        )
        HomePopup.JAM_CARD -> NewItemDialog(
            R.drawable.home_card_seongsan,
            stringResource(R.string.home_new_jam_card),
            stringResource(R.string.home_demo_jam_name),
            onClose = home::dismiss,
            onShare = home::dismiss,
            onOpenEncyclopedia = openEncyclopedia,
        )
        null -> Unit
    }
}
