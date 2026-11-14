package com.break8t.unknown.ui.guestbook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.drive.LocalUxLimits
import com.break8t.unknown.ui.account.Avatar
import com.break8t.unknown.ui.myinfo.Friend
import com.break8t.unknown.ui.myinfo.FriendProfileCard
import com.break8t.unknown.ui.myinfo.FriendStore
import com.break8t.unknown.ui.components.AppDialog
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.shop.DangerButton
import com.break8t.unknown.ui.shop.DialogTitle
import com.break8t.unknown.ui.shop.DisabledButton
import com.break8t.unknown.ui.shop.ShopDialog
import com.break8t.unknown.ui.shop.TileShape
import com.break8t.unknown.ui.theme.AppColors
import java.util.UUID

/**
 * 내 섬 게시판을 누르면 여는 방명록. 친구가 남긴 글을 읽고 지운다. [newSince]보다 뒤에 온 것에 "새 방명록"을 붙인다.
 */
@Composable
fun MyGuestbookPanel(newSince: Long, onClose: () -> Unit) {
    val state = LocalDemoState.current
    val all by GuestbookStore.entries.collectAsState()
    val entries = all[GuestbookStore.MY_ISLAND].orEmpty()
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var askStore by rememberSaveable { mutableStateOf(false) }
    val friends by FriendStore.friends.collectAsState()
    var profileId by rememberSaveable { mutableStateOf<String?>(null) }
    MyGuestbookContent(
        entries, newSince, System.currentTimeMillis(), friends,
        onAuthor = { profileId = it.id }, onDelete = { deleteId = it.id }, onStore = { askStore = true }, onClose = onClose,
    )
    friends.firstOrNull { it.id == profileId }?.let { ProfileDialog(it) { profileId = null } }
    entries.firstOrNull { it.id == deleteId }?.let { e ->
        DeleteDialog(e, onCancel = { deleteId = null }) {
            GuestbookStore.remove(GuestbookStore.MY_ISLAND, e.id)
            deleteId = null
        }
    }
    if (askStore) {
        AppDialog(
            stringResource(R.string.guestbook_store_title), stringResource(R.string.guestbook_store_body),
            stringResource(R.string.guestbook_store_confirm),
            onConfirm = { askStore = false; state.update { withBoard(it, placed = false) }; onClose() },
            onDismiss = { askStore = false },
        )
    }
}

/** 친구 섬 게시판을 누르면 여는 방명록. 남긴 글을 읽고, 글을 써서 남기고, 내가 남긴 것만 지운다. */
@Composable
fun FriendGuestbookPanel(friendId: String, friendNickname: String, onClose: () -> Unit) {
    val profile by LocalDemoState.current.profile.collectAsState()
    val all by GuestbookStore.entries.collectAsState()
    val entries = all[friendId].orEmpty()
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var writing by rememberSaveable { mutableStateOf(false) }
    val friends by FriendStore.friends.collectAsState()
    var profileId by rememberSaveable { mutableStateOf<String?>(null) }
    FriendGuestbookContent(
        friendNickname, friendBoardShown(friendId), entries, System.currentTimeMillis(), friends,
        canDelete = { canDelete(friendId, it) }, onAuthor = { profileId = it.id }, onDelete = { deleteId = it.id },
        onWrite = { writing = true }, onClose = onClose,
    )
    friends.firstOrNull { it.id == profileId }?.let { ProfileDialog(it) { profileId = null } }
    if (writing) {
        WriteDialog(friendNickname, onCancel = { writing = false }) { text ->
            GuestbookStore.add(
                friendId,
                GuestbookEntry("g_${UUID.randomUUID()}", GuestbookStore.ME, profile.nickname, profile.selectedCharacterId, System.currentTimeMillis(), text),
            )
            writing = false
        }
    }
    entries.firstOrNull { it.id == deleteId }?.let { e ->
        DeleteDialog(e, onCancel = { deleteId = null }) {
            GuestbookStore.remove(friendId, e.id)
            deleteId = null
        }
    }
}

/** 섬에 들어올 때 지난 방문 뒤 새 방명록이 있으면 띄운다. 바깥을 누르면 나중에와 같다. */
@Composable
fun GuestbookNoticeDialog(count: Int, onOpen: () -> Unit, onLater: () -> Unit) =
    ShopDialog(onLater) { GuestbookNoticeBody(count, onOpen, onLater) }

@Composable
internal fun ColumnScope.GuestbookNoticeBody(count: Int, onOpen: () -> Unit, onLater: () -> Unit) {
    DialogTitle(stringResource(R.string.guestbook_notice_title, count))
    Text(stringResource(R.string.guestbook_notice_body), color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.guestbook_notice_later), onLater, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.guestbook_notice_open), onOpen, Modifier.weight(1f))
    }
}

@Composable
private fun ProfileDialog(friend: Friend, onClose: () -> Unit) = Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
    FriendProfileCard(friend, onClose, Modifier.padding(24.dp))
}

@Composable
private fun DeleteDialog(entry: GuestbookEntry, onCancel: () -> Unit, onDelete: () -> Unit) =
    ShopDialog(onCancel) { DeleteBody(entry, onCancel, onDelete) }

@Composable
internal fun ColumnScope.DeleteBody(entry: GuestbookEntry, onCancel: () -> Unit, onDelete: () -> Unit) {
    DialogTitle(stringResource(R.string.guestbook_delete_title))
    val body = if (entry.authorId == GuestbookStore.ME) stringResource(R.string.guestbook_delete_body_mine)
    else stringResource(R.string.guestbook_delete_body, entry.nickname)
    Text(body, color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.guestbook_cancel), onCancel, Modifier.weight(1f))
        DangerButton(stringResource(R.string.guestbook_delete), onDelete, Modifier.weight(1f))
    }
}

@Composable
private fun WriteDialog(friendNickname: String, onCancel: () -> Unit, onLeave: (String) -> Unit) {
    val field = rememberTextFieldState()
    ShopDialog(onCancel) { WriteBody(friendNickname, field, onCancel, onLeave) }
}

/** 글 쓰는 창. 열리면 바로 입력 칸에 커서를 두어 키보드가 뜬다. 비어 있으면 남기기를 누를 수 없다. */
@Composable
internal fun ColumnScope.WriteBody(
    friendNickname: String,
    field: TextFieldState,
    onCancel: () -> Unit,
    onLeave: (String) -> Unit,
    config: BalanceConfig = BalanceConfig.DEFAULT,
) {
    val noKeyboard = LocalUxLimits.current.noKeyboard
    val focus = remember { FocusRequester() }
    val preview = LocalInspectionMode.current
    LaunchedEffect(Unit) { if (!preview) focus.requestFocus() }
    val text = guestbookText(field.text.toString(), config)
    DialogTitle(stringResource(R.string.guestbook_write_title, friendNickname))
    BasicTextField(
        state = field,
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        readOnly = noKeyboard,
        inputTransformation = InputTransformation.maxLength(config.guestbookMaxChars),
        textStyle = TextStyle(color = AppColors.Text, fontSize = 40.sp, lineHeight = 56.sp),
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 4),
        cursorBrush = SolidColor(AppColors.Text),
        decorator = { inner ->
            Column(
                Modifier
                    .clip(TileShape)
                    .background(AppColors.Background)
                    .border(6.dp, if (noKeyboard) AppColors.Disabled else AppColors.Selected, TileShape)
                    .padding(horizontal = 25.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box {
                    if (field.text.isEmpty()) Text(stringResource(R.string.guestbook_write_hint), color = AppColors.TextSub, fontSize = 40.sp, lineHeight = 56.sp)
                    inner()
                }
                Text(
                    stringResource(R.string.guestbook_write_counter, field.text.length, config.guestbookMaxChars),
                    Modifier.align(Alignment.End), color = AppColors.TextSub, fontSize = 31.sp,
                )
            }
        },
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.guestbook_cancel), onCancel, Modifier.weight(1f))
        if (text != null) PrimaryButton(stringResource(R.string.guestbook_leave), { onLeave(text) }, Modifier.weight(1f))
        else DisabledButton(stringResource(R.string.guestbook_leave), Modifier.weight(1f))
    }
}

@Composable
internal fun MyGuestbookContent(
    entries: List<GuestbookEntry>,
    newSince: Long,
    now: Long,
    friends: List<Friend>,
    onAuthor: (Friend) -> Unit,
    onDelete: (GuestbookEntry) -> Unit,
    onStore: () -> Unit,
    onClose: () -> Unit,
) = PanelFrame(
    stringResource(R.string.guestbook_title), onClose,
    actions = { SecondaryButton(stringResource(R.string.guestbook_store), onStore) },
) {
    val fresh = newSince(entries, newSince).map { it.id }.toSet()
    EntryList(entries, Modifier.weight(1f)) { e ->
        EntryRow(e, isNew = e.id in fresh, now, authorOf(e, friends, onAuthor), onDelete.takeIf { canDelete(GuestbookStore.MY_ISLAND, e) })
    }
}

@Composable
internal fun FriendGuestbookContent(
    friendNickname: String,
    boardShown: Boolean,
    entries: List<GuestbookEntry>,
    now: Long,
    friends: List<Friend>,
    canDelete: (GuestbookEntry) -> Boolean,
    onAuthor: (Friend) -> Unit,
    onDelete: (GuestbookEntry) -> Unit,
    onWrite: () -> Unit,
    onClose: () -> Unit,
) = PanelFrame(
    stringResource(R.string.guestbook_friend_title, friendNickname), onClose,
    actions = { if (boardShown) PrimaryButton(stringResource(R.string.guestbook_write), onWrite) },
) {
    if (!boardShown) {
        Text(stringResource(R.string.guestbook_board_off), color = AppColors.TextSub, fontSize = 39.sp)
        return@PanelFrame
    }
    EntryList(entries, Modifier.weight(1f)) { e -> EntryRow(e, isNew = false, now, authorOf(e, friends, onAuthor), onDelete.takeIf { canDelete(e) }) }
}

/** 패널 틀. 아래 섬을 누르지 않게 화면 전체를 덮고, 가운데에 카드를 둔다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanelFrame(
    title: String,
    onClose: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) = Box(
    Modifier
        .fillMaxSize()
        .background(AppColors.Scrim)
        .clickable(remember { MutableInteractionSource() }, indication = null, onClick = {}),
) {
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(24.dp)
            .widthIn(max = 1200.dp)
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .clip(TileShape)
            .background(AppColors.Card)
            .border(3.dp, AppColors.Line, TileShape)
            .padding(36.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                title, Modifier.align(Alignment.CenterVertically), color = AppColors.Text, fontSize = 52.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            actions()
            SecondaryButton(stringResource(R.string.guestbook_close), onClose)
        }
        content()
    }
}

/** 저장소가 메모리에 있어서 늘 불러온 상태다. 그래서 빈 목록은 정말 비어 있는 것이다. */
@Composable
private fun EntryList(
    entries: List<GuestbookEntry>,
    modifier: Modifier,
    row: @Composable (GuestbookEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxWidth(), Alignment.Center) {
            Text(stringResource(R.string.guestbook_empty), color = AppColors.TextSub, fontSize = 39.sp)
        }
        return
    }
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items(entries, key = { it.id }) { row(it) }
    }
}

@Composable
private fun EntryRow(entry: GuestbookEntry, isNew: Boolean, now: Long, onAuthor: (() -> Unit)?, onDelete: ((GuestbookEntry) -> Unit)?) = Row(
    Modifier
        .fillMaxWidth()
        .heightIn(min = 128.dp)
        .clip(TileShape)
        .background(AppColors.Background)
        .border(if (isNew) 6.dp else 3.dp, if (isNew) AppColors.Selected else AppColors.Line, TileShape)
        .padding(20.dp),
    horizontalArrangement = Arrangement.spacedBy(20.dp),
) {
    // 그림과 이름을 누르면 프로필이 열린다. 친구가 아닌 사람(나, 지운 친구)은 누를 수 없다.
    val author = if (onAuthor != null) Modifier.clip(ControlShape).clickable(role = Role.Button, onClick = onAuthor) else Modifier
    Avatar(entry.characterId, 88.dp, 13.dp, author)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.nickname, Modifier.weight(1f, fill = false).then(author), color = AppColors.Text, fontSize = 40.sp,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (isNew) Tag(stringResource(R.string.guestbook_new), AppColors.Selected)
            if (entry.authorId == GuestbookStore.ME) Tag(stringResource(R.string.guestbook_mine), AppColors.Accent)
            Text(agoText(now, entry.atMs), color = AppColors.TextSub, fontSize = 31.sp)
        }
        Text(entry.text, color = AppColors.Text, fontSize = 36.sp, lineHeight = 52.sp)
    }
    if (onDelete != null) DangerButton(stringResource(R.string.guestbook_delete), { onDelete(entry) }, Modifier.align(Alignment.CenterVertically))
}

/** 남긴 사람이 지금 친구 목록에 있으면 그 친구. 나와 지운 친구는 null이라 프로필을 열 수 없다. */
internal fun friendOf(entry: GuestbookEntry, friends: List<Friend>): Friend? = friends.firstOrNull { it.id == entry.authorId }

private fun authorOf(entry: GuestbookEntry, friends: List<Friend>, onAuthor: (Friend) -> Unit): (() -> Unit)? =
    friendOf(entry, friends)?.let { f -> { onAuthor(f) } }

@Composable
private fun Tag(text: String, fill: androidx.compose.ui.graphics.Color) = Text(
    text,
    Modifier.clip(ControlShape).background(fill).padding(horizontal = 14.dp, vertical = 4.dp),
    color = AppColors.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold,
)

@Composable
private fun agoText(now: Long, at: Long): String = when (val a = ago(now, at)) {
    Ago.JustNow -> stringResource(R.string.guestbook_just_now)
    is Ago.Minutes -> stringResource(R.string.guestbook_minutes_ago, a.n.toInt())
    is Ago.Hours -> stringResource(R.string.guestbook_hours_ago, a.n.toInt())
    is Ago.Days -> stringResource(R.string.guestbook_days_ago, a.n.toInt())
}
