package com.break8t.unknown.ui.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.ui.components.DialogPanel
import com.break8t.unknown.ui.shop.PreviewFrame
import com.break8t.unknown.ui.shop.ScreenPreviews
import com.break8t.unknown.ui.theme.AppColors

// Preview-only states: no recorder, player or TTS is created here.
private const val NOW = 1_700_000_000_000L
private val books = demoGuestbooks(NOW)
private val mine = books.getValue(GuestbookStore.MY_ISLAND)
private val since = NOW - DAY_MS
private val friend = books.getValue("f_camper") + GuestbookEntry(
    "g_mine", GuestbookStore.ME, "구름농부", "ch_sprout", NOW - 10 * 60 * 1000L, 9, audioPath = "/preview.m4a",
)

private fun playback(sound: GuestbookSound = GuestbookSound.Idle, tts: TtsStatus = TtsStatus.READY) = GuestbookPlayback(sound, tts, NOW)

@Composable
private fun Frame(dialog: (@Composable ColumnScope.() -> Unit)? = null, panel: @Composable () -> Unit) =
    PreviewFrame(dialog) { Box(Modifier.fillMaxSize().background(AppColors.Sky)) { panel() } }

@Composable
private fun My(entries: List<GuestbookEntry> = mine, play: GuestbookPlayback = playback(), dialog: (@Composable ColumnScope.() -> Unit)? = null) =
    Frame(dialog) { MyGuestbookContent(entries, since, play, {}, {}, {}, {}, {}) }

@Composable
private fun Friend(record: RecordUi = RecordUi(RecordStep.IDLE), boardShown: Boolean = true, play: GuestbookPlayback = playback()) = Frame {
    FriendGuestbookContent("f_camper", "주말캠퍼", boardShown, friend, play, record, {}, {}, {}, {}, {}, {}, {}, {})
}

@ScreenPreviews @Composable private fun MyList() = My()
@ScreenPreviews @Composable private fun MyPlaying() = My(play = playback(GuestbookSound.Playing("g_me_1", NOW - 2000, null)))
@ScreenPreviews @Composable private fun MyTtsStarting() = My(play = playback(tts = TtsStatus.STARTING))
@ScreenPreviews @Composable private fun MyTtsUnavailable() = My(play = playback(tts = TtsStatus.UNAVAILABLE))
@ScreenPreviews @Composable private fun MyEmpty() = My(entries = emptyList())
@ScreenPreviews @Composable private fun MyDeleteConfirm() = My(dialog = { DeleteBody(mine[0], {}, {}) })
@ScreenPreviews @Composable private fun MyNotice() = My(dialog = { GuestbookNoticeBody(2, {}, {}) })

@ScreenPreviews @Composable
private fun MyStoreBoardConfirm() = Frame {
    MyGuestbookContent(mine, since, playback(), {}, {}, {}, {}, {})
    Box(Modifier.fillMaxSize().background(AppColors.Scrim)) {
        DialogPanel(
            stringResource(R.string.guestbook_store_title), stringResource(R.string.guestbook_store_body),
            stringResource(R.string.guestbook_store_confirm), {}, {},
        )
    }
}

@ScreenPreviews @Composable private fun FriendIdle() = Friend()
@ScreenPreviews @Composable private fun FriendMicDenied() = Friend(RecordUi(RecordStep.IDLE, micDenied = true))
@ScreenPreviews @Composable private fun FriendRecording() = Friend(RecordUi(RecordStep.RECORDING, secondsLeft = 9))
@ScreenPreviews @Composable private fun FriendRecorded() = Friend(RecordUi(RecordStep.RECORDED, draftSec = 6))
@ScreenPreviews @Composable
private fun FriendReplaying() = Friend(RecordUi(RecordStep.RECORDED, draftSec = 6, playingDraft = true), play = playback(GuestbookSound.Playing(DRAFT_KEY, NOW - 1000, 6000)))
@ScreenPreviews @Composable private fun FriendSaving() = Friend(RecordUi(RecordStep.SAVING))
@ScreenPreviews @Composable private fun FriendMineDeleteConfirm() = Frame(dialog = { DeleteBody(friend.last(), {}, {}) }) {
    FriendGuestbookContent("f_camper", "주말캠퍼", true, friend, playback(), RecordUi(RecordStep.IDLE), {}, {}, {}, {}, {}, {}, {}, {})
}
@ScreenPreviews @Composable private fun FriendBoardOff() = Friend(boardShown = false)
