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
import com.break8t.unknown.ui.photo.ShareHandler

/** 홈 진입점. P일 때만 S-08과 보상 알림을, 그 밖(DRIVING, UNKNOWN)에는 S-09를 보인다. 알림의 공유는 [onShare]로 공유 QR 화면을 연다. */
@Composable
fun HomeScreen(mode: DriveMode, onNavigate: (Route) -> Unit, onShare: ShareHandler = { _, _, _, _ -> }) {
    val demo = LocalDemoState.current
    val home = HomeStore.controller
    val context = LocalContext.current
    LaunchedEffect(Unit) { home.attachNavi(context, demo) }
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
    val postcardName = stringResource(R.string.home_demo_postcard)
    val postcardNote = stringResource(R.string.home_demo_postcard_note)
    val jamName = stringResource(R.string.home_demo_jam_name)
    val jamRoad = stringResource(R.string.home_demo_road)
    // 알림을 먼저 넘겨야 공유 화면을 닫았을 때 같은 알림이 다시 뜨지 않고 다음 알림이 뜬다.
    when (popups.firstOrNull()) {
        HomePopup.REWARD -> RewardDialog(home.reward, HomeDemo.SESSION, profile, home::dismiss)
        HomePopup.LEVEL_UP -> LevelUpDialog(profile.level, home::dismiss)
        HomePopup.POSTCARD -> NewItemDialog(
            R.drawable.home_post_blossom,
            stringResource(R.string.home_new_postcard),
            postcardName,
            onClose = home::dismiss,
            onShare = { home.dismiss(); onShare(true, postcardName, postcardNote, R.drawable.home_post_blossom) },
            onOpenEncyclopedia = openEncyclopedia,
            postcard = true,
        )
        HomePopup.JAM_CARD -> NewItemDialog(
            R.drawable.home_card_seongsan,
            stringResource(R.string.home_new_jam_card),
            jamName,
            onClose = home::dismiss,
            onShare = { home.dismiss(); onShare(false, jamName, jamRoad, R.drawable.home_card_seongsan) },
            onOpenEncyclopedia = openEncyclopedia,
        )
        null -> Unit
    }
}
