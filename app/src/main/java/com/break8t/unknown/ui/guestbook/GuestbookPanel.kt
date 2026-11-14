package com.break8t.unknown.ui.guestbook

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.BalanceConfig
import com.break8t.unknown.data.LocalDemoState
import com.break8t.unknown.ui.account.Avatar
import com.break8t.unknown.ui.components.AppDialog
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PixelBusy
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.components.SecondaryButton
import com.break8t.unknown.ui.shop.DangerButton
import com.break8t.unknown.ui.shop.DialogTitle
import com.break8t.unknown.ui.shop.ShopDialog
import com.break8t.unknown.ui.shop.TileShape
import com.break8t.unknown.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/** 목록이 그리는 소리 상태. 미리보기는 기기 없이 이것만 만들어 넣는다. */
data class GuestbookPlayback(val sound: GuestbookSound, val tts: TtsStatus, val now: Long)

/** 친구 섬 아래쪽 녹음 칸의 단계. */
enum class RecordStep { IDLE, RECORDING, RECORDED, SAVING }

fun recordStep(sound: GuestbookSound, draft: GuestbookDraft?, saving: Boolean): RecordStep = when {
    saving -> RecordStep.SAVING
    sound is GuestbookSound.Recording -> RecordStep.RECORDING
    draft != null -> RecordStep.RECORDED
    else -> RecordStep.IDLE
}

/** 녹음 칸에 넘기는 것. [secondsLeft]는 녹음 중, [draftSec]은 녹음한 뒤에 쓴다. */
data class RecordUi(
    val step: RecordStep,
    val secondsLeft: Int = 0,
    val draftSec: Int = 0,
    val playingDraft: Boolean = false,
    val micDenied: Boolean = false,
    val maxSec: Int = BalanceConfig.DEFAULT.guestbookMaxSec,
)

/**
 * 내 섬 게시판을 누르면 여는 방명록. 친구가 남긴 것을 듣고 지운다. [newSince]보다 뒤에 온 것에 "새 방명록"을 붙인다.
 * 화면에서 빠지면(닫기, 주행 덮개) 재생을 멈춘다.
 */
@Composable
fun MyGuestbookPanel(newSince: Long, onClose: () -> Unit) {
    val audio = rememberGuestbookAudio()
    val state = LocalDemoState.current
    val all by GuestbookStore.entries.collectAsState()
    val entries = all[GuestbookStore.MY_ISLAND].orEmpty()
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var askStore by rememberSaveable { mutableStateOf(false) }
    MyGuestbookContent(
        entries, newSince, rememberPlayback(audio),
        onPlay = audio::playEntry, onStop = audio::stop, onDelete = { deleteId = it.id }, onStore = { askStore = true }, onClose = onClose,
    )
    entries.firstOrNull { it.id == deleteId }?.let { e ->
        DeleteDialog(e, onCancel = { deleteId = null }) {
            GuestbookStore.remove(GuestbookStore.MY_ISLAND, e.id)
            audio.delete(e)
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

/**
 * 친구 섬 게시판을 누르면 여는 방명록. 남긴 것을 듣고, 녹음해서 남기고, 내가 남긴 것만 지운다.
 * 화면에서 빠지면(닫기, 주행 덮개) 녹음과 재생을 멈추고 남기지 않은 녹음을 지운다.
 */
@Composable
fun FriendGuestbookPanel(friendId: String, friendNickname: String, onClose: () -> Unit) {
    val config = BalanceConfig.DEFAULT
    val audio = rememberGuestbookAudio()
    val profile by LocalDemoState.current.profile.collectAsState()
    val all by GuestbookStore.entries.collectAsState()
    val entries = all[friendId].orEmpty()
    val draft by audio.draft.collectAsState()
    val playback = rememberPlayback(audio)
    var saving by remember { mutableStateOf(false) }
    var micDenied by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        micDenied = !ok
        if (ok) audio.record()
    }
    val recording = playback.sound as? GuestbookSound.Recording
    // The countdown on screen is what ends a recording; the recorder's own limit is only a backstop.
    LaunchedEffect(recording) {
        while (recording != null) {
            if (recordSecondsLeft(System.currentTimeMillis() - recording.startedMs, config) == 0) { audio.stop(); break }
            delay(TICK_MS)
        }
    }
    val record = RecordUi(
        recordStep(playback.sound, draft, saving),
        secondsLeft = recording?.let { recordSecondsLeft(playback.now - it.startedMs, config) } ?: 0,
        draftSec = draft?.lengthSec ?: 0,
        playingDraft = (playback.sound as? GuestbookSound.Playing)?.key == DRAFT_KEY,
        micDenied = micDenied,
        maxSec = config.guestbookMaxSec,
    )
    FriendGuestbookContent(
        friendId, friendNickname, friendBoardShown(friendId), entries, playback, record,
        onPlay = audio::playEntry, onStop = audio::stop, onDelete = { deleteId = it.id }, onClose = onClose,
        onRecord = {
            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                micDenied = false
                audio.record()
            } else {
                ask.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onReplay = { d -> if (d) audio.stop() else draft?.let { audio.play(DRAFT_KEY, it.path) } },
        onRerecord = audio::record,
        onLeave = {
            scope.launch {
                saving = true
                audio.keepDraft()?.let { kept ->
                    GuestbookStore.add(
                        friendId,
                        GuestbookEntry(
                            "g_${UUID.randomUUID()}", GuestbookStore.ME, profile.nickname, profile.selectedCharacterId,
                            System.currentTimeMillis(), kept.lengthSec, audioPath = kept.path,
                        ),
                    )
                }
                saving = false
            }
        },
    )
    entries.firstOrNull { it.id == deleteId }?.let { e ->
        DeleteDialog(e, onCancel = { deleteId = null }) {
            GuestbookStore.remove(friendId, e.id)
            audio.delete(e)
            deleteId = null
        }
    }
}

/** 섬에 들어올 때 지난 방문 뒤 새 방명록이 있으면 띄운다. 바깥을 누르면 나중에와 같다. */
@Composable
fun GuestbookNoticeDialog(count: Int, onListen: () -> Unit, onLater: () -> Unit) =
    ShopDialog(onLater) { GuestbookNoticeBody(count, onListen, onLater) }

@Composable
internal fun ColumnScope.GuestbookNoticeBody(count: Int, onListen: () -> Unit, onLater: () -> Unit) {
    DialogTitle(stringResource(R.string.guestbook_notice_title, count))
    Text(stringResource(R.string.guestbook_notice_body), color = AppColors.TextSub, fontSize = 36.sp, lineHeight = 55.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(35.dp)) {
        SecondaryButton(stringResource(R.string.guestbook_notice_later), onLater, Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.guestbook_notice_listen), onListen, Modifier.weight(1f))
    }
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
internal fun MyGuestbookContent(
    entries: List<GuestbookEntry>,
    newSince: Long,
    playback: GuestbookPlayback,
    onPlay: (GuestbookEntry) -> Unit,
    onStop: () -> Unit,
    onDelete: (GuestbookEntry) -> Unit,
    onStore: () -> Unit,
    onClose: () -> Unit,
) = PanelFrame(
    stringResource(R.string.guestbook_title), onClose,
    actions = { SecondaryButton(stringResource(R.string.guestbook_store), onStore) },
) {
    val fresh = newSince(entries, newSince).map { it.id }.toSet()
    EntryList(entries, Modifier.weight(1f)) { e ->
        EntryRow(e, isNew = e.id in fresh, playback, onPlay, onStop, onDelete.takeIf { canDelete(GuestbookStore.MY_ISLAND, e) })
    }
}

@Composable
internal fun FriendGuestbookContent(
    friendId: String,
    friendNickname: String,
    boardShown: Boolean,
    entries: List<GuestbookEntry>,
    playback: GuestbookPlayback,
    record: RecordUi,
    onPlay: (GuestbookEntry) -> Unit,
    onStop: () -> Unit,
    onDelete: (GuestbookEntry) -> Unit,
    onClose: () -> Unit,
    onRecord: () -> Unit,
    onReplay: (playing: Boolean) -> Unit,
    onRerecord: () -> Unit,
    onLeave: () -> Unit,
) = PanelFrame(stringResource(R.string.guestbook_friend_title, friendNickname), onClose) {
    if (!boardShown) {
        Text(stringResource(R.string.guestbook_board_off), color = AppColors.TextSub, fontSize = 39.sp)
        return@PanelFrame
    }
    EntryList(entries, Modifier.weight(1f)) { e ->
        EntryRow(e, isNew = false, playback, onPlay, onStop, onDelete.takeIf { canDelete(friendId, e) })
    }
    RecordBar(record, onRecord, onStop, onReplay, onRerecord, onLeave)
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
private fun EntryRow(
    entry: GuestbookEntry,
    isNew: Boolean,
    playback: GuestbookPlayback,
    onPlay: (GuestbookEntry) -> Unit,
    onStop: () -> Unit,
    onDelete: ((GuestbookEntry) -> Unit)?,
) {
    val playing = (playback.sound as? GuestbookSound.Playing)?.takeIf { it.key == entry.id }
    val sample = entry.audioPath == null
    Column(
        Modifier
            .fillMaxWidth()
            .clip(TileShape)
            .background(AppColors.Background)
            .border(if (isNew) 6.dp else 3.dp, if (isNew) AppColors.Selected else AppColors.Line, TileShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(entry.characterId, 88.dp, 13.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.nickname, Modifier.weight(1f, fill = false), color = AppColors.Text, fontSize = 40.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (isNew) Tag(stringResource(R.string.guestbook_new), AppColors.Selected)
                    if (entry.authorId == GuestbookStore.ME) Tag(stringResource(R.string.guestbook_mine), AppColors.Accent)
                }
                Text(
                    stringResource(R.string.guestbook_meta, agoText(playback.now, entry.atMs), entry.lengthSec),
                    color = AppColors.TextSub, fontSize = 31.sp,
                )
                if (playing != null) PlayingText(playing, playback.now)
            }
            PlayButton(
                playing = playing != null,
                waiting = sample && playback.tts == TtsStatus.STARTING,
                enabled = !sample || playback.tts == TtsStatus.READY,
                onClick = { if (playing != null) onStop() else onPlay(entry) },
            )
            if (onDelete != null) DangerButton(stringResource(R.string.guestbook_delete), { onDelete(entry) })
        }
        // Without a Korean voice on the car the sample is shown as text instead.
        if (sample && playback.tts == TtsStatus.UNAVAILABLE && entry.sampleText != null) {
            Text(stringResource(R.string.guestbook_sample_text, entry.sampleText), color = AppColors.Text, fontSize = 34.sp, lineHeight = 48.sp)
        }
    }
}

@Composable
private fun PlayingText(playing: GuestbookSound.Playing, now: Long) {
    val text = playing.lengthMs?.let { len ->
        val left = ((len - (now - playing.startedMs) + 999) / 1000).coerceAtLeast(0)
        stringResource(R.string.guestbook_playing_left, left.toInt())
    } ?: stringResource(R.string.guestbook_playing)
    Text(text, color = AppColors.Success, fontSize = 31.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Tag(text: String, fill: androidx.compose.ui.graphics.Color) = Text(
    text,
    Modifier.clip(ControlShape).background(fill).padding(horizontal = 14.dp, vertical = 4.dp),
    color = AppColors.Text, fontSize = 27.sp, fontWeight = FontWeight.Bold,
)

@Composable
private fun PlayButton(playing: Boolean, waiting: Boolean, enabled: Boolean, onClick: () -> Unit) = Box(
    Modifier
        .size(100.dp)
        .clip(ControlShape)
        .background(if (enabled) AppColors.Card else AppColors.Disabled)
        .border(3.dp, if (enabled) AppColors.Line else AppColors.Disabled, ControlShape)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
) {
    if (waiting) {
        PixelBusy(block = 8.dp)
    } else {
        Icon(
            painterResource(if (playing) R.drawable.shop_ic_pause else R.drawable.shop_ic_play),
            contentDescription = stringResource(if (playing) R.string.guestbook_stop else R.string.guestbook_play),
            Modifier.size(52.dp),
            tint = if (enabled) AppColors.Text else AppColors.DisabledText,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordBar(
    ui: RecordUi,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onReplay: (playing: Boolean) -> Unit,
    onRerecord: () -> Unit,
    onLeave: () -> Unit,
) = Column(
    Modifier.fillMaxWidth().clip(TileShape).background(AppColors.Accent).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    val row = @Composable { content: @Composable FlowRowScope.() -> Unit ->
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
    when (ui.step) {
        RecordStep.IDLE -> {
            row {
                PrimaryButton(stringResource(R.string.guestbook_record), onRecord)
                Text(
                    stringResource(R.string.guestbook_record_hint, ui.maxSec), Modifier.align(Alignment.CenterVertically),
                    color = AppColors.TextSub, fontSize = 31.sp,
                )
            }
            if (ui.micDenied) Text(stringResource(R.string.guestbook_mic_denied), color = AppColors.Error, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        }
        RecordStep.RECORDING -> row {
            Row(Modifier.align(Alignment.CenterVertically), Arrangement.spacedBy(14.dp), Alignment.CenterVertically) {
                // A still red dot: the screen stays static, the seconds count down beside it.
                Box(Modifier.size(28.dp).clip(ControlShape).background(AppColors.Error))
                Text(stringResource(R.string.guestbook_recording, ui.secondsLeft), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold)
            }
            SecondaryButton(stringResource(R.string.guestbook_record_stop), onStop)
        }
        RecordStep.RECORDED -> {
            Text(stringResource(R.string.guestbook_recorded, ui.draftSec), color = AppColors.Text, fontSize = 39.sp, fontWeight = FontWeight.Bold)
            row {
                SecondaryButton(
                    stringResource(if (ui.playingDraft) R.string.guestbook_stop else R.string.guestbook_replay),
                    { onReplay(ui.playingDraft) },
                )
                SecondaryButton(stringResource(R.string.guestbook_rerecord), onRerecord)
                PrimaryButton(stringResource(R.string.guestbook_leave), onLeave)
            }
        }
        RecordStep.SAVING -> Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), Alignment.Center) { PixelBusy() }
    }
}

@Composable
private fun agoText(now: Long, at: Long): String = when (val a = ago(now, at)) {
    Ago.JustNow -> stringResource(R.string.guestbook_just_now)
    is Ago.Minutes -> stringResource(R.string.guestbook_minutes_ago, a.n.toInt())
    is Ago.Hours -> stringResource(R.string.guestbook_hours_ago, a.n.toInt())
    is Ago.Days -> stringResource(R.string.guestbook_days_ago, a.n.toInt())
}

private fun GuestbookAudio.playEntry(e: GuestbookEntry) {
    val path = e.audioPath
    if (path != null) play(e.id, path) else e.sampleText?.let { speak(e.id, it) }
}

/** 패널마다 하나. 화면에서 빠지면 놓아서, 주행 덮개가 섬을 치우는 순간 소리도 멈춘다. */
@Composable
private fun rememberGuestbookAudio(): GuestbookAudio {
    val context = LocalContext.current
    val audio = remember { GuestbookAudio(context) }
    DisposableEffect(audio) { onDispose { audio.release() } }
    return audio
}

@Composable
private fun rememberPlayback(audio: GuestbookAudio): GuestbookPlayback {
    val sound by audio.sound.collectAsState()
    val tts by audio.tts.collectAsState()
    val ticking = sound is GuestbookSound.Recording || sound is GuestbookSound.Playing
    val now by produceState(System.currentTimeMillis(), ticking) {
        value = System.currentTimeMillis()
        while (ticking) {
            delay(TICK_MS)
            value = System.currentTimeMillis()
        }
    }
    return GuestbookPlayback(sound, tts, now)
}

private const val TICK_MS = 200L
