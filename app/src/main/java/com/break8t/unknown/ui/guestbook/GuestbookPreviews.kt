package com.break8t.unknown.ui.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.break8t.unknown.R
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.drive.UxLimits
import com.break8t.unknown.ui.components.DialogPanel
import com.break8t.unknown.ui.shop.PreviewFrame
import com.break8t.unknown.ui.shop.ScreenPreviews
import com.break8t.unknown.ui.theme.AppColors
import androidx.compose.runtime.CompositionLocalProvider

private const val NOW = 1_700_000_000_000L
private val books = demoGuestbooks(NOW)
private val mine = books.getValue(GuestbookStore.MY_ISLAND)
private val since = NOW - DAY_MS
private val friend = books.getValue("f_camper") + GuestbookEntry(
    "g_mine", GuestbookStore.ME, "구름농부", "ch_sprout", NOW - 10 * 60 * 1000L, "오늘도 무사히 퇴근했어요. 섬 구경 잘하고 가요!",
)

// Previews start from UNKNOWN limits (all blocked), so the write box is shown as parked.
@Composable
private fun Frame(dialog: (@Composable ColumnScope.() -> Unit)? = null, panel: @Composable () -> Unit) =
    CompositionLocalProvider(LocalUxLimits provides UxLimits(false, 0)) {
        PreviewFrame(dialog) { Box(Modifier.fillMaxSize().background(AppColors.Sky)) { panel() } }
    }

@Composable
private fun My(entries: List<GuestbookEntry> = mine, dialog: (@Composable ColumnScope.() -> Unit)? = null) =
    Frame(dialog) { MyGuestbookContent(entries, since, NOW, {}, {}, {}) }

@Composable
private fun Friend(boardShown: Boolean = true, dialog: (@Composable ColumnScope.() -> Unit)? = null) = Frame(dialog) {
    FriendGuestbookContent("주말캠퍼", boardShown, friend, NOW, { it.authorId == GuestbookStore.ME }, {}, {}, {})
}

@ScreenPreviews @Composable private fun MyList() = My()
@ScreenPreviews @Composable private fun MyEmpty() = My(entries = emptyList())
@ScreenPreviews @Composable private fun MyDeleteConfirm() = My(dialog = { DeleteBody(mine[0], {}, {}) })
@ScreenPreviews @Composable private fun MyNotice() = My(dialog = { GuestbookNoticeBody(2, {}, {}) })

@ScreenPreviews @Composable
private fun MyStoreBoardConfirm() = Frame {
    MyGuestbookContent(mine, since, NOW, {}, {}, {})
    Box(Modifier.fillMaxSize().background(AppColors.Scrim)) {
        DialogPanel(
            stringResource(R.string.guestbook_store_title), stringResource(R.string.guestbook_store_body),
            stringResource(R.string.guestbook_store_confirm), {}, {},
        )
    }
}

@ScreenPreviews @Composable private fun FriendList() = Friend()
@ScreenPreviews @Composable private fun FriendWriteEmpty() = Friend(dialog = { WriteBody("주말캠퍼", TextFieldState(), {}, {}) })
@ScreenPreviews @Composable
private fun FriendWriteTyped() = Friend(dialog = { WriteBody("주말캠퍼", TextFieldState("주말 캠핑 사진 기대할게요!"), {}, {}) })
@ScreenPreviews @Composable private fun FriendMineDeleteConfirm() = Friend(dialog = { DeleteBody(friend.last(), {}, {}) })
@ScreenPreviews @Composable private fun FriendBoardOff() = Friend(boardShown = false)
