package com.break8t.unknown.ui.scene

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
                Row(Modifier.widthIn(max = 620.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
