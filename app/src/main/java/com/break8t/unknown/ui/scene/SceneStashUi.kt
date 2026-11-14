package com.break8t.unknown.ui.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.break8t.unknown.R
import com.break8t.unknown.config.Catalog
import com.break8t.unknown.ui.components.AppCard
import com.break8t.unknown.ui.components.BorderWidth
import com.break8t.unknown.ui.components.ControlShape
import com.break8t.unknown.ui.components.PrimaryButton
import com.break8t.unknown.ui.house.SceneCharacter
import com.break8t.unknown.ui.house.SquareIconButton
import com.break8t.unknown.ui.iso.IsoShapes
import com.break8t.unknown.ui.iso.IsoThumbnail
import com.break8t.unknown.ui.photo.CharacterSizes
import com.break8t.unknown.ui.photo.characterObject
import com.break8t.unknown.ui.theme.AppColors

/**
 * 장면 보관함 패널. [items]는 아직 장면에 없는 것만 받고, [rule]이 막는 칸은 흐리게 두고 누를 수 없다.
 * [note]는 그 장면에서만 붙는 안내 한 줄이다(예: 친구 섬에서는 가구를 놓을 수 없음).
 */
@Composable
fun StashPanel(items: List<StashItem>, rule: StashRule, note: String?, onTakeOut: (StashItem) -> Unit, modifier: Modifier = Modifier) {
    // The empty pointerInput keeps touches on the panel from reaching the scene underneath.
    AppCard(modifier.fillMaxWidth().pointerInput(Unit) {}) {
        Column(Modifier.padding(23.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(stringResource(R.string.scene_stash), color = AppColors.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            // 장면 보관함은 메모리의 견본에서 바로 만들어서 불러오는 단계가 없다. 그래서 0개면 곧 빈 상태다.
            if (items.isEmpty()) {
                Text(stringResource(R.string.scene_stash_empty), color = AppColors.TextSub, fontSize = 31.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                    items(items, key = { it.id }) { item ->
                        val allowed = rule(item)
                        StashTile(item, if (allowed) { { onTakeOut(item) } } else null)
                    }
                }
            }
            if (note != null) Text(note, color = AppColors.TextSub, fontSize = 29.sp)
        }
    }
}

@Composable
private fun StashTile(item: StashItem, onClick: (() -> Unit)?) {
    val shape = if (item.kind == StashKind.CHARACTER) {
        characterObject(SceneCharacter(item.id, 0f, 0f, CharacterSizes[DEFAULT_SIZE], 0), 1, characterId = item.id).shape
    } else IsoShapes.of(item.id) ?: IsoShapes.plant
    Column(
        Modifier
            .width(166.dp)
            .alpha(if (onClick != null) 1f else DISABLED_ALPHA)
            .clip(ControlShape)
            .background(AppColors.Card)
            .border(BorderWidth, AppColors.Line, ControlShape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IsoThumbnail(shape, Modifier.size(99.dp))
        Text(Catalog.item(item.id)?.name.orEmpty(), color = AppColors.Text, fontSize = 31.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/**
 * 꺼낸 캐릭터의 조종 카드(사진 찍기 S-16-f와 같은 모양). [look]은 지금 포즈, [options]는 고를 수 있는 포즈다.
 * [onPutAway]가 있으면 보관함에 넣기 버튼을 더한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterCard(
    characterId: String,
    look: String,
    options: List<String>,
    onPose: (String) -> Unit,
    onResize: (Int) -> Unit,
    onTurn: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onPutAway: (() -> Unit)? = null,
) {
    AppCard(modifier.fillMaxWidth().pointerInput(Unit) {}) {
        FlowRow(
            Modifier.padding(23.dp),
            horizontalArrangement = Arrangement.spacedBy(31.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            itemVerticalAlignment = Alignment.Bottom,
        ) {
            Labeled(stringResource(R.string.photo_pose)) {
                // 칸이 많으면 좌우로 민다. 끝 칸이 반쯤 걸리게 폭을 잡고, 더 있는 쪽에 화살표를 두어 더 있다는 것을 보인다.
                val scroll = rememberScrollState()
                val scope = rememberCoroutineScope()
                val step = with(LocalDensity.current) { (109 + 16).dp.toPx() }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 화살표 자리는 늘 비워 둔다. 나타날 때마다 줄이 밀리면 그림이 제자리를 벗어나 보인다.
                Box(Modifier.size(48.dp)) { if (scroll.canScrollBackward) PoseArrow(left = true) { scope.launch { scroll.scrollBy(-step) } } }
                Row(Modifier.widthIn(max = 560.dp).horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (id in options) {
                        val on = look == id
                        val model = characterObject(SceneCharacter(id, 0f, 0f, CharacterSizes[DEFAULT_SIZE], 0), 1, characterId = characterId)
                        IsoThumbnail(
                            model.shape,
                            Modifier
                                .size(109.dp)
                                .clip(ControlShape)
                                .background(AppColors.Accent)
                                .border(if (on) 6.5.dp else BorderWidth, if (on) AppColors.Selected else AppColors.Line, ControlShape)
                                .clickable(role = Role.RadioButton) { onPose(id) }
                                .semantics { contentDescription = Catalog.item(id)?.name.orEmpty() }
                                .padding(8.dp),
                            rotation = model.rotation,
                        )
                    }
                }
                Box(Modifier.size(48.dp)) { if (scroll.canScrollForward) PoseArrow(left = false) { scope.launch { scroll.scrollBy(step) } } }
                }
                PixelScrollBar(scroll, Modifier.padding(start = 56.dp).widthIn(max = 560.dp).fillMaxWidth())
            }
            Labeled(stringResource(R.string.photo_size)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SquareIconButton(R.drawable.photo_ic_minus, stringResource(R.string.photo_smaller), { onResize(-1) })
                    SquareIconButton(R.drawable.photo_ic_plus, stringResource(R.string.photo_bigger), { onResize(+1) })
                }
            }
            SquareIconButton(R.drawable.photo_ic_rotate, stringResource(R.string.photo_rotate), onTurn)
            if (onPutAway != null) SquareIconButton(R.drawable.house_ic_box, stringResource(R.string.scene_put_away), onPutAway)
            PrimaryButton(stringResource(R.string.photo_done), onDone)
        }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = AppColors.TextSub, fontSize = 29.sp)
        content()
    }
}

private const val DISABLED_ALPHA = 0.4f

/** 포즈 줄 양끝의 화살표. 누르면 한 칸씩 민다(애니메이션 없이). */
@Composable
private fun PoseArrow(left: Boolean, onClick: () -> Unit) = Icon(
    painterResource(R.drawable.myinfo_ic_next),
    stringResource(if (left) R.string.scene_pose_prev else R.string.scene_pose_next),
    Modifier
        .size(48.dp)
        .clip(ControlShape)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(6.dp)
        .graphicsLayer { if (left) rotationZ = 180f },
    tint = AppColors.Text,
)

/** 가로 스크롤 위치를 보이는 픽셀 막대. 다 보이면 그리지 않는다. 손잡이 길이는 보이는 비율이다. */
@Composable
private fun PixelScrollBar(scroll: androidx.compose.foundation.ScrollState, modifier: Modifier = Modifier) {
    if (scroll.maxValue <= 0 || scroll.maxValue == Int.MAX_VALUE) return
    val view = scroll.viewportSize.toFloat().coerceAtLeast(1f)
    val shown = view / (view + scroll.maxValue)
    val at = scroll.value.toFloat() / scroll.maxValue
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.height(10.dp).background(AppColors.Muted)) {
        val thumb = maxWidth * shown
        androidx.compose.foundation.layout.Box(
            Modifier.offset(x = (maxWidth - thumb) * at).width(thumb).height(10.dp).background(AppColors.Line),
        )
    }
}
